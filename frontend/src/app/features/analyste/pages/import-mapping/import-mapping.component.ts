import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { ImportService, ImportContexte } from '../../../../core/services/import.service';
import { ImportUploadStateService } from '../../../../core/services/import-upload-state.service';
import {
  CategoryScoreDTO,
  ColumnProfileDTO,
  ImportIssue,
  ImportProcessingResponse,
  ImportQualityReport,
  KpiCalculatedDTO,
  RejectedReasonSummary,
} from '../../../../core/models/import-session.model';

@Component({
  selector: 'app-import-mapping',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatChipsModule,
  ],
  templateUrl: './import-mapping.component.html',
  styleUrls: ['./import-mapping.component.css'],
})
export class ImportMappingComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private importService = inject(ImportService);
  private uploadState = inject(ImportUploadStateService);
  private snackBar = inject(MatSnackBar);

  uploadStateData = signal<{ file: File; yearN: number; yearNMinus1: number; headers: string[]; detectedHeaders?: string[]; contexteSecteur?: string; contexteTaille?: string; contexteCertifications?: string; contexteObjectifs?: string; contexteReglementation?: string; contexteSpecifique?: string } | null>(null);

  kpiColumn = signal<number | null>(null);
  valueNColumn = signal<number | null>(null);
  valueNMinus1Column = signal<number | null>(null);

  errorMsg = signal('');
  isColumnMappingError = signal(false);
  processing = signal(false);
  progressPercent = signal(0);
  progressStage = signal('');
  progressMessage = signal('');
  previewResponse = signal<ImportProcessingResponse | null>(null);
  result = signal<ImportProcessingResponse | null>(null);

  
  columnProfiles = signal<ColumnProfileDTO[]>([]);
  profilingLoading = signal(false);

  private eventSource: EventSource | null = null;
  private profileCache = new Map<string, ColumnProfileDTO[]>();

  private fileKey(file: File): string {
    return `${file.name}_${file.size}_${file.lastModified}`;
  }

  
  private mappingKey(headers: string[]): string {
    return `qhse-col-mapping-${btoa(encodeURIComponent(headers.join('|'))).slice(0, 40)}`;
  }

  private saveMapping(headers: string[]): void {
    try {
      const payload = { kpiColumn: this.kpiColumn(), valueNColumn: this.valueNColumn(), valueNMinus1Column: this.valueNMinus1Column() };
      localStorage.setItem(this.mappingKey(headers), JSON.stringify(payload));
    } catch {  }
  }

  private restoreMapping(headers: string[]): void {
    try {
      const raw = localStorage.getItem(this.mappingKey(headers));
      if (!raw) return;
      const saved = JSON.parse(raw);
      if (saved.kpiColumn !== undefined && saved.kpiColumn !== null) this.kpiColumn.set(saved.kpiColumn);
      if (saved.valueNColumn !== undefined && saved.valueNColumn !== null) this.valueNColumn.set(saved.valueNColumn);
      if (saved.valueNMinus1Column !== undefined && saved.valueNMinus1Column !== null) this.valueNMinus1Column.set(saved.valueNMinus1Column);
    } catch {  }
  }

  readonly previewColumns = ['rowStatus', 'kpiName', 'categorie',/* 'unite','definition',*/ 'valeurN1','valeurN',  'status', 'commentaire', 'variation', 'absoluteGap'/*, 'meta'*/];
  readonly resultColumns  = ['spark', 'kpiName', 'categorie', 'unite', 'valeurN', 'valeurN1', 'variation', 'absoluteGap', 'status', 'risk', 'meta'];
  readonly issueColumns   = ['severity', 'row', 'column', 'message'];

  readonly riskRows    = [5, 4, 3, 2, 1]; 
  readonly riskColumns = [1, 2, 3, 4, 5]; 

  riskKpis = computed(() =>
    (this.result()?.calculatedData ?? []).filter(k => k.riskProbability != null && k.riskImpact != null)
  );

  categoryScores = computed((): CategoryScoreDTO[] => this.result()?.categoryScores ?? []);

  kpisAtCell(prob: number, impact: number): KpiCalculatedDTO[] {
    return this.riskKpis().filter(k => k.riskProbability === prob && k.riskImpact === impact);
  }

  riskCellClass(prob: number, impact: number): string {
    const score = prob * impact;
    if (score >= 20) return 'risk-cell critical';
    if (score >= 12) return 'risk-cell high';
    if (score >= 6)  return 'risk-cell medium';
    return 'risk-cell low';
  }

  riskLevelClass(level: string | null | undefined): string {
    if (level === 'Critique' || level === 'CRITICAL') return 'risk-tag critical';
    if (level === 'Élevé' || level === 'HIGH')       return 'risk-tag high';
    if (level === 'Modéré' || level === 'MEDIUM')    return 'risk-tag medium';
    return 'risk-tag low';
  }

  
  private sparkMax = computed((): number => {
    const vals = (this.result()?.calculatedData ?? [])
      .flatMap(k => [Math.abs(k.valeurN ?? 0), Math.abs(k.valeurN1 ?? 0)]);
    return Math.max(...vals, 1);
  });

  sparkN1Height(row: KpiCalculatedDTO): number {
    return Math.round((Math.abs(row.valeurN1 ?? 0) / this.sparkMax()) * 24);
  }

  sparkNHeight(row: KpiCalculatedDTO): number {
    return Math.round((Math.abs(row.valeurN ?? 0) / this.sparkMax()) * 24);
  }

  sparkNColor(row: KpiCalculatedDTO): string {
    const n = row.valeurN ?? 0;
    const n1 = row.valeurN1 ?? 0;
    if (row.spcOutOfControl) return '#E53E3E';
    return n >= n1 ? '#38A169' : '#DD6B20';
  }

  categoryScoreClass(score: number): string {
    if (score >= 90) return 'cat-score-excellent';
    if (score >= 75) return 'cat-score-bon';
    if (score >= 55) return 'cat-score-acceptable';
    if (score >= 30) return 'cat-score-surveiller';
    return 'cat-score-critique';
  }

  ngOnInit() {
    const state = this.uploadState.getUpload();
    if (!state) { this.router.navigate(['/analyste/import']); return; }
    this.uploadStateData.set(state);
    this.restoreMapping(state.headers);
    this.loadColumnProfiles(state.file);
  }

  private async loadColumnProfiles(file: File): Promise<void> {
    const key = this.fileKey(file);
    const cached = this.profileCache.get(key);
    if (cached) {
      this.columnProfiles.set(cached);
      this.autoSelectFromProfiles(cached);
      return;
    }
    this.profilingLoading.set(true);
    try {
      const profiles = await firstValueFrom(this.importService.profileColumns(file));
      const result = profiles ?? [];
      this.profileCache.set(key, result);
      this.columnProfiles.set(result);
      this.autoSelectFromProfiles(result);
    } catch {
      this.columnProfiles.set([]);
    } finally {
      this.profilingLoading.set(false);
    }
  }

  private autoSelectFromProfiles(profiles: ColumnProfileDTO[]): void {
    this.kpiColumn.set(null);
    this.valueNColumn.set(null);
    this.valueNMinus1Column.set(null);

    const kpi    = profiles.find(p => p.likelySemantic === 'KPI_NAME');
    const yearN  = this.uploadStateData()?.yearN;
    const yearN1 = this.uploadStateData()?.yearNMinus1;

    const byYear = (year: number | undefined, fallbackSemantic: string): ColumnProfileDTO | undefined =>
      year != null
        ? (profiles.find(p => p.detectedHeader.includes(String(year))) ??
           profiles.find(p => p.likelySemantic === fallbackSemantic))
        : profiles.find(p => p.likelySemantic === fallbackSemantic);

    const valN  = byYear(yearN,  'VALUE_N');
    const valN1 = byYear(yearN1, 'VALUE_N1');

    if (kpi)   this.kpiColumn.set(kpi.columnIndex);
    if (valN)  this.valueNColumn.set(valN.columnIndex);
    if (valN1) this.valueNMinus1Column.set(valN1.columnIndex);
  }

  ngOnDestroy() {
    this.closeProgressStream();
    this.profileCache.clear();
  }

  private openProgressStream(clientId: string): void {
    this.closeProgressStream();
    const url = this.importService.progressStreamUrl(clientId);
    this.eventSource = new EventSource(url);
    this.eventSource.addEventListener('progress', (event: MessageEvent) => {
      const data = JSON.parse(event.data);
      this.progressPercent.set(data.percent ?? 0);
      this.progressStage.set(data.stage ?? '');
      this.progressMessage.set(data.message ?? '');
    });
    this.eventSource.addEventListener('import-error', (event: MessageEvent) => {
      const data = JSON.parse(event.data);
      this.progressMessage.set(data.message ?? 'Erreur de traitement.');
      this.closeProgressStream();
    });
    this.eventSource.onerror = () => { this.closeProgressStream(); };
  }

  private closeProgressStream(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  get steps() { return ['Importer', 'Mapper les colonnes', 'Traiter']; }

  get indexes(): number[] {
    return (this.uploadStateData()?.headers ?? []).map((_, i) => i);
  }

  get selectedIndexes(): number[] {
    return [this.kpiColumn(), this.valueNColumn(), this.valueNMinus1Column()]
      .filter((v): v is number => v !== null);
  }

  validateSelections(): boolean {
    if (this.kpiColumn() === null || this.valueNColumn() === null || this.valueNMinus1Column() === null) {
      this.errorMsg.set('Tous les champs obligatoires (KPI, Valeur N, Valeur N-1) doivent être mappés.');
      return false;
    }
    const dupes = this.selectedIndexes.filter((v, i, s) => s.indexOf(v) !== i);
    if (dupes.length > 0) { this.errorMsg.set('Chaque colonne doit être sélectionnée une seule fois.'); return false; }
    this.errorMsg.set('');
    return true;
  }

  async submitMapping() {
    if (!this.validateSelections()) return;
    const state = this.uploadStateData();
    if (!state) { this.errorMsg.set("État d'import manquant. Revenez à l'étape précédente."); return; }

    const mapping = { kpiNameIndex: this.kpiColumn()!, valueNIndex: this.valueNColumn()!, valueN1Index: this.valueNMinus1Column()! };
    this.processing.set(true);
    this.errorMsg.set('');
    this.isColumnMappingError.set(false);
    this.previewResponse.set(null);

    try {
      const response = await firstValueFrom(this.importService.previewImport(state.file, state.yearN, state.yearNMinus1, mapping));
      this.previewResponse.set(response);
      this.uploadState.saveResponse(response);
      this.saveMapping(state.headers);
      if (response.qualityReport?.hardBlocking) {
        this.snackBar.open('Erreur structurelle bloquante.', 'OK', { duration: 7000 });
      } else if (response.qualityReport?.softBlocking) {
        this.snackBar.open('Import partiel disponible : certaines lignes restent importables.', 'OK', { duration: 6000 });
      } else if (response.qualityReport?.blocking) {
        this.snackBar.open('⚠️ Des erreurs bloquantes ont été détectées. Corrigez le fichier avant de confirmer.', 'OK', { duration: 6000 });
      } else {
        this.snackBar.open('Prévisualisation prête.', 'OK', { duration: 3000 });
      }
    } catch (err: unknown) {
      const msg: string = (err instanceof HttpErrorResponse ? err.error?.message : null) ?? 'Erreur lors de la prévisualisation.';
      if (msg.startsWith('Colonnes non reconnues')) {
        this.isColumnMappingError.set(true);
      } else {
        this.errorMsg.set(msg);
        this.snackBar.open(msg, 'OK', { duration: 5000 });
      }
    } finally {
      this.processing.set(false);
    }
  }

  async confirmImport() {
    if (!this.validateSelections()) return;
    const state = this.uploadStateData();
    const preview = this.previewResponse();
    if (!state || !preview) { this.errorMsg.set('Aucune prévisualisation disponible.'); return; }
    if (this.isHardBlocking()) { this.errorMsg.set('Erreur structurelle bloquante.'); return; }
    if (this.isSoftBlocking()) { this.errorMsg.set('Des lignes comportent des erreurs. Utilisez "Continuer avec lignes valides".'); return; }
    if (this.isImportBlocking()) { this.errorMsg.set("Corrigez les erreurs bloquantes avant de confirmer l'import."); return; }

    const clientId = crypto.randomUUID();
    const mapping = { kpiNameIndex: this.kpiColumn()!, valueNIndex: this.valueNColumn()!, valueN1Index: this.valueNMinus1Column()! };
    this.progressPercent.set(0);
    this.progressStage.set('INITIALISATION');
    this.progressMessage.set('Démarrage du traitement...');
    this.processing.set(true);
    this.errorMsg.set('');
    this.openProgressStream(clientId);

    try {
      const response = await firstValueFrom(this.importService.confirmStrict(state.file, state.yearN, state.yearNMinus1, mapping, clientId, this.getContexte()));
      this.uploadState.saveResponse(response);
      this.snackBar.open('Importation confirmée avec succès.', 'OK', { duration: 3000 });
      const sessionId = response.importSessionId;
      if (sessionId) {
        this.router.navigate(['/analyste/dashboard', sessionId]);
      } else {
        this.router.navigate(['/analyste/dashboard']);
      }
    } catch (err: unknown) {
      this.errorMsg.set((err instanceof HttpErrorResponse ? err.error?.message : null) ?? 'Erreur lors de la confirmation.');
      this.snackBar.open(this.errorMsg(), 'OK', { duration: 5000 });
    } finally {
      this.processing.set(false);
      this.closeProgressStream();
    }
  }

  async confirmPartialImport() {
    if (!this.validateSelections()) return;
    const state = this.uploadStateData();
    const preview = this.previewResponse();
    if (!state || !preview) { this.errorMsg.set('Aucune prévisualisation disponible.'); return; }
    if (this.isHardBlocking()) { this.errorMsg.set('Erreur structurelle bloquante. Impossible d\'importer même partiellement.'); return; }

    const clientId = crypto.randomUUID();
    const mapping = { kpiNameIndex: this.kpiColumn()!, valueNIndex: this.valueNColumn()!, valueN1Index: this.valueNMinus1Column()! };
    this.progressPercent.set(0);
    this.progressStage.set('INITIALISATION');
    this.progressMessage.set('Démarrage du traitement partiel...');
    this.processing.set(true);
    this.errorMsg.set('');
    this.openProgressStream(clientId);

    try {
      const response = await firstValueFrom(this.importService.confirmPartial(state.file, state.yearN, state.yearNMinus1, mapping, clientId, this.getContexte()));
      this.uploadState.saveResponse(response);
      const imported = response.qualityReport?.importedRowsCount ?? 0;
      const rejected = response.qualityReport?.rejectedRowsCount ?? 0;
      this.snackBar.open(`Import partiel terminé : ${imported} importées, ${rejected} rejetées.`, 'OK', { duration: 6000 });
      const sessionId = response.importSessionId;
      if (sessionId) {
        this.router.navigate(['/analyste/dashboard', sessionId]);
      } else {
        this.router.navigate(['/analyste/dashboard']);
      }
    } catch (err: unknown) {
      this.errorMsg.set((err instanceof HttpErrorResponse ? err.error?.message : null) ?? 'Erreur lors de l\'import partiel.');
      this.snackBar.open(this.errorMsg(), 'OK', { duration: 5000 });
    } finally {
      this.processing.set(false);
      this.closeProgressStream();
    }
  }

  

  get qualityReport(): ImportQualityReport | undefined {
    return this.previewResponse()?.qualityReport;
  }

  isImportBlocking(): boolean {
    return this.qualityReport?.blocking === true;
  }

  isHardBlocking(): boolean {
    return this.qualityReport?.hardBlocking === true;
  }

  isSoftBlocking(): boolean {
    return this.qualityReport?.softBlocking === true;
  }

  canConfirmStrict(): boolean {
    return !!this.previewResponse() && !this.isHardBlocking() && !this.isSoftBlocking() && !this.isImportBlocking();
  }

  canConfirmPartial(): boolean {
    return !!this.previewResponse() && this.isSoftBlocking() && !this.isHardBlocking();
  }

  qualityScorePercent(): string {
    const score = this.qualityReport?.qualityScore ?? 0;
    return (+score.toFixed(1)).toString();
  }

  qualityScoreClass(): string {
    const s = this.qualityReport?.qualityScore ?? 0;
    if (s >= 80) return 'score-good';
    if (s >= 50) return 'score-warn';
    return 'score-bad';
  }

  get allIssues(): ImportIssue[] {
    const r = this.qualityReport;
    if (!r) return [];
    return [...(r.extractionIssues ?? []), ...(r.errors ?? []), ...(r.warnings ?? []), ...(r.infos ?? [])];
  }

  get rejectedReasons(): RejectedReasonSummary[] {
    return this.qualityReport?.rejectedReasons ?? [];
  }

  get hasBlockingReason(): boolean {
    return !!this.qualityReport?.blockingReason;
  }

  issueRowLabel(issue: ImportIssue): string {
    return issue.rowIndex === null ? 'Global' : String(issue.rowIndex);
  }

  rejectedReasonLabel(reason: RejectedReasonSummary): string {
    return `${reason.code} · ${reason.count}`;
  }

  get hasLowConfidenceMapping(): boolean {
    if (this.allIssues.some(i => i.code === 'LOW_CONFIDENCE_EXTRACTION')) return true;
    const selected = [this.kpiColumn(), this.valueNColumn(), this.valueNMinus1Column()]
      .filter((v): v is number => v !== null)
      .map(idx => this.columnProfiles().find(p => p.columnIndex === idx))
      .filter((p): p is ColumnProfileDTO => p !== undefined);
    return selected.length > 0 && selected.every(p => (p.confidenceScore ?? 0) < 0.70);
  }

  severityClass(severity: string): string {
    if (severity === 'ERROR')   return 'issue-error';
    if (severity === 'WARNING') return 'issue-warning';
    return 'issue-info';
  }

  severityIcon(severity: string): string {
    if (severity === 'ERROR')   return 'error';
    if (severity === 'WARNING') return 'warning';
    return 'info';
  }

  severityLabel(severity: string): string {
    if (severity === 'ERROR')   return 'Erreur';
    if (severity === 'WARNING') return 'Avertissement';
    if (severity === 'INFO')    return 'Information';
    return severity;
  }

  rowHasError(rowIndex: number): boolean {
    return this.allIssues.some(i => i.rowIndex === rowIndex && i.severity === 'ERROR');
  }

  rowHasWarning(rowIndex: number): boolean {
    return this.allIssues.some(i => i.rowIndex === rowIndex && i.severity === 'WARNING');
  }

  rowIssueTooltip(rowIndex: number): string {
    return this.allIssues
      .filter(i => i.rowIndex === rowIndex)
      .map(i => `[${this.severityLabel(i.severity)}] ${i.message}`)
      .join('\n');
  }

  

  profileForColumn(colIndex: number): ColumnProfileDTO | undefined {
    return this.columnProfiles().find(p => p.columnIndex === colIndex);
  }

  profileTypeIcon(type: ColumnProfileDTO['inferredType']): string {
    const icons: Record<string, string> = { NUMERIC: 'tag', TEXT: 'text_fields', BOOLEAN: 'toggle_on', DATE: 'calendar_today', MIXED: 'join_full' };
    return icons[type] ?? 'help_outline';
  }


  profileNullRate(p: ColumnProfileDTO): string {
    if (!p.totalRows) return '0%';
    return `${Math.round((p.nullCount / p.totalRows) * 100)}%`;
  }

  getConfidenceClass(p: ColumnProfileDTO): string {
    const s = p.confidenceScore ?? 0;
    if (s >= 0.85) return 'confidence-high';
    if (s >= 0.70) return 'confidence-medium';
    return 'confidence-low';
  }

  getConfidenceLabel(p: ColumnProfileDTO): string {
    const s = p.confidenceScore ?? 0;
    if (s >= 0.85) return 'Sûr';
    if (s >= 0.70) return 'Douteux';
    return 'Inconnu';
  }

  clampConfidence(value: number | undefined): number {
    return Math.max(0, Math.min(100, value ?? 0));
  }



  

  get isProcessingStep(): boolean { return this.processing(); }

  getHeaderLabel(index: number): string {
    return this.uploadStateData()?.headers?.[index] ?? `Colonne ${index}`;
  }

  extractMethodBadgeClass(method: string | null | undefined): string {
    const n = method?.trim().toUpperCase() ?? '';
    if (n.includes('OCR'))    return 'badge-ocr';
    if (n.includes('AI'))     return 'badge-ai';
    if (n.includes('MANUAL')) return 'badge-manual';
    return 'badge-default';
  }

  qualityLabel(score: number | null | undefined): string {
    if (score === null || score === undefined || Number.isNaN(score)) return 'N/A';
    return `${Math.round(score)}%`;
  }

  invalidRateLabel(rawData: ImportProcessingResponse['rawData']): string {
    const items = rawData ?? [];
    if (!items.length) return 'Aucune ligne détectée.';
    const invalid = items.filter(r => !r.valid).length;
    return `${invalid} ligne(s) invalide(s) sur ${items.length} (${Math.round((invalid / items.length) * 100)}%)`;
  }

  private getContexte(): ImportContexte {
    const s = this.uploadStateData();
    return {
      contexteSecteur:        s?.contexteSecteur,
      contexteTaille:         s?.contexteTaille,
      contexteCertifications: s?.contexteCertifications,
      contexteObjectifs:      s?.contexteObjectifs,
      contexteReglementation: s?.contexteReglementation,
      contexteSpecifique:     s?.contexteSpecifique,
    };
  }

  goBack() { this.router.navigate(['/analyste/import']); }

  goToDashboard() {
    const id = this.result()?.importSessionId ?? null;
    if (id) { this.router.navigate(['/analyste/dashboard', id]); return; }
    this.router.navigate(['/analyste/dashboard']);
  }

  getVariationClass(variation: number | undefined): string {
    if (variation === undefined) return '';
    if (variation > 0) return 'text-up';
    if (variation < 0) return 'text-down';
    return 'text-stable';
  }

  getStatusClass(color: string | undefined): string {
    return `status-badge status-${color || 'gray'}`;
  }
}
