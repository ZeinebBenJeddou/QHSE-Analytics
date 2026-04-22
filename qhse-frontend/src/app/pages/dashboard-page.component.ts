import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { AuthStorageService } from '../shared/services/auth-storage.service';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatIconModule, MatGridListModule],
  template: `
    <section class="dashboard-shell">
      <div class="dashboard-hero">
        <div>
          <p class="eyebrow">Tableau de bord QHSE</p>
          <h1>Bienvenue, {{ authStorage.userName || 'Analyste' }}</h1>
          <p>Consultez vos indicateurs, suivez les tendances et partagez des rapports sécurisés en un seul endroit.</p>
        </div>

        <div class="hero-actions">
          <a mat-flat-button color="primary" routerLink="/">Retour à l’accueil</a>
        </div>
      </div>

      <div class="stats-grid">
        <mat-card class="stat-card">
          <div class="stat-icon"><mat-icon>analytics</mat-icon></div>
          <div>
            <p class="stat-title">KPI actifs</p>
            <p class="stat-number">52</p>
          </div>
        </mat-card>

        <mat-card class="stat-card">
          <div class="stat-icon"><mat-icon>security</mat-icon></div>
          <div>
            <p class="stat-title">Alertes de conformité</p>
            <p class="stat-number">8</p>
          </div>
        </mat-card>

        <mat-card class="stat-card">
          <div class="stat-icon"><mat-icon>share</mat-icon></div>
          <div>
            <p class="stat-title">Rapports partagés</p>
            <p class="stat-number">17</p>
          </div>
        </mat-card>
      </div>

      <mat-card class="overview-card">
        <h2>Votre rôle</h2>
        <p>{{ authStorage.userRole || 'Analyste' }}</p>
        <p>Vous êtes connecté avec un accès sécurisé. Les indicateurs sont protégés par authentification JWT et vérification OTP.</p>
      </mat-card>
    </section>
  `,
  styles: [`
    .dashboard-shell {
      display: grid;
      gap: 2rem;
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 1rem;
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
    @media (max-width: 960px) {
      .stats-grid {
        grid-template-columns: 1fr;
      }
    }
    @media (max-width: 720px) {
      .dashboard-hero {
        padding: 1.5rem;
      }
    }
  `]
})
export class DashboardPage {
  constructor(public authStorage: AuthStorageService) {}
}
