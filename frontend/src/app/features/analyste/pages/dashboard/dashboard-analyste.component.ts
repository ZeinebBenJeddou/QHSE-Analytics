import {
  Component, inject, OnInit, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { catchError, finalize, of } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { DashboardService } from '../../../../core/services/dashboard.service';
import { ImportService } from '../../../../core/services/import.service';
import { KpiEnrichmentService } from '../../../../core/services/kpi-enrichment.service';
import {
  ResumeAnalysteResponse,
  ComparatifTableauResponse,
  GraphiquesDataResponse,
  LigneComparatifResponse,
} from '../../../../core/models/dashboard.model';
import { AnalyseCompleteResponse, ResultatKpiIaResponse } from '../../../../core/models/analyse-ia.model';

import { BarComparisonComponent } from '../../../../shared/components/charts/bar-comparison.component';
import { PieDistributionComponent } from '../../../../shared/components/charts/pie-distribution.component';
import * as XLSX from 'xlsx';

interface SummaryCardVm {
  label: string;
  value: string | number;
  icon: string;
  color: 'blue' | 'red' | 'orange' | 'green';
  note: string;
  ratio?: number;
}

interface InsightCardVm {
  title: string;
  description: string;
  icon: string;
  tone: 'danger' | 'success' | 'warning';
}

@Component({
  selector: 'app-dashboard-analyste',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatChipsModule, MatTooltipModule,
    MatProgressSpinnerModule, MatTabsModule,
    MatInputModule, MatFormFieldModule, MatSelectModule,
    MatExpansionModule, MatSnackBarModule,
    MatBadgeModule, MatDividerModule, MatProgressBarModule,
    BarComparisonComponent, PieDistributionComponent,
  ],
  templateUrl: './dashboard-analyste.component.html',
  styleUrls: ['./dashboard-analyste.component.css'],
})
export class DashboardAnalysteComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dashboardService = inject(DashboardService);
  private importService = inject(ImportService);
  private enrichmentService = inject(KpiEnrichmentService);
  private snackBar = inject(MatSnackBar);

  importId = signal<number | null>(null);
  exporting = signal(false);
  isAnalysing = signal(false);
  loading = signal(true);
  error = signal('');

  resume = signal<ResumeAnalysteResponse | null>(null);

  get hasImport(): boolean {
    return !!this.importId();
  }
  comparatif = signal<ComparatifTableauResponse | null>(null);
  graphiques = signal<GraphiquesDataResponse | null>(null);
  analysesIa = signal<AnalyseCompleteResponse | null>(null);

  searchFilter = signal('');
  categorieFilter = signal('');
  niveauFilter = signal('');
  riskFilter = signal('');
  selectedYearN = signal<number | null>(null);
  selectedYearN1 = signal<number | null>(null);

  private filtersKey = 'analyste.dashboard.filters';

  /** Track which KPI rows are expanded to show full AI analysis */
  expandedRows = signal<Set<number>>(new Set());

  tableColumns = ['expand', 'kpiNom', 'categorieLibelle', 'valeurN1', 'valeurN', 'variationAbsolue', 'variationRelative', 'niveauVariation', 'tendance'];

  filteredLignes = computed(() => {
    const lignes = this.comparatif()?.lignes ?? [];
    const search = this.searchFilter().toLowerCase();
    const cat = this.categorieFilter();
    const niveau = this.niveauFilter();
    const risk = this.riskFilter();
    return lignes.filter(l =>
      (!search || l.kpiNom.toLowerCase().includes(search)) &&
      (!cat || l.categorieCode === cat) &&
      (!niveau || l.niveauVariation === niveau) &&
      (!risk || l.riskLevel === risk)
    );
  });

  categories = computed(() => {
    const lignes = this.comparatif()?.lignes ?? [];
    return [...new Set(lignes.map(l => l.categorieCode))];
  });

  get dataYearN(): number | null {
    return this.comparatif()?.periodeN ?? this.resume()?.periodeN ?? null;
  }

  get dataYearN1(): number | null {
    return this.comparatif()?.periodeN1 ?? this.resume()?.periodeN1 ?? null;
  }

  get analysisDateLabel(): string | null {
    return this.comparatif()?.dateAnalyse ?? this.resume()?.dateAnalyse ?? null;
  }

  get periodLabel(): string {
    const yearN = this.dataYearN;
    if (!yearN) return 'Période en cours';
    return `Janv. - Déc. ${yearN}`;
  }

  get previousPeriodLabel(): string {
    const yearN1 = this.dataYearN1;
    return yearN1 ? String(yearN1) : 'N-1';
  }

  get titleSuffix(): string {
    const yearN = this.dataYearN;
    const yearN1 = this.dataYearN1;
    if (!yearN || !yearN1) return 'Comparatif de performance';
    return `Comparatif Année ${yearN1} vs Année ${yearN}`;
  }

  private formatNumber(value: number): string {
    return value.toLocaleString('fr-FR');
  }

  get summaryCards(): SummaryCardVm[] {
    const resume = this.resume();
    const comparatif = this.comparatif();
    const lignes = comparatif?.lignes ?? [];
    const totalKpis = resume?.nombreTotalKpis ?? lignes.length ?? 0;
    const critiques = resume?.nombreTotalCritiques ?? comparatif?.nombreCritiques ?? 0;

    let enHausse = 0;
    let enBaisse = 0;
    let sumVariation = 0;

    if (lignes.length > 0) {
      lignes.forEach(l => {
        if (l.variationRelative > 0) enHausse++;
        else if (l.variationRelative < 0) enBaisse++;
        sumVariation += l.variationRelative;
      });
    }

    const avgVariation = lignes.length > 0 ? sumVariation / lignes.length : 0;
    const isAmelioration = avgVariation < 0; // Less incidents is better generally, but this is a rough assumption.

    // Difference from previous period if available, otherwise just mock it as +0
    // Actually, we can just say 'vs N-1'
    const noteTotalKpis = `vs N-1`;

    return [
      {
        label: 'INDICATEURS SUIVIS',
        value: totalKpis,
        icon: 'assignment',
        color: 'blue',
        note: noteTotalKpis,
      },
      {
        label: 'TAUX D\'ÉVOLUTION GLOBAL',
        value: `${isAmelioration ? '-' : '+'}${Math.abs(avgVariation).toFixed(1)}%`,
        icon: isAmelioration ? 'trending_down' : 'trending_up',
        color: isAmelioration ? 'green' : 'red',
        note: isAmelioration ? 'Amélioration' : 'Dégradation',
      },
      {
        label: 'INDICATEURS EN HAUSSE',
        value: enHausse,
        icon: 'trending_up',
        color: 'red',
        note: totalKpis ? `${Math.round((enHausse / totalKpis) * 100)}% du total` : '',
      },
      {
        label: 'INDICATEURS EN BAISSE',
        value: enBaisse,
        icon: 'trending_down',
        color: 'green',
        note: totalKpis ? `${Math.round((enBaisse / totalKpis) * 100)}% du total` : '',
      },
      {
        label: 'ALERTES CRITIQUES',
        value: critiques,
        icon: 'warning',
        color: 'red',
        note: 'À traiter en priorité',
      },
    ];
  }

  get insightCards(): InsightCardVm[] {
    const degraded = this.graphiques()?.topKpisDegrades ?? [];
    const comparisons = this.comparatif()?.lignes ?? [];
    const topKpi = degraded[0] ?? comparisons[0];
    const secondKpi = degraded[1] ?? comparisons[1];
    const criticalCount = this.comparatif()?.nombreCritiques ?? 0;
    const incidents = this.aggregateMetric(['incident']).current;

    return [
      {
        title: 'Hausse significative des écarts',
        description: topKpi
          ? `${topKpi.kpiNom} montre la plus forte dégradation observée sur la période.`
          : 'Les écarts principaux doivent être surveillés sur la période sélectionnée.',
        icon: 'warning',
        tone: 'danger',
      },
      {
        title: 'Amélioration des indicateurs clés',
        description: criticalCount > 0
          ? `${criticalCount} KPI(s) restent critiques, mais les tendances globales montrent des actions correctives actives.`
          : 'Les indicateurs critiques sont sous contrôle sur la période analysée.',
        icon: 'trending_down',
        tone: 'success',
      },
      {
        title: 'Tendance à surveiller',
        description: secondKpi
          ? `${secondKpi.kpiNom} et les incidents cumulés (${incidents}) doivent rester prioritaires.`
          : 'Aucun signal supplémentaire prioritaire à ce stade.',
        icon: 'insights',
        tone: 'warning',
      },
    ];
  }

  get donutChartData() {
    const lignes = this.comparatif()?.lignes ?? [];
    let enHausseCritique = 0;
    let enHausseModeree = 0;
    let enBaisseModeree = 0;
    let enBaisseFaible = 0;

    lignes.forEach(l => {
      if (l.tendance === 'HAUSSE') {
        if (l.niveauVariation === 'CRITIQUE') enHausseCritique++;
        else enHausseModeree++;
      } else if (l.tendance === 'BAISSE') {
        if (l.niveauVariation === 'MODERE' || l.niveauVariation === 'CRITIQUE') enBaisseModeree++;
        else enBaisseFaible++;
      }
    });

    return { enHausseCritique, enHausseModeree, enBaisseModeree, enBaisseFaible };
  }

  get recommendationCards(): InsightCardVm[] {
    const criticalCount = this.comparatif()?.nombreCritiques ?? 0;
    const incidents = this.aggregateMetric(['incident', 'accident', 'fréquence', 'gravité']).current;
    const accidents = this.aggregateMetric(['accident']).current;
    const audits = this.aggregateMetric(['audit', 'visite', 'vms']).current;
    const securite = this.aggregateMetric(['sécurité', 'epi', 'situation', 'presque', 'near-miss']).current;
    const qualite = this.aggregateMetric(['non-conformité', 'conformité', 'réclamation']).current;

    return [
      {
        title: 'Renforcer les formations sécurité',
        description: incidents > 0 || accidents > 0
          ? `${incidents} événements sécurité détectés. Planifier des sessions ciblées sur les procédures critiques et les retours d’expérience terrain.`
          : 'Conserver le rythme des sensibilisations sur les bonnes pratiques et les procédures.',
        icon: 'school',
        tone: incidents > 0 ? 'danger' : 'warning',
      },
      {
        title: 'Auditer les processus opérationnels',
        description: audits > 0
          ? `${audits} audits/visites enregistrés. Prioriser les zones où les écarts sont les plus marqués.`
          : 'Lancer un audit ciblé dès que de nouveaux écarts apparaissent.',
        icon: 'manage_search',
        tone: 'danger',
      },
      {
        title: 'Optimiser la charge de travail et les ressources',
        description: criticalCount > 0 || incidents > 0
          ? `${criticalCount} KPI(s) critiques. Organiser un point de pilotage court avec les équipes pour suivre les actions correctives.`
          : 'Maintenir le suivi mensuel avec les responsables de secteur.',
        icon: 'groups',
        tone: criticalCount > 0 ? 'warning' : 'success',
      },
    ];
  }

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.importId.set(+id);
        this.loadSelectedImport(+id);
      } else {
        this.loadResume();
      }
    });

    this.loadSavedFilters();
  }

  loadResume() {
    this.dashboardService.getResume().subscribe({
      next: res => {
        this.resume.set(res);
        this.selectedYearN.set(res.periodeN ?? null);
        this.selectedYearN1.set(res.periodeN1 ?? null);
        this.importId.set(res.dernierImportId);
        this.loadDashboardData(res.dernierImportId, { includeComparatif: true });
      },
      error: err => {
        this.error.set(this.extractErrorMessage(err, 'Aucune donnée disponible. Veuillez importer des données.'));
        this.loading.set(false);
      }
    });
  }

  loadSelectedImport(importId: number) {
    this.loading.set(true);
    this.error.set('');
    this.comparatif.set(null);
    this.graphiques.set(null);
    this.analysesIa.set(null);
    this.expandedRows.set(new Set());

    this.dashboardService.getComparatif(importId).subscribe({
      next: res => {
        this.comparatif.set(res);
        this.selectedYearN.set(res.periodeN ?? null);
        this.selectedYearN1.set(res.periodeN1 ?? null);
        this.loadDashboardData(importId, { includeComparatif: false });
      },
      error: err => {
        this.error.set(this.extractErrorMessage(err, 'Import non disponible. Sélectionnez un import traité depuis l\'historique.'));
        this.loading.set(false);
      }
    });
  }

  // Persist filters locally so the analyst keeps their context
  private loadSavedFilters(): void {
    try {
      const raw = localStorage.getItem(this.filtersKey);
      if (!raw) return;
      const obj = JSON.parse(raw);
      if (obj.search) this.searchFilter.set(obj.search);
      if (obj.categorie) this.categorieFilter.set(obj.categorie);
      if (obj.niveau) this.niveauFilter.set(obj.niveau);
      if (obj.risk) this.riskFilter.set(obj.risk);
    } catch {
      // ignore
    }
  }

  private saveFilters(): void {
    const payload = {
      search: this.searchFilter(),
      categorie: this.categorieFilter(),
      niveau: this.niveauFilter(),
      risk: this.riskFilter(),
    };
    try { localStorage.setItem(this.filtersKey, JSON.stringify(payload)); } catch {}
  }

  onCategoryChange(val: string): void {
    this.categorieFilter.set(val);
    this.saveFilters();
  }

  onSearchChange(val: string): void {
    this.searchFilter.set(val);
    this.saveFilters();
  }

  onPeriodChangeN(val: string): void {
    const n = Number(val) || null;
    this.selectedYearN.set(n);
    // reload data for selected import to reflect period change if relevant
    const id = this.importId();
    if (id) this.loadDashboardData(id, { includeComparatif: true });
  }

  onPeriodChangeN1(val: string): void {
    const n = Number(val) || null;
    this.selectedYearN1.set(n);
    const id = this.importId();
    if (id) this.loadDashboardData(id, { includeComparatif: true });
  }

  loadDashboardData(importId: number, options: { includeComparatif?: boolean } = {}) {
    const { includeComparatif = false } = options;
    this.loading.set(true);
    this.error.set('');

    const endpoints = includeComparatif ? 3 : 2;
    let done = 0;
    const check = () => { if (++done === endpoints) this.loading.set(false); };

    if (includeComparatif) {
      this.dashboardService.getComparatif(importId)
        .pipe(
          catchError(err => {
            this.error.set(this.extractErrorMessage(err, 'Impossible de charger le comparatif.'));
            return of(null);
          }),
          finalize(check)
        )
        .subscribe({ next: r => { if (r) this.comparatif.set(r); } });
    }

    this.dashboardService.getGraphiques(importId)
      .pipe(
        catchError(err => {
          this.error.set(this.extractErrorMessage(err, 'Impossible de charger les graphiques.'));
          return of(null);
        }),
        finalize(check)
      )
      .subscribe({ next: r => {
        if (r) {
          this.graphiques.set(r);
        }
      } });

    this.dashboardService.getAnalysesIa(importId)
      .pipe(
        catchError(err => {
          const msg = this.extractErrorMessage(err, 'Analyse IA indisponible pour le moment.');
          this.snackBar.open(msg, 'OK', { duration: 6000 });
          return of(null);
        }),
        finalize(check)
      )
      .subscribe({ next: r => {
        if (r) {
          this.analysesIa.set(r);
          this.applyAnalysesToComparatif();
        }
      } });
  }

  private applyAnalysesToComparatif(): void {
    const comparatif = this.comparatif();
    const analyses = this.analysesIa();
    if (!comparatif || !analyses?.analysesKpis?.length) return;

    const analysisMap = new Map<number, ResultatKpiIaResponse>();
    analyses.analysesKpis.forEach(item => analysisMap.set(item.kpiId, item));

    const mergedLignes = comparatif.lignes.map(row => {
      const analysis = analysisMap.get(row.kpiId);
      if (!analysis) return row;
      return {
        ...row,
        riskLevel: analysis.riskLevel ?? row.riskLevel,
        riskJustification: analysis.riskJustification ?? row.riskJustification,
        issueDetected: analysis.issueDetected ?? row.issueDetected,
        correctiveAction: analysis.correctiveAction ?? row.correctiveAction,
        preventiveAction: analysis.preventiveAction ?? row.preventiveAction,
        immediateAction: analysis.immediateAction ?? row.immediateAction,
        immediatePriority: analysis.immediatePriority ?? row.immediatePriority,
        requires8d: analysis.requires8d ?? row.requires8d,
        eightDDetails: analysis.eightDDetails ?? row.eightDDetails,
        aiNote: analysis.aiNote ?? row.aiNote,
      };
    });

    this.comparatif.set({ ...comparatif, lignes: mergedLignes });
  }

  // ─────────────────── Row expand / collapse ────────────────────────────

  toggleRow(kpiId: number): void {
    const current = new Set(this.expandedRows());
    if (current.has(kpiId)) {
      current.delete(kpiId);
    } else {
      current.add(kpiId);
    }
    this.expandedRows.set(current);
  }

  isExpanded(kpiId: number): boolean {
    return this.expandedRows().has(kpiId);
  }

  // ─────────────────── 8D parsing ───────────────────────────────────────

  parse8D(json: string | undefined): Record<string, string> | null {
    if (!json) return null;
    try {
      return JSON.parse(json);
    } catch {
      return null;
    }
  }

  eightDSteps(json: string | undefined): { key: string; label: string; value: string }[] {
    const data = this.parse8D(json);
    if (!data) return [];
    const labels: Record<string, string> = {
      D1: 'Équipe',
      D2: 'Description du problème',
      D3: 'Actions de confinement',
      D4: 'Cause racine',
      D5: 'Actions correctives',
      D6: 'Validation',
      D7: 'Prévention',
      D8: 'Clôture',
    };
    return Object.entries(data).map(([key, value]) => ({
      key,
      label: labels[key] ?? key,
      value,
    }));
  }

  // ─────────────────── Helpers ──────────────────────────────────────────

  private extractErrorMessage(err: any, fallback: string): string {
    if (!err) return fallback;
    if (err.error && typeof err.error === 'object' && err.error.message) return err.error.message;
    if (typeof err.error === 'string') return err.error;
    if (err.message) return err.message;
    return fallback;
  }

  variationStyle(val: number): string {
    if (val > 10) return 'critique';
    if (val > 0) return 'hausse';
    if (val < 0) return 'baisse';
    return 'stable';
  }

  catClass(code: string): string {
    const map: Record<string, string> = {
      'Q': 'cat-q',
      'H': 'cat-h',
      'S': 'cat-s',
      'E': 'cat-e',
    };
    return map[code?.toUpperCase()] ?? 'cat-default';
  }

  accentClass(accent: string): string {
    return `accent-${accent}`;
  }

  niveauClass(n: string): string {
    return { CRITIQUE: 'chip-critique', MODERE: 'chip-modere', FAIBLE: 'chip-faible' }[n] ?? '';
  }

  riskClass(r: string | undefined): string {
    if (!r) return 'risk-unknown';
    return { 'Élevé': 'risk-high', 'Modéré': 'risk-medium', 'Faible': 'risk-low' }[r] ?? 'risk-unknown';
  }

  riskIcon(r: string | undefined): string {
    return { 'Élevé': '🔴', 'Modéré': '🟠', 'Faible': '🟢' }[r ?? ''] ?? '⚪';
  }

  priorityClass(p: string | undefined): string {
    if (!p) return '';
    return { 'Haute': 'priority-high', 'Moyenne': 'priority-medium', 'Basse': 'priority-low' }[p] ?? '';
  }

  tendanceIcon(t: string): string {
    return { HAUSSE: 'trending_up', BAISSE: 'trending_down', STABLE: 'trending_flat' }[t] ?? 'remove';
  }

  tendanceClass(t: string): string {
    return { HAUSSE: 'trend-up', BAISSE: 'trend-down', STABLE: 'trend-stable' }[t] ?? '';
  }

  hasDeepAnalysis(row: LigneComparatifResponse): boolean {
    return !!(row.riskLevel || row.aiNote || row.correctiveAction);
  }

  exportPdf() {
    const id = this.importId();
    if (!id) {
      this.snackBar.open('Aucun import sélectionné pour l\'export.', 'OK', { duration: 3000 });
      return;
    }
    this.exporting.set(true);
    this.importService.exportAnalyste(id).subscribe({
      next: (blob) => this.downloadFile(blob, `rapport-import-${id}.pdf`),
      error: () => this.snackBar.open('Erreur lors de l\'export PDF.', 'OK', { duration: 4000 }),
      complete: () => this.exporting.set(false),
    });
  }

  exportExcel() {
    const rows = this.filteredLignes().map(r => ({
      'Indicateur': r.kpiNom,
      'Catégorie': r.categorieLibelle,
      'Année N-1': r.valeurN1,
      'Année N': r.valeurN,
      'Variation Absolue': r.variationAbsolue,
      'Variation Relative (%)': r.variationRelative,
      'Niveau': r.niveauVariation,
      'Tendance': r.tendance,
    }));

    const ws = XLSX.utils.json_to_sheet(rows);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Comparatif');
    const buf = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
    const blob = new Blob([buf], { type: 'application/octet-stream' });
    this.downloadFile(blob, `comparatif-${this.importId() ?? 'resume'}.xlsx`);
  }

  goToIA() {
    const id = this.importId();
    if (!id) {
      this.snackBar.open('Aucun import sélectionné pour l\'analyse IA.', 'OK', { duration: 3000 });
      return;
    }
    this.router.navigate(['/analyste/ia', id]);
  }

  /**
    * Triggers the line-by-line Groq analysis for all KPIs in the current session,
    * with Gemini used as fallback.
   * This generates risk levels, corrective actions, and 8D plans.
   */
  runFullAnalysis() {
    const id = this.importId();
    if (!id) return;

    this.isAnalysing.set(true);
    this.snackBar.open('Analyse IA approfondie en cours (ligne par ligne)...', 'Fermer', { duration: 5000 });

    this.enrichmentService.analyseAll(id, true).subscribe({
      next: (results) => {
        this.snackBar.open(`${results.length} indicateurs analysés avec succès.`, 'OK', { duration: 4000 });
        // Refresh the comparison data and IA analysis to show the new fields
        this.loadSelectedImport(id);
      },
      error: (err) => {
        const msg = this.extractErrorMessage(err, 'Erreur lors de l\'analyse approfondie.');
        this.snackBar.open(msg, 'OK', { duration: 5000 });
      },
      complete: () => {
        this.isAnalysing.set(false);
      }
    });
  }

  private downloadFile(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    window.URL.revokeObjectURL(url);
  }

  goToImport() {
    this.router.navigate(['/analyste/import']);
  }

  private normalizeText(value: string): string {
    return value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  }

  private aggregateMetric(keywords: string[]): { current: number; previous: number } {
    const lignes = this.comparatif()?.lignes ?? [];
    const normalizedKeywords = keywords.map(keyword => this.normalizeText(keyword));
    const matches = lignes.filter(row => {
      const name = this.normalizeText(row.kpiNom);
      const category = this.normalizeText(row.categorieLibelle);
      return normalizedKeywords.some(keyword => name.includes(keyword) || category.includes(keyword));
    });

    const current = matches.reduce((sum, row) => sum + (Number(row.valeurN) || 0), 0);
    const previous = matches.reduce((sum, row) => sum + (Number(row.valeurN1) || 0), 0);
    return {
      current: Math.round(current),
      previous: Math.round(previous),
    };
  }

  private computeRiskScore(rows: LigneComparatifResponse[] = this.comparatif()?.lignes ?? []): number {
    if (!rows.length) return 0;

    const total = rows.length;
    const weighted = rows.reduce((sum, row) => {
      const levelWeight = row.niveauVariation === 'CRITIQUE' ? 3 : row.niveauVariation === 'MODERE' ? 2 : 1;
      const trendPenalty = row.tendance === 'HAUSSE' ? 1 : 0;
      return sum + levelWeight + trendPenalty;
    }, 0);

    return Math.min(100, Math.round((weighted / (total * 4)) * 100));
  }

  private metricNote(current: number, previous: number): string {
    if (previous === 0) {
      return current > 0 ? 'Nouvelle activité' : 'Stable';
    }
    if (current === previous) {
      return 'Stable';
    }
    return current < previous ? 'Amélioration' : 'Détérioration';
  }
}
