import {
  Component, inject, OnInit, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
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

import { DashboardService } from '../../../../core/services/dashboard.service';
import { ImportService } from '../../../../core/services/import.service';
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
    BarComparisonComponent, RadarPerformanceComponent, PieDistributionComponent,
  ],
  templateUrl: './dashboard-analyste.component.html',
  styleUrls: ['./dashboard-analyste.component.css'],
})
export class DashboardAnalysteComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dashboardService = inject(DashboardService);
  private snackBar = inject(MatSnackBar);

  importId = signal<number | null>(null);
  loading = signal(true);
  error = signal('');

  resume = signal<ResumeAnalysteResponse | null>(null);
  comparatif = signal<ComparatifTableauResponse | null>(null);
  graphiques = signal<GraphiquesDataResponse | null>(null);
  analysesIa = signal<AnalyseCompleteResponse | null>(null);

  searchFilter = signal('');
  categorieFilter = signal('');
  niveauFilter = signal('');

  tableColumns = ['kpiNom', 'categorieLibelle', 'valeurN1', 'valeurN', 'variationAbsolue', 'variationRelative', 'niveauVariation', 'tendance', 'analyseIa'];

  filteredLignes = computed(() => {
    const lignes = this.comparatif()?.lignes ?? [];
    const search = this.searchFilter().toLowerCase();
    const cat = this.categorieFilter();
    const niveau = this.niveauFilter();
    return lignes.filter(l =>
      (!search || l.kpiNom.toLowerCase().includes(search)) &&
      (!cat || l.categorieCode === cat) &&
      (!niveau || l.niveauVariation === niveau)
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
        this.loadAll(+id);
      } else {
        // Load most recent
        this.loadResume();
      }
    });
  }

  loadResume() {
    this.dashboardService.getResume().subscribe({
      next: res => {
        this.resume.set(res);
        this.importId.set(res.dernierImportId);
        this.loadAll(res.dernierImportId);
      },
      error: () => {
        this.error.set('Aucune donnée disponible. Veuillez importer des données.');
        this.loading.set(false);
      }
    });
  }

  loadAll(importId: number) {
    this.loading.set(true);
    this.error.set('');
    let done = 0;
    const check = () => { if (++done === 3) this.loading.set(false); };

    this.dashboardService.getComparatif(importId).subscribe({
      next: r => { this.comparatif.set(r); check(); },
      error: () => check(),
    });
    this.dashboardService.getGraphiques(importId).subscribe({
      next: r => { this.graphiques.set(r); check(); },
      error: () => check(),
    });
    this.dashboardService.getAnalysesIa(importId).subscribe({
      next: r => { this.analysesIa.set(r); check(); },
      error: () => check(),
    });
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

  tendanceIcon(t: string): string {
    return { HAUSSE: 'trending_up', BAISSE: 'trending_down', STABLE: 'trending_flat' }[t] ?? 'remove';
  }

  tendanceClass(t: string): string {
    return { HAUSSE: 'trend-up', BAISSE: 'trend-down', STABLE: 'trend-stable' }[t] ?? '';
  }

  goToIA() {
    const id = this.importId();
    if (id) this.router.navigate(['/analyste/ia', id]);
  }

  goToImport() {
    this.router.navigate(['/analyste/import']);
  }
}
