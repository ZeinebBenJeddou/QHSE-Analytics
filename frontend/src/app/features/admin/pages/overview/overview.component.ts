import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AdminService } from '../../../../core/services/admin.service';
import { AdminOverviewData } from './admin-overview.resolver';
import {
  AdminStatsResponse, AdminAnalysteItemResponse,
  AdminKpiCritiqueResponse, AdminGraphiquesDataResponse,
  AdminRepartitionResponse
} from '../../models/admin.models';

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule, MatTooltipModule],
  templateUrl: './overview.component.html',
  styleUrls:  ['./overview.component.css'],
})
export class AdminOverviewComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route        = inject(ActivatedRoute);

  stats:         AdminStatsResponse | null         = null;
  analystes:     AdminAnalysteItemResponse[]        = [];
  kpisCritiques: AdminKpiCritiqueResponse[]         = [];
  repartition:   AdminRepartitionResponse | null    = null;
  graphiques:    AdminGraphiquesDataResponse | null = null;
  loading = false;
  exportInProgress = false;
  errorMessage     = '';

  ngOnInit(): void {
    const data = this.route.snapshot.data['data'] as AdminOverviewData | undefined;

    if (data) {
      this.stats         = data.stats;
      this.analystes     = data.analystes     ?? [];
      this.kpisCritiques = data.kpisCritiques ?? [];
      this.repartition   = data.repartition;
      this.graphiques    = data.graphiques;

      if (!data.stats) {
        this.errorMessage = 'Impossible de charger les statistiques.';
      }
    } else {
      this.loadDashboard();
    }
  }

  loadDashboard(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      stats:         this.adminService.getStats().pipe(catchError(() => of(null))),
      analystes:     this.adminService.getAnalystes().pipe(catchError(() => of([]))),
      kpisCritiques: this.adminService.getKpisCritiques().pipe(catchError(() => of([]))),
      repartition:   this.adminService.getRepartition().pipe(catchError(() => of(null))),
      graphiques:    this.adminService.getGraphiques().pipe(catchError(() => of(null))),
    }).subscribe({
      next: (data) => {
        this.stats         = data.stats;
        this.analystes     = data.analystes     ?? [];
        this.kpisCritiques = data.kpisCritiques ?? [];
        this.repartition   = data.repartition;
        this.graphiques    = data.graphiques;
        this.loading       = false;
        if (!data.stats) {
          this.errorMessage = 'Impossible de charger les statistiques.';
        }
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement du tableau de bord.';
        this.loading      = false;
      },
    });
  }

  objectKeys(obj: Record<string, unknown> | null): string[] {
    return obj ? Object.keys(obj) : [];
  }

  get niveauxList(): Array<{ label: string; count: number; pct: number; color: string }> {
    const n = this.graphiques?.repartitionNiveaux ?? {};
    const total = Object.values(n).reduce((s, v) => s + v, 0);
    const colors: Record<string, string> = { FAIBLE: '#38A169', MODERE: '#DD6B20', CRITIQUE: '#E53E3E' };
    return ['FAIBLE', 'MODERE', 'CRITIQUE'].map(k => ({
      label: k,
      count: n[k] ?? 0,
      pct: total > 0 ? Math.round(((n[k] ?? 0) / total) * 100) : 0,
      color: colors[k],
    }));
  }

  get niveauxTotal(): number {
    return this.niveauxList.reduce((s, l) => s + l.count, 0);
  }

  get donutGradient(): string {
    const list = this.niveauxList;
    const total = list.reduce((s, l) => s + l.count, 0);
    if (total === 0) return 'conic-gradient(#E2EAF6 0deg 360deg)';
    let acc = 0;
    const stops = list.map(l => {
      const from = acc;
      acc += (l.count / total) * 360;
      return `${l.color} ${from}deg ${acc}deg`;
    });
    return `conic-gradient(${stops.join(', ')})`;
  }

  get critiquesParCatList(): Array<{ cat: string; count: number; pct: number }> {
    const m = this.graphiques?.kpisCritiquesByCategorie ?? {};
    const max = Math.max(...Object.values(m), 1);
    return Object.entries(m)
      .sort((a, b) => b[1] - a[1])
      .map(([cat, count]) => ({ cat, count, pct: Math.round((count / max) * 100) }));
  }

  get evolutionMax(): number {
    if (!this.graphiques?.evolutionParCategorie?.length) return 1;
    return Math.max(
      ...this.graphiques.evolutionParCategorie.flatMap(e => [e.valeurMoyenneN1, e.valeurMoyenneN]),
      1,
    );
  }

  evolutionBarPx(value: number): number {
    return Math.max(Math.round((value / this.evolutionMax) * 100), 3);
  }

  exportPdf(): void {
    this.exportInProgress = true;
    this.errorMessage = '';

    this.adminService.exportAdminPdf().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'admin-dashboard-report.pdf';
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.setTimeout(() => window.URL.revokeObjectURL(url), 1000);
        this.exportInProgress = false;
      },
      error: () => {
        this.errorMessage     = 'Impossible d\'exporter le PDF.';
        this.exportInProgress = false;
      },
    });
  }
}
