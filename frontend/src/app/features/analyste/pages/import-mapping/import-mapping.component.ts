import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
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
import { MatTooltipModule } from '@angular/material/tooltip';
import { ImportService } from '../../../../core/services/import.service';
import { ImportUploadStateService } from '../../../../core/services/import-upload-state.service';
import {
  ImportIssue,
  ImportProcessingResponse,
  ImportQualityReport,
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
    MatTooltipModule,
  ],
  templateUrl: './import-mapping.component.html',
  styleUrls: ['./import-mapping.component.css'],
})
export class ImportMappingComponent implements OnInit {
  private router = inject(Router);
  private importService = inject(ImportService);
  private uploadState = inject(ImportUploadStateService);
  private snackBar = inject(MatSnackBar);

  uploadStateData = signal<{ file: File; yearN: number; yearNMinus1: number; headers: string[]; detectedHeaders?: string[] } | null>(null);

  kpiColumn = signal<number | null>(null);
  valueNColumn = signal<number | null>(null);
  valueNMinus1Column = signal<number | null>(null);

  errorMsg = signal('');
  processing = signal(false);
  previewResponse = signal<ImportProcessingResponse | null>(null);
  result = signal<ImportProcessingResponse | null>(null);

  readonly previewColumns = ['rowStatus', 'kpiName', 'categorie', 'unite', 'definition', 'valeurN', 'valeurN1', 'status', 'commentaire', 'variation', 'absoluteGap'];
  readonly resultColumns  = ['kpiName', 'categorie', 'unite', 'valeurN', 'valeurN1', 'variation', 'absoluteGap', 'status'];
  readonly issueColumns   = ['severity', 'row', 'column', 'message'];

  ngOnInit() {
    const state = this.uploadState.getUpload();
    if (!state) { this.router.navigate(['/analyste/import']); return; }
    this.uploadStateData.set(state);
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

    const mapping = { kpiNameIndex: this.kpiColumn()!, unitIndex: -1, valueNIndex: this.valueNColumn()!, valueN1Index: this.valueNMinus1Column()!, categoryIndex: -1 };
    this.processing.set(true);
    this.errorMsg.set('');
    this.previewResponse.set(null);

    try {
      const response = await firstValueFrom(this.importService.previewImport(state.file, state.yearN, state.yearNMinus1, mapping));
      this.previewResponse.set(response);
      this.uploadState.saveResponse(response);
      if (response.qualityReport?.hardBlocking) {
        this.snackBar.open('Erreur structurelle bloquante.', 'OK', { duration: 7000 });
      } else if (response.qualityReport?.softBlocking) {
        this.snackBar.open('Import partiel disponible : certaines lignes restent importables.', 'OK', { duration: 6000 });
      } else if (response.qualityReport?.blocking) {
        this.snackBar.open('⚠️ Des erreurs bloquantes ont été détectées. Corrigez le fichier avant de confirmer.', 'OK', { duration: 6000 });
      } else {
        this.snackBar.open('Prévisualisation prête.', 'OK', { duration: 3000 });
      }
    } catch (err: any) {
      this.errorMsg.set(err?.error?.message ?? 'Erreur lors de la prévisualisation.');
      this.snackBar.open(this.errorMsg(), 'OK', { duration: 5000 });
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

    const mapping = { kpiNameIndex: this.kpiColumn()!, unitIndex: -1, valueNIndex: this.valueNColumn()!, valueN1Index: this.valueNMinus1Column()!, categoryIndex: -1 };
    this.processing.set(true);
    this.errorMsg.set('');

    try {
      const response = await firstValueFrom(this.importService.confirmStrict(state.file, state.yearN, state.yearNMinus1, mapping));
      this.result.set(response);
      this.uploadState.saveResponse(response);
      this.snackBar.open('Importation confirmée avec succès.', 'OK', { duration: 3000 });
    } catch (err: any) {
      this.errorMsg.set(err?.error?.message ?? 'Erreur lors de la confirmation.');
      this.snackBar.open(this.errorMsg(), 'OK', { duration: 5000 });
    } finally {
      this.processing.set(false);
    }
  }

  async confirmPartialImport() {
    if (!this.validateSelections()) return;
    const state = this.uploadStateData();
    const preview = this.previewResponse();
    if (!state || !preview) { this.errorMsg.set('Aucune prévisualisation disponible.'); return; }
    if (this.isHardBlocking()) { this.errorMsg.set('Erreur structurelle bloquante. Impossible d\'importer même partiellement.'); return; }

    const mapping = { kpiNameIndex: this.kpiColumn()!, unitIndex: -1, valueNIndex: this.valueNColumn()!, valueN1Index: this.valueNMinus1Column()!, categoryIndex: -1 };
    this.processing.set(true);
    this.errorMsg.set('');

    try {
      const response = await firstValueFrom(this.importService.confirmPartial(state.file, state.yearN, state.yearNMinus1, mapping));
      this.result.set(response);
      this.uploadState.saveResponse(response);
      const imported = response.qualityReport?.importedRowsCount ?? 0;
      const rejected = response.qualityReport?.rejectedRowsCount ?? 0;
      this.snackBar.open(`Import partiel terminé : ${imported} importées, ${rejected} rejetées.`, 'OK', { duration: 6000 });
    } catch (err: any) {
      this.errorMsg.set(err?.error?.message ?? 'Erreur lors de l\'import partiel.');
      this.snackBar.open(this.errorMsg(), 'OK', { duration: 5000 });
    } finally {
      this.processing.set(false);
    }
  }

  // ── Quality Report helpers ──────────────────────────────────────────────

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

  qualityScorePercent(): number {
    return Math.round(this.qualityReport?.qualityScore ?? 0);
  }

  qualityScoreClass(): string {
    const s = this.qualityScorePercent();
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
    return this.allIssues.some(i => i.code === 'LOW_CONFIDENCE_EXTRACTION');
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

  rowHasError(rowIndex: number): boolean {
    return this.allIssues.some(i => i.rowIndex === rowIndex && i.severity === 'ERROR');
  }

  rowHasWarning(rowIndex: number): boolean {
    return this.allIssues.some(i => i.rowIndex === rowIndex && i.severity === 'WARNING');
  }

  rowIssueTooltip(rowIndex: number): string {
    return this.allIssues
      .filter(i => i.rowIndex === rowIndex)
      .map(i => `[${i.severity}] ${i.message}`)
      .join('\n');
  }

  // ── Helpers existants ───────────────────────────────────────────────────

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
