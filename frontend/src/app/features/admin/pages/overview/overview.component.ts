import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdminService } from '../../../../core/services/admin.service';
import { AdminStatsResponse, AdminAnalysteItemResponse, AdminKpiCritiqueResponse } from '../../models/admin.models';

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})
export class AdminOverviewComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  stats: AdminStatsResponse | null = null;
  analystes: AdminAnalysteItemResponse[] = [];
  kpisCritiques: AdminKpiCritiqueResponse[] = [];
  loading = true;
  exportInProgress = false;
  errorMessage = '';

  ngOnInit(): void {
    Promise.resolve().then(() => this.loadDashboard());
  }

  loadDashboard(): void {
    this.loading = true;
    this.errorMessage = '';

    this.adminService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
      },
      error: () => {
        this.errorMessage = 'Unable to load admin statistics.';
      }
    });

    this.adminService.getAnalystes().subscribe({
      next: (items) => {
        this.analystes = items;
      },
      error: () => {
        this.errorMessage = 'Unable to load analyst list.';
      }
    });

    this.adminService.getKpisCritiques().subscribe({
      next: (items) => {
        this.kpisCritiques = items;
      },
      error: () => {
        this.errorMessage = 'Unable to load critical KPIs.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  exportPdf(): void {
    this.exportInProgress = true;
    this.adminService.exportAdminPdf().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'admin-dashboard-report.pdf';
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.errorMessage = 'Unable to export PDF at this time.';
      },
      complete: () => {
        this.exportInProgress = false;
      }
    });
  }
}
