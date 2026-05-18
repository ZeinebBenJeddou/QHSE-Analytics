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
import { ImportSessionStateService } from '../../../../core/services/import-session-state.service';
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
  badge: string;
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
  private sessionState = inject(ImportSessionStateService);

  importId = signal<number | null>(null);
  exporting = signal(false);
  isAnalysing = signal(false);
  private analysisAutoTriggered = false;
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

  sortColumn = signal<string>('variationAbsolue');
  sortDir = signal<'asc' | 'desc'>('desc');

  private filtersKey = 'analyste.dashboard.filters';

  expandedRows = signal<Set<number>>(new Set());

  tableColumns = ['expand', 'kpiNom', 'categorieLibelle', 'valeurN1', 'valeurN', 'variationAbsolue', 'variationRelative', 'niveauVariation', 'tendance'];

  filteredLignes = computed(() => {
    const lignes = this.comparatif()?.lignes ?? [];
    const search = this.searchFilter().toLowerCase();
    const cat = this.categorieFilter();
    const niveau = this.niveauFilter();
    const risk = this.riskFilter();
    const filtered = lignes.filter(l =>
      (!search || l.kpiNom.toLowerCase().includes(search)) &&
      (!cat || l.categorieCode === cat) &&
      (!niveau || l.niveauVariation === niveau) &&
      (!risk || l.riskLevel === risk)
    );
    const col = this.sortColumn();
    const dir = this.sortDir();
    return [...filtered].sort((a, b) => {
      const aVal = (a as any)[col] ?? 0;
      const bVal = (b as any)[col] ?? 0;
      if (typeof aVal === 'string') {
        return dir === 'asc' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
      }
      return dir === 'asc' ? aVal - bVal : bVal - aVal;
    });
  });

  maxVariationAbs = computed(() => {
    const lignes = this.comparatif()?.lignes ?? [];
    return Math.max(...lignes.map(l => Math.abs(l.variationRelative)), 1);
  });

  categories = computed(() => {
    const lignes = this.comparatif()?.lignes ?? [];
    return [...new Set(lignes.map(l => l.categorieCode))];
  });

  recommendationCards = computed((): InsightCardVm[] => {
    const lignes = this.filteredLignes();
    const priorityOrder: Record<string, number> = { 'Haute': 3, 'Moyenne': 2, 'Basse': 1 };

    const withActions = [...lignes]
      .filter(l => l.immediateAction || l.correctiveAction || l.preventiveAction)
      .sort((a, b) =>
        ((priorityOrder[b.immediatePriority ?? ''] ?? 0) - (priorityOrder[a.immediatePriority ?? ''] ?? 0)) ||
        (Math.abs(b.variationRelative) - Math.abs(a.variationRelative))
      );

    if (!withActions.length) {
      
      return [];
    }

    return withActions.slice(0, 3).map(l => ({
      title: l.kpiNom,
      description: l.immediateAction ?? l.correctiveAction ?? l.preventiveAction ?? '',
      icon: l.immediatePriority === 'Haute' ? 'bolt' : l.immediatePriority === 'Moyenne' ? 'build_circle' : 'shield',
      tone: (l.immediatePriority === 'Haute' ? 'danger' : l.immediatePriority === 'Moyenne' ? 'warning' : 'success') as 'danger' | 'success' | 'warning',
      badge: l.immediatePriority === 'Haute' ? 'Priorité haute' : l.immediatePriority === 'Moyenne' ? 'Priorité moyenne' : 'Priorité basse',
    }));
  });

  hasAiData = computed(() =>
    (this.comparatif()?.lignes ?? []).some(k =>
      k.aiNote || k.riskJustification || k.issueDetected ||
      k.immediateAction || k.correctiveAction || k.riskLevel
    )
  );

  topIssues = computed((): LigneComparatifResponse[] => {
    const lignes = this.comparatif()?.lignes ?? [];
    const priorityOrder: Record<string, number> = { CRITIQUE: 3, MODERE: 2, FAIBLE: 1 };

    const critical = [...lignes]
      .filter(k => k.niveauVariation === 'CRITIQUE' || k.niveauVariation === 'MODERE')
      .sort((a, b) =>
        (priorityOrder[b.niveauVariation ?? 'FAIBLE'] ?? 0) -
        (priorityOrder[a.niveauVariation ?? 'FAIBLE'] ?? 0)
      )
      .slice(0, 6);

    if (critical.length > 0) return critical;

    return [...lignes]
      .filter(k => k.riskJustification || k.aiNote || k.riskLevel || k.issueDetected)
      .sort((a, b) => Math.abs(b.variationRelative ?? 0) - Math.abs(a.variationRelative ?? 0))
      .slice(0, 6);
  });

  topActions = computed((): LigneComparatifResponse[] => {
    const levelOrder: Record<string, number> = {
      Haute: 5, CRITIQUE: 4, Moyenne: 3, MODERE: 2, Basse: 1, FAIBLE: 0
    };
    return [...this.comparatif()?.lignes ?? []]
      .filter(k => !!(k.immediateAction || k.correctiveAction || k.preventiveAction || k.riskJustification))
      .sort((a, b) => {
        const pa = levelOrder[a.immediatePriority ?? a.niveauVariation ?? 'Basse'] ?? 0;
        const pb = levelOrder[b.immediatePriority ?? b.niveauVariation ?? 'Basse'] ?? 0;
        return pb - pa;
      })
      .slice(0, 5);
  });

  dynamicSynthese = computed((): string | null => {
    const global = this.analysesIa()?.analyseGlobale?.synthese;
    if (global) return global;

    const withAi = [...this.comparatif()?.lignes ?? []]
      .filter(k => k.aiNote || k.riskJustification || k.issueDetected)
      .slice(0, 3);

    if (!withAi.length) return null;

    return withAi
      .map(k => `• ${k.kpiNom} : ${k.aiNote ?? k.riskJustification ?? k.issueDetected}`)
      .join('\n');
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

  get summaryCards(): SummaryCardVm[] {
    const resume = this.resume();
    const comparatif = this.comparatif();
    const lignes = comparatif?.lignes ?? [];
    const totalKpis = resume?.nombreTotalKpis ?? lignes.length ?? 0;
    const critiques     = resume?.nombreTotalCritiques  ?? comparatif?.nombreCritiques ?? 0;
    const preEscalades  = resume?.nombreTotalPreEscalades  ?? 0;
    const moderes       = resume?.nombreTotalModeres    ?? comparatif?.nombreModeres   ?? 0;
    const faibles       = resume?.nombreTotalFaibles    ?? comparatif?.nombreFaibles   ?? 0;
    const excellents    = resume?.nombreTotalExcellents    ?? 0;
    const indetermines  = resume?.nombreTotalIndetermines  ?? 0;

    return [
      {
        label: 'INDICATEURS SUIVIS',
        value: totalKpis,
        icon: 'assignment',
        color: 'blue',
        note: `${this.dataYearN1 ?? 'N-1'} vs ${this.dataYearN ?? 'N'}`,
      },
      {
        label: 'ALERTES CRITIQUES',
        value: critiques,
        icon: 'warning',
        color: 'red',
        note: critiques > 0 ? `${critiques} à traiter en priorité` : 'Tous les seuils respectés',
      },
      {
        label: 'PRÉ-ESCALADE',
        value: preEscalades,
        icon: 'trending_up',
        color: 'orange',
        note: totalKpis ? `${Math.round((preEscalades / totalKpis) * 100)}% du total` : '',
      },
      {
        label: 'MODÉRÉS',
        value: moderes,
        icon: 'swap_vert',
        color: 'orange',
        note: totalKpis ? `${Math.round((moderes / totalKpis) * 100)}% du total` : '',
      },
      {
        label: 'FAIBLES',
        value: faibles,
        icon: 'trending_down',
        color: 'green',
        note: totalKpis ? `${Math.round((faibles / totalKpis) * 100)}% du total` : '',
      },
      {
        label: 'EXCELLENTS',
        value: excellents,
        icon: 'star',
        color: 'green',
        note: totalKpis ? `${Math.round((excellents / totalKpis) * 100)}% du total` : '',
      },
      {
        label: 'INDÉTERMINÉS',
        value: indetermines,
        icon: 'help_outline',
        color: 'blue',
        note: indetermines > 0 ? 'Données N-1 manquantes' : 'Aucun KPI indéterminé',
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
        this.sessionState.setActiveImport(res.dernierImportId);
        this.sessionState.patch({ resume: res });
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
    this.analysisAutoTriggered = false;
    this.sessionState.setActiveImport(importId);

    this.dashboardService.getComparatif(importId).subscribe({
      next: res => {
        this.comparatif.set(res);
        this.selectedYearN.set(res.periodeN ?? null);
        this.selectedYearN1.set(res.periodeN1 ?? null);
        this.sessionState.patch({ comparatif: res });
        this.loadDashboardData(importId, { includeComparatif: false });
      },
      error: err => {
        this.error.set(this.extractErrorMessage(err, 'Import non disponible. Sélectionnez un import traité depuis l\'historique.'));
        this.loading.set(false);
      }
    });
  }


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

  onNiveauChange(val: string): void {
    this.niveauFilter.set(val);
    this.saveFilters();
  }

  toggleSort(col: string): void {
    if (this.sortColumn() === col) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortColumn.set(col);
      this.sortDir.set('desc');
    }
  }

  sortIcon(col: string): string {
    if (this.sortColumn() !== col) return 'unfold_more';
    return this.sortDir() === 'asc' ? 'arrow_upward' : 'arrow_downward';
  }

  getBarWidth(val: number): string {
    const max = this.maxVariationAbs();
    if (!max) return '0%';
    return Math.min(100, Math.round((Math.abs(val) / max) * 100)) + '%';
  }

  variationBarClass(row: LigneComparatifResponse): string {
    if (row.tendance === 'HAUSSE' && row.niveauVariation === 'CRITIQUE') return 'bar-critique';
    if (row.tendance === 'HAUSSE') return 'bar-hausse';
    if (row.tendance === 'BAISSE') return 'bar-baisse';
    return 'bar-stable';
  }

  catLabel(code: string): string {
    const map: Record<string, string> = { Q: 'Qualité', H: 'Hygiène', S: 'Sécurité', E: 'Environnement' };
    return map[code?.toUpperCase()] ?? code;
  }

  catCount(code: string): number {
    return (this.comparatif()?.lignes ?? []).filter(l => l.categorieCode === code).length;
  }

  onPeriodChangeN(val: string): void {
    const n = Number(val) || null;
    this.selectedYearN.set(n);
    
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
          this.sessionState.patch({ graphiques: r });
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
        if (r && (r.analysesKpis?.length ?? 0) > 0) {
          this.analysesIa.set(r);
          this.sessionState.patch({ analysesIa: r });
          this.applyAnalysesToComparatif();
          this.analysisAutoTriggered = false;
        } else if (!this.analysisAutoTriggered && !this.isAnalysing()) {
          const id = this.importId();
          if (id && (this.comparatif()?.lignes?.length ?? 0) > 0) {
            this.analysisAutoTriggered = true;
            this.runFullAnalysis();
          }
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

 
  runFullAnalysis() {
    const id = this.importId();
    if (!id) return;

    this.analysisAutoTriggered = false;
    this.isAnalysing.set(true);
    this.snackBar.open('Analyse IA approfondie en cours (ligne par ligne)...', 'Fermer', { duration: 5000 });

    this.enrichmentService.analyseAll(id, true).subscribe({
      next: (results) => {
        this.snackBar.open(`${results.length} indicateurs analysés avec succès.`, 'OK', { duration: 4000 });
        
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
}

