import {
  Component, inject, OnInit, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';

import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';

import { DashboardService } from '../../../../core/services/dashboard.service';
import {
  ResumeAnalysteResponse,
  ComparatifTableauResponse,
  GraphiquesDataResponse,
  LigneComparatifResponse,
} from '../../../../core/models/dashboard.model';

import { BarComparisonComponent } from '../../../../shared/components/charts/bar-comparison.component';
import { PieDistributionComponent } from '../../../../shared/components/charts/pie-distribution.component';
import { RadarPerformanceComponent } from '../../../../shared/components/charts/radar-performance.component';
import * as XLSX from 'xlsx';

/** Category metadata for Q / H / S / E */
interface CategorieInfo {
  code: string;
  libelle: string;
  icon: string;
  color: string;
  bgLight: string;
}

const CATEGORIES: CategorieInfo[] = [
  { code: 'Q', libelle: 'Qualité',       icon: 'verified',  color: '#4318FF', bgLight: '#f0edff' },
  { code: 'H', libelle: 'Hygiène',       icon: 'sanitizer', color: '#05cd99', bgLight: '#e8f8f0' },
  { code: 'S', libelle: 'Sécurité',      icon: 'security',  color: '#ee5d50', bgLight: '#ffebee' },
  { code: 'E', libelle: 'Environnement', icon: 'eco',       color: '#ff9800', bgLight: '#fff3e0' },
];

@Component({
  selector: 'app-comparatif',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule,
    MatIconModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatTooltipModule, MatButtonModule,
    BarComparisonComponent, PieDistributionComponent, RadarPerformanceComponent,
  ],
  templateUrl: './comparatif.component.html',
  styleUrls: ['./comparatif.component.css'],
})
export class ComparatifComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private snackBar = inject(MatSnackBar);

  // ── State signals ──────────────────────────────────────────────────────────
  importId     = signal<number | null>(null);
  loading      = signal(true);
  error        = signal('');

  resume      = signal<ResumeAnalysteResponse | null>(null);
  comparatif  = signal<ComparatifTableauResponse | null>(null);
  graphiques  = signal<GraphiquesDataResponse | null>(null);

  // ── Filter signals ─────────────────────────────────────────────────────────
  private searchFilterSig = signal('');
  private niveauFilterSig = signal('');
  private statutFilterSig = signal('');
  activeCatTab    = signal(''); // '' = Tous

  // ── Header Form Filters ────────────────────────────────────────────────────

  // ── Sorting ───────────────────────────────────────────────────────────────
  get searchFilter(): string { return this.searchFilterSig(); }
  set searchFilter(value: string) { this.searchFilterSig.set(value); }

  get niveauFilter(): string { return this.niveauFilterSig(); }
  set niveauFilter(value: string) { this.niveauFilterSig.set(value); }

  get statutFilter(): string { return this.statutFilterSig(); }
  set statutFilter(value: string) { this.statutFilterSig.set(value); }

  get formCatFilter(): string { return this.activeCatTab(); }
  set formCatFilter(value: string) { this.setActiveCat(value); }

  get formKpiFilter(): string { return this.searchFilterSig(); }
  set formKpiFilter(value: string) { this.searchFilterSig.set(value); }

  applyFilters(): void {}

  sortField  = signal<keyof LigneComparatifResponse | ''>('');
  sortAsc    = signal(true);

  // ── Constants ─────────────────────────────────────────────────────────────
  readonly CATEGORIES = CATEGORIES;

  // ── Computed: filtered + sorted table rows ─────────────────────────────────
  filteredLignes = computed(() => {
    const lignes  = this.comparatif()?.lignes ?? [];
    const search  = this.searchFilterSig().toLowerCase();
    const cat     = this.activeCatTab();
    const niveau  = this.niveauFilterSig();
    const statut  = this.statutFilterSig();
    const field   = this.sortField();
    const asc     = this.sortAsc();

    let result = lignes.filter(l =>
      (!search  || l.kpiNom.toLowerCase().includes(search)) &&
      (!cat     || l.categorieCode === cat) &&
      (!niveau  || l.niveauVariation === niveau) &&
      (!statut  || this.getStatut(l.tendance, l.niveauVariation) === statut)
    );

    if (field) {
      result = [...result].sort((a, b) => {
        const av = (a as any)[field];
        const bv = (b as any)[field];
        if (typeof av === 'number' && typeof bv === 'number') {
          return asc ? av - bv : bv - av;
        }
        return asc
          ? String(av ?? '').localeCompare(String(bv ?? ''))
          : String(bv ?? '').localeCompare(String(av ?? ''));
      });
    }

    return result;
  });

  // ── Computed: per-category summary ────────────────────────────────────────
  catStats = computed(() => {
    const lignes = this.filteredLignes() ?? [];
    return CATEGORIES.map(cat => {
      const items    = lignes.filter(l => l.categorieCode === cat.code);
      const critique = items.filter(l => l.niveauVariation === 'CRITIQUE').length;
      const modere   = items.filter(l => l.niveauVariation === 'MODERE').length;
      const faible   = items.filter(l => l.niveauVariation === 'FAIBLE').length;
      const avgVar   = items.length
        ? items.reduce((s, l) => s + l.variationRelative, 0) / items.length
        : 0;
      return { ...cat, total: items.length, critique, modere, faible, avgVar };
    });
  });

  // ── Computed: global KPI counts ───────────────────────────────────────────
  get stats() {
    const lignes = this.filteredLignes() ?? [];
    const total  = lignes.length;
    let amelioration = 0, degradation = 0, stables = 0, sumVar = 0;

    lignes.forEach(l => {
      if (l.variationRelative < -10)      amelioration++;
      else if (l.variationRelative > 10)  degradation++;
      else                                stables++;
      sumVar += l.variationRelative;
    });

    const avg = total > 0 ? sumVar / total : 0;
    return {
      total, amelioration, degradation, stables,
      avgVariation: avg,
      ameliorationPct : total ? +(amelioration / total * 100).toFixed(1) : 0,
      degradationPct  : total ? +(degradation  / total * 100).toFixed(1) : 0,
      stablesPct      : total ? +(stables      / total * 100).toFixed(1) : 0,
    };
  }

  get donutChartData() {
    const lignes = this.filteredLignes() ?? [];
    let enHausseCritique = 0, enHausseModeree = 0, enBaisseModeree = 0, enBaisseFaible = 0;
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

  get dataYearN():  number | null { return this.comparatif()?.periodeN  ?? this.resume()?.periodeN  ?? null; }
  get dataYearN1(): number | null { return this.comparatif()?.periodeN1 ?? this.resume()?.periodeN1 ?? null; }

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit() { this.loadResume(); }

  loadResume() {
    this.dashboardService.getResume().subscribe({
      next: res => {
        this.resume.set(res);
        this.importId.set(res.dernierImportId);
        if (res.dernierImportId) {
          this.loadDashboardData(res.dernierImportId);
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.error.set('Aucune donnée disponible. Veuillez importer des données.');
        this.loading.set(false);
      }
    });
  }

  loadDashboardData(importId: number) {
    this.loading.set(true);
    let done = 0;
    const check = () => { if (++done === 2) this.loading.set(false); };

    this.dashboardService.getComparatif(importId).pipe(
      catchError(() => { this.error.set('Impossible de charger le comparatif.'); return of(null); }),
      finalize(check)
    ).subscribe(r => { if (r) this.comparatif.set(r); });

    this.dashboardService.getGraphiques(importId).pipe(
      catchError(() => of(null)),
      finalize(check)
    ).subscribe(r => { if (r) this.graphiques.set(r); });
  }

  // ── Sorting helper ────────────────────────────────────────────────────────
  sortBy(field: keyof LigneComparatifResponse) {
    if (this.sortField() === field) {
      this.sortAsc.update(v => !v);
    } else {
      this.sortField.set(field);
      this.sortAsc.set(true);
    }
  }

  sortIcon(field: keyof LigneComparatifResponse): string {
    if (this.sortField() !== field) return 'unfold_more';
    return this.sortAsc() ? 'keyboard_arrow_up' : 'keyboard_arrow_down';
  }

  // ── Category tab ──────────────────────────────────────────────────────────
  setActiveCat(code: string) {
    this.activeCatTab.set(code);
  }

  getCatInfo(code: string): CategorieInfo {
    return CATEGORIES.find(c => c.code === code)
      ?? { code, libelle: code, icon: 'label', color: '#a3aed1', bgLight: '#f4f7fe' };
  }

  // ── Filter Applier ────────────────────────────────────────────────────────
  clearFilters(): void {
    this.searchFilter = '';
    this.niveauFilter = '';
    this.statutFilter = '';

    this.setActiveCat('');
    return;
    this.snackBar.open('Filtres appliqués', 'Fermer', { duration: 2500 });
  }

  // ── Display helpers ───────────────────────────────────────────────────────
  getStatut(tendance: string, niveau: string): string {
    if (tendance === 'HAUSSE' && niveau === 'CRITIQUE')                          return 'Dégradation';
    if (tendance === 'HAUSSE' && (niveau === 'MODERE' || niveau === 'FAIBLE'))   return 'Dégradation légère';
    if (tendance === 'BAISSE')                                                    return 'Amélioration';
    return 'Stable';
  }

  getStatutClass(statut: string): string {
    if (statut === 'Dégradation')        return 'statut-red';
    if (statut === 'Dégradation légère') return 'statut-orange';
    if (statut === 'Amélioration')       return 'statut-green';
    return 'statut-blue';
  }

  getNiveauClass(niveau: string): string {
    if (niveau === 'CRITIQUE') return 'niveau-critique';
    if (niveau === 'MODERE')   return 'niveau-modere';
    if (niveau === 'FAIBLE')   return 'niveau-faible';
    return 'niveau-default';
  }

  getTendanceIcon(tendance: string): string {
    if (tendance === 'HAUSSE') return 'trending_up';
    if (tendance === 'BAISSE') return 'trending_down';
    return 'trending_flat';
  }

  getTendanceIconClass(tendance: string): string {
    if (tendance === 'HAUSSE') return 'icon-red';
    if (tendance === 'BAISSE') return 'icon-green';
    return 'icon-blue';
  }

  getVariationClass(value: number): string {
    if (value > 10)  return 'var-neg';
    if (value < -10) return 'var-pos';
    return 'var-neutral';
  }

  variationSign(value: number): string {
    return value > 0 ? '+' : '';
  }

  // ── Export ────────────────────────────────────────────────────────────────
  exportExcel() {
    const rows = this.filteredLignes().map(r => ({
      'Indicateur'              : r.kpiNom,
      'Catégorie'               : r.categorieLibelle,
      'Année N-1'               : r.valeurN1,
      'Année N'                 : r.valeurN,
      'Variation Absolue'       : r.variationAbsolue,
      'Variation Relative (%)'  : r.variationRelative,
      'Niveau'                  : r.niveauVariation,
      'Statut'                  : this.getStatut(r.tendance, r.niveauVariation),
      'Tendance'                : r.tendance,
    }));
    const ws  = XLSX.utils.json_to_sheet(rows);
    const wb  = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Comparatif');
    const buf  = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
    const blob = new Blob([buf], { type: 'application/octet-stream' });
    const url  = window.URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `comparatif-N-vs-N1-${this.importId()}.xlsx`;
    a.click();
    window.URL.revokeObjectURL(url);
    this.snackBar.open('Export Excel téléchargé', 'Fermer', { duration: 3000 });
  }
}
