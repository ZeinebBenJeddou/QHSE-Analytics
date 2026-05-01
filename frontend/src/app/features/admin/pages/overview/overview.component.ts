import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
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
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule],
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

    this.adminService.getStats().subscribe({
      next: (stats) => this.stats = stats,
      error: () => this.errorMessage = 'Impossible de charger les statistiques.',
      complete: () => this.loading = false,
    });

    this.adminService.getAnalystes().subscribe({
      next: (items) => this.analystes = items,
      error: () => this.errorMessage = 'Impossible de charger la liste des analystes.',
    });

    this.adminService.getKpisCritiques().subscribe({
      next: (items) => this.kpisCritiques = items,
      error: () => this.errorMessage = 'Impossible de charger les KPIs critiques.',
    });

    this.adminService.getRepartition().subscribe({
      next: (data) => this.repartition = data,
      error: () => this.errorMessage = 'Impossible de charger la répartition.',
    });

    this.adminService.getGraphiques().subscribe({
      next: (data) => this.graphiques = data,
      error: () => this.errorMessage = 'Impossible de charger les graphiques.',
    });
  }

  objectKeys(obj: Record<string, unknown> | null): string[] {
    return obj ? Object.keys(obj) : [];
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

        window.setTimeout(() => {
          window.URL.revokeObjectURL(url);
        }, 1000);
      },
      error: () => {
        this.errorMessage = 'Impossible d\'exporter le PDF.';
        this.exportInProgress = false;
      },
      complete: () => { this.exportInProgress = false; },
    });
  }
}