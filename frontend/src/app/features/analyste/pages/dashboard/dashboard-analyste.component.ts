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
import { AnalyseCompleteResponse } from '../../../../core/models/analyse-ia.model';

import { BarComparisonComponent } from '../../../../shared/components/charts/bar-comparison.component';
import { RadarPerformanceComponent } from '../../../../shared/components/charts/radar-performance.component';
import { PieDistributionComponent } from '../../../../shared/components/charts/pie-distribution.component';

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
    BarComparisonComponent, RadarPerformanceComponent, PieDistributionComponent,
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

  /** Track which KPI rows are expanded to show full AI analysis */
  expandedRows = signal<Set<number>>(new Set());

  tableColumns = ['expand', 'kpiNom', 'categorieLibelle', 'valeurN1', 'valeurN', 'variationRelative', 'status', 'niveauVariation', 'riskBadge', 'tendance', 'aiNote'];

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
  }

  loadResume() {
    this.dashboardService.getResume().subscribe({
      next: res => {
        this.resume.set(res);
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
        this.loadDashboardData(importId, { includeComparatif: false });
      },
      error: err => {
        this.error.set(this.extractErrorMessage(err, 'Import non disponible. Sélectionnez un import traité depuis l\'historique.'));
        this.loading.set(false);
      }
    });
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
      .subscribe({ next: r => { if (r) this.graphiques.set(r); } });

    this.dashboardService.getAnalysesIa(importId)
      .pipe(
        catchError(err => {
          const msg = this.extractErrorMessage(err, 'Analyse IA indisponible pour le moment.');
          this.snackBar.open(msg, 'OK', { duration: 6000 });
          return of(null);
        }),
        finalize(check)
      )
      .subscribe({ next: r => { if (r) this.analysesIa.set(r); } });
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

  get summaryCards() {
    const c = this.comparatif();
    return [
      { label: 'Total KPIs', value: (c?.lignes.length ?? 0), icon: 'analytics', color: 'blue' },
      { label: 'Critiques', value: (c?.nombreCritiques ?? 0), icon: 'warning', color: 'red' },
      { label: 'Modérés', value: (c?.nombreModeres ?? 0), icon: 'info', color: 'orange' },
      { label: 'Faibles', value: (c?.nombreFaibles ?? 0), icon: 'check_circle', color: 'green' },
    ];
  }

  variationStyle(val: number): string {
    if (val > 10) return 'critique';
    if (val > 0) return 'hausse';
    if (val < 0) return 'baisse';
    return 'stable';
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

  goToIA() {
    const id = this.importId();
    if (!id) {
      this.snackBar.open('Aucun import sélectionné pour l\'analyse IA.', 'OK', { duration: 3000 });
      return;
    }
    this.router.navigate(['/analyste/ia', id]);
  }

  /**
   * Triggers the line-by-line Gemini analysis for all KPIs in the current session.
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
        // Refresh the comparison data to show the new analysis fields
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
