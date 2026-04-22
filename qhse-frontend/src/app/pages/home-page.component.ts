import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatCardModule, MatIconModule],
  template: `
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">Plateforme SaaS professionnelle</p>
        <h1>QHSE Analytics</h1>
        <p class="hero-text">
          Analysez vos résultats Qualité, Hygiène, Sécurité et Environnement avec
          des tableaux de bord clairs, des comparatifs année N / N-1 et une expérience
          conçue pour les experts QHSE.
        </p>
        <div class="hero-actions">
          <a mat-flat-button color="primary" routerLink="/register">Créer un compte analyste</a>
          <a mat-stroked-button color="primary" routerLink="/login">Se connecter</a>
        </div>
      </div>
      <div class="hero-visual">
        <div class="brand-card">
          <span class="brand-label">QHSE Analytics</span>
          <span class="brand-caption">Votre central QHSE moderne</span>
        </div>
        <div class="stats-panel">
          <div class="stat-card">
            <span class="stat-value">4</span>
            <span class="stat-label">Domaines suivis</span>
          </div>
          <div class="stat-card">
            <span class="stat-value">98%</span>
            <span class="stat-label">Taux de conformité cible</span>
          </div>
          <div class="stat-card">
            <span class="stat-value">24/7</span>
            <span class="stat-label">Support et visibilité</span>
          </div>
        </div>
      </div>
    </section>

    <section class="features">
      <h2>Un environnement professionnel pour vos équipes QHSE</h2>
      <div class="feature-grid">
        <mat-card>
          <mat-icon>insights</mat-icon>
          <h3>Analyse comparative</h3>
          <p>Comparez facilement l’année N et N-1 avec des indicateurs clairs et des alertes visuelles.</p>
        </mat-card>
        <mat-card>
          <mat-icon>verified_user</mat-icon>
          <h3>Sécurité et conformité</h3>
          <p>Suivez les KPI critiques, les tendances et les priorités d’action pour votre entreprise.</p>
        </mat-card>
        <mat-card>
          <mat-icon>support_agent</mat-icon>
          <h3>Accompagnement SaaS</h3>
          <p>Interface responsive, design épuré et accès sécurisé pour les analystes et les administrateurs.</p>
        </mat-card>
      </div>
    </section>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }

    .hero {
      display: grid;
      grid-template-columns: minmax(0, 1.3fr) minmax(320px, 1fr);
      gap: 2rem;
      align-items: center;
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem 1rem;
    }

    .eyebrow {
      margin: 0 0 1rem;
      font-weight: 700;
      color: #0b4a94;
      text-transform: uppercase;
      letter-spacing: 0.2em;
      font-size: 0.85rem;
    }

    h1 {
      margin: 0;
      font-size: clamp(2.75rem, 4vw, 4.5rem);
      line-height: 1.02;
      max-width: 680px;
      color: #102a43;
    }

    .hero-text {
      margin: 1.5rem 0;
      font-size: 1.1rem;
      line-height: 1.75;
      max-width: 680px;
      color: #334e68;
    }

    .hero-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .hero-visual {
      display: grid;
      gap: 1.25rem;
    }

    .brand-card {
      padding: 2rem;
      border-radius: 1.5rem;
      background: linear-gradient(135deg, #0b4a94 0%, #2c6fb8 100%);
      color: white;
      box-shadow: 0 28px 60px rgba(11, 74, 148, 0.16);
    }

    .brand-label {
      display: block;
      font-size: 1.15rem;
      font-weight: 700;
      letter-spacing: 0.04em;
    }

    .brand-caption {
      margin-top: 0.75rem;
      display: block;
      color: rgba(255, 255, 255, 0.85);
      line-height: 1.5;
    }

    .stats-panel {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }

    .stat-card {
      padding: 1.25rem;
      border-radius: 1rem;
      background: #ffffff;
      box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
      display: grid;
      gap: 0.5rem;
    }

    .stat-value {
      font-size: 1.8rem;
      font-weight: 700;
      color: #0b4a94;
    }

    .stat-label {
      color: #334e68;
      font-size: 0.95rem;
    }

    .features {
      max-width: 1200px;
      margin: 3rem auto 0;
      padding: 0 1rem;
    }

    .features h2 {
      margin: 0 0 1.5rem;
      font-size: 2rem;
      color: #102a43;
    }

    .feature-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 1.25rem;
    }

    mat-card {
      padding: 1.5rem;
      border-radius: 1.25rem;
      min-height: 220px;
      transition: transform 0.3s ease, box-shadow 0.3s ease;
    }

    mat-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 20px 45px rgba(11, 74, 148, 0.12);
    }

    mat-icon {
      font-size: 2rem;
      color: #0b4a94;
      margin-bottom: 1rem;
    }

    h3 {
      margin: 0 0 0.75rem;
      color: #102a43;
    }

    p {
      color: #475569;
      margin: 0;
      line-height: 1.75;
    }

    @media (max-width: 960px) {
      .hero {
        grid-template-columns: 1fr;
      }

      .stats-panel {
        grid-template-columns: 1fr;
      }

      .feature-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class HomePage {}
