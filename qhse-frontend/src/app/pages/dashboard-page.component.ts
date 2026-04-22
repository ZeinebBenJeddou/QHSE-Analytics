import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { AuthStorageService } from '../shared/services/auth-storage.service';
import { KpiService } from '../shared/services/kpi.service';
import { Kpi } from '../shared/models/kpi.models';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatIconModule, MatGridListModule],
  template: `
    <section class="dashboard-shell">
      <div class="dashboard-hero">
        <div>
          <p class="eyebrow">Tableau de bord QHSE</p>
          <h1>Bienvenue, {{ authStorage.userName || 'Utilisateur' }}</h1>
          <p>Consultez vos indicateurs, suivez les tendances et accédez aux fonctionnalités qui correspondent à votre rôle.</p>
        </div>

        <div class="hero-actions">
          <a mat-flat-button color="primary" routerLink="/">Retour à l’accueil</a>
          <a mat-flat-button color="accent" routerLink="/kpis">Voir les KPI</a>
        </div>
      </div>

      <div class="stats-grid">
        <mat-card class="stat-card">
          <div class="stat-icon"><mat-icon>insights</mat-icon></div>
          <div>
            <p class="stat-title">KPI actifs</p>
            <p class="stat-number">{{ activeKpiCount }}</p>
          </div>
        </mat-card>

        <mat-card class="stat-card" *ngIf="authStorage.isAdmin()">
          <div class="stat-icon"><mat-icon>restore</mat-icon></div>
          <div>
            <p class="stat-title">KPI inactifs</p>
            <p class="stat-number">{{ inactiveKpiCount }}</p>
          </div>
        </mat-card>

        <mat-card class="stat-card">
          <div class="stat-icon"><mat-icon>category</mat-icon></div>
          <div>
            <p class="stat-title">Catégories de KPI</p>
            <p class="stat-number">{{ categoryCount }}</p>
          </div>
        </mat-card>
      </div>

      <div class="overview-grid">
        <mat-card class="overview-card">
          <h2>Rôle</h2>
          <p>{{ authStorage.userRole || 'Non défini' }}</p>
          <p>Votre accès est géré par le backend. Les admins peuvent modifier et restaurer des KPI, les analystes peuvent consulter les indicateurs actifs.</p>
        </mat-card>

        <mat-card class="overview-card" *ngIf="authStorage.isAnalyste()">
          <h2>Top KPI actifs</h2>
          <ol>
            <li *ngFor="let kpi of topActiveKpis">{{ kpi.nom }} • {{ kpi.categorieLibelle || kpi.categorieCode }}</li>
          </ol>
          <p *ngIf="topActiveKpis.length === 0">Aucun KPI actif pour le moment.</p>
        </mat-card>

        <mat-card class="overview-card" *ngIf="authStorage.isAdmin()">
          <h2>Actions administrateur</h2>
          <p>Utilisez la page KPI pour créer, modifier, supprimer et restaurer des indicateurs métier.</p>
          <p>Le backend valide toutes les requêtes et protège les ressources sensibles via les rôles.</p>
        </mat-card>
      </div>
    </section>
  `,
  styles: [`
    .dashboard-shell {
      display: grid;
      gap: 2rem;
      max-width: 1200px;
      margin: 0 auto;
      padding: 1rem;
    }
    .dashboard-hero {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      gap: 1.5rem;
      align-items: center;
      background: #ffffff;
      padding: 2rem;
      border-radius: 1.25rem;
      box-shadow: 0 24px 70px rgba(15, 23, 42, 0.08);
    }
    .eyebrow {
      margin: 0 0 0.5rem;
      font-size: 0.9rem;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      color: #0b4a94;
    }
    h1 {
      margin: 0 0 1rem;
      font-size: clamp(2rem, 2.5vw, 3rem);
      color: #0f172a;
    }
    p {
      margin: 0;
      color: #475569;
      max-width: 42rem;
      line-height: 1.7;
    }
    .hero-actions {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
    }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 1.5rem;
    }
    .stat-card {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1.5rem;
      border-radius: 1.25rem;
      box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
    }
    .stat-icon {
      width: 3.5rem;
      height: 3.5rem;
      display: grid;
      place-items: center;
      border-radius: 1rem;
      background: rgba(11, 74, 148, 0.08);
      color: #0b4a94;
      font-size: 1.5rem;
    }
    .stat-title {
      margin: 0 0 0.25rem;
      font-weight: 700;
      color: #0f172a;
    }
    .stat-number {
      margin: 0;
      font-size: 2rem;
      color: #0b4a94;
    }
    .overview-grid {
      display: grid;
      gap: 1.5rem;
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .overview-card {
      padding: 1.75rem;
      border-radius: 1.25rem;
      box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
      background: #ffffff;
    }
    .overview-card h2 {
      margin: 0 0 0.75rem;
      color: #0b4a94;
    }
    ol {
      margin: 0;
      padding-left: 1.2rem;
      color: #475569;
    }
    @media (max-width: 960px) {
      .stats-grid,
      .overview-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class DashboardPage implements OnInit {
  activeKpiCount = 0;
  inactiveKpiCount = 0;
  categoryCount = 0;
  topActiveKpis: Kpi[] = [];

  constructor(public authStorage: AuthStorageService, private kpiService: KpiService) {}

  ngOnInit(): void {
    this.loadDashboardMetrics();
  }

  private loadDashboardMetrics(): void {
    this.kpiService.getKpis().subscribe((kpis) => {
      this.activeKpiCount = kpis.length;
      this.topActiveKpis = kpis.slice(0, 3);
    });

    if (this.authStorage.isAdmin()) {
      this.kpiService.getInactiveKpis().subscribe((kpis) => {
        this.inactiveKpiCount = kpis.length;
      });
    }

    this.kpiService.getAllCategories().subscribe((categories) => {
      this.categoryCount = categories.length;
    });
  }
}
