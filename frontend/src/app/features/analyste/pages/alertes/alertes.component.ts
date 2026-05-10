import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, of } from 'rxjs';
import { DashboardService } from '../../../../core/services/dashboard.service';
import { AlerteKpiItemResponse, AlertesResponse } from '../../../../core/models/dashboard.model';

@Component({
  selector: 'app-alertes',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatTooltipModule,
  ],
  templateUrl: './alertes.component.html',
  styleUrls: ['./alertes.component.css'],
})
export class AlertesComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  data: AlertesResponse | null = null;
  loading = true;
  error = '';

  ngOnInit(): void {
    this.load();
  }

  get byCategorie(): Record<string, AlerteKpiItemResponse[]> {
    if (!this.data?.alertes.length) return {};
    return this.data.alertes.reduce((acc, a) => {
      const key = a.categorieLibelle;
      (acc[key] = acc[key] ?? []).push(a);
      return acc;
    }, {} as Record<string, AlerteKpiItemResponse[]>);
  }

  get categorieKeys(): string[] {
    return Object.keys(this.byCategorie);
  }

  categorieCode(libelle: string): string {
    const kpi = this.data?.alertes.find(a => a.categorieLibelle === libelle);
    return kpi?.categorieCode ?? '';
  }

  tendanceIcon(t: string | null): string {
    if (t === 'HAUSSE') return 'trending_up';
    if (t === 'BAISSE') return 'trending_down';
    return 'trending_flat';
  }

  formatVariation(v: number): string {
    return (v > 0 ? '+' : '') + v.toFixed(1) + '%';
  }

  private load(): void {
    this.loading = true;
    this.dashboardService.getAlertes().pipe(catchError(() => of(null))).subscribe({
      next: (r) => {
        if (r) {
          this.data = r;
        } else {
          this.error = 'Impossible de charger les alertes.';
        }
      },
      complete: () => { this.loading = false; },
    });
  }
}
