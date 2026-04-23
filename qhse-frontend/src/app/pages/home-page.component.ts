import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, MatButtonModule, RouterModule],
  template: `
    <div class="page-wrapper">

      <!-- Animated background grid -->
      <div class="bg-grid"></div>
      <div class="bg-orb orb-1"></div>
      <div class="bg-orb orb-2"></div>
      <div class="bg-orb orb-3"></div>

      <!-- ─── NAVBAR ─── -->
      <nav class="navbar">
        <div class="nav-inner">
          <div class="brand">
            <div class="brand-icon">
              <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                <path d="M14 2L24 8V20L14 26L4 20V8L14 2Z" stroke="#1E6FD9" stroke-width="2" fill="none"/>
                <path d="M14 7L20 10.5V17.5L14 21L8 17.5V10.5L14 7Z" fill="#1E6FD9" opacity="0.3"/>
                <circle cx="14" cy="14" r="3" fill="#1E6FD9"/>
              </svg>
            </div>
            <span class="brand-name">QHSE <strong>Analytics</strong></span>
          </div>
          <div class="nav-links">
            <a class="nav-link" href="#features">Fonctionnalités</a>
            <a class="nav-link" href="#stats">Indicateurs</a>
            <a class="nav-link" href="#about">À propos</a>
          </div>
          <div class="nav-cta">
            <a routerLink="/login" class="btn-nav-outline">Connexion</a>
            <a routerLink="/register" class="btn-nav-filled">Commencer</a>
          </div>
        </div>
      </nav>

      <!-- ─── HERO ─── -->
      <section class="hero">
        <div class="hero-content">
          <div class="hero-badge">
            <span class="badge-dot"></span>
            Plateforme IA nouvelle génération
          </div>

          <h1 class="hero-title">
            Analysez vos performances<br>
            <span class="title-accent">QHSE avec l'IA</span>
          </h1>

          <p class="hero-desc">
            Comparez automatiquement vos indicateurs N-1 vs N, détectez les dérives critiques
            et générez des synthèses intelligentes grâce aux modèles de langage avancés.
          </p>

          <div class="hero-actions">
            <a routerLink="/register" class="btn-primary">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
              Démarrer gratuitement
            </a>
            <a routerLink="/login" class="btn-ghost">
              Se connecter
            </a>
          </div>

          <div class="hero-trust">
            <div class="trust-item">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              <span>Sécurité JWT</span>
            </div>
            <div class="trust-sep">·</div>
            <div class="trust-item">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              <span>Analyse en temps réel</span>
            </div>
            <div class="trust-sep">·</div>
            <div class="trust-item">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              <span>Export PDF/Excel</span>
            </div>
          </div>
        </div>

        <!-- Dashboard Preview Card -->
        <div class="hero-visual">
          <div class="dashboard-card">
            <div class="dc-header">
              <div class="dc-dots">
                <span></span><span></span><span></span>
              </div>
              <span class="dc-title">Tableau de bord QHSE — 2025</span>
            </div>
            <div class="dc-body">
              <!-- KPI Row -->
              <div class="kpi-row">
                <div class="kpi-card kpi-green">
                  <div class="kpi-label">Conformité</div>
                  <div class="kpi-value">94.2%</div>
                  <div class="kpi-trend up">▲ +3.1%</div>
                </div>
                <div class="kpi-card kpi-red">
                  <div class="kpi-label">Non-conformités</div>
                  <div class="kpi-value">12</div>
                  <div class="kpi-trend down">▼ -5</div>
                </div>
                <div class="kpi-card kpi-blue">
                  <div class="kpi-label">Accidents</div>
                  <div class="kpi-value">2</div>
                  <div class="kpi-trend up">▼ -3</div>
                </div>
              </div>
              <!-- Mini bar chart -->
              <div class="mini-chart">
                <div class="chart-label">Évolution mensuelle (NC)</div>
                <div class="bars">
                  <div class="bar-group" *ngFor="let b of bars">
                    <div class="bar bar-prev" [style.height]="b.prev + 'px'" title="N-1"></div>
                    <div class="bar bar-curr" [style.height]="b.curr + 'px'" title="N"></div>
                  </div>
                </div>
                <div class="chart-legend">
                  <span class="leg leg-prev">2024</span>
                  <span class="leg leg-curr">2025</span>
                </div>
              </div>
              <!-- AI insight -->
              <div class="ai-insight">
                <div class="ai-icon">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M4.22 4.22l2.12 2.12M17.66 17.66l2.12 2.12M2 12h3M19 12h3M4.22 19.78l2.12-2.12M17.66 6.34l2.12-2.12"/></svg>
                </div>
                <p class="ai-text">
                  <strong>Analyse IA :</strong> La baisse des non-conformités (-29%) reflète l'efficacité des actions correctives du T3.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ─── FEATURES ─── -->
      <section id="features" class="features-section">
        <div class="section-label">Fonctionnalités clés</div>
        <h2 class="section-title">Tout ce dont vous avez besoin<br>pour piloter votre QHSE</h2>

        <div class="features-grid">
          <div class="feat-card" *ngFor="let f of features; let i = index" [style.animation-delay]="(i * 0.1) + 's'">
            <div class="feat-icon" [innerHTML]="f.icon"></div>
            <h3 class="feat-title">{{ f.title }}</h3>
            <p class="feat-desc">{{ f.desc }}</p>
          </div>
        </div>
      </section>

      <!-- ─── STATS BANNER ─── -->
      <section id="stats" class="stats-section">
        <div class="stats-inner">
          <div class="stat-item" *ngFor="let s of stats">
            <div class="stat-value">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </section>

      <!-- ─── CTA ─── -->
      <section class="cta-section">
        <div class="cta-box">
          <div class="cta-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
          </div>
          <h2 class="cta-title">Prêt à transformer votre analyse QHSE ?</h2>
          <p class="cta-desc">Rejoignez les organisations qui pilotent leur performance avec l'intelligence artificielle.</p>
          <div class="cta-actions">
            <a routerLink="/register" class="btn-cta-white">Créer un compte</a>
            <a routerLink="/login" class="btn-cta-outline">Se connecter</a>
          </div>
        </div>
      </section>

      <!-- ─── FOOTER ─── -->
      <footer class="footer">
        <div class="footer-inner">
          <div class="brand">
            <div class="brand-icon">
              <svg width="22" height="22" viewBox="0 0 28 28" fill="none">
                <path d="M14 2L24 8V20L14 26L4 20V8L14 2Z" stroke="#1E6FD9" stroke-width="2" fill="none"/>
                <circle cx="14" cy="14" r="3" fill="#1E6FD9"/>
              </svg>
            </div>
            <span class="brand-name" style="font-size:0.95rem">QHSE <strong>Analytics</strong></span>
          </div>
          <p class="footer-copy">© 2025 Qualipro by Imagine Human — Tous droits réservés</p>
        </div>
      </footer>

    </div>
  `,
  styles: [`
    /* ── RESET & BASE ── */
    * { box-sizing: border-box; margin: 0; padding: 0; }

    .page-wrapper {
      min-height: 100vh;
      background: #F4F7FF;
      font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
      color: #0D1B3E;
      overflow-x: hidden;
      position: relative;
    }

    /* ── ANIMATED BACKGROUND ── */
    .bg-grid {
      position: fixed;
      inset: 0;
      background-image:
        linear-gradient(rgba(30,111,217,0.04) 1px, transparent 1px),
        linear-gradient(90deg, rgba(30,111,217,0.04) 1px, transparent 1px);
      background-size: 48px 48px;
      pointer-events: none;
      z-index: 0;
    }
    .bg-orb {
      position: fixed;
      border-radius: 50%;
      filter: blur(80px);
      pointer-events: none;
      z-index: 0;
      animation: floatOrb 12s ease-in-out infinite;
    }
    .orb-1 { width: 500px; height: 500px; background: rgba(30,111,217,0.10); top: -150px; right: -100px; animation-delay: 0s; }
    .orb-2 { width: 350px; height: 350px; background: rgba(100,180,255,0.08); bottom: 10%; left: -80px; animation-delay: -4s; }
    .orb-3 { width: 280px; height: 280px; background: rgba(30,111,217,0.06); top: 45%; left: 40%; animation-delay: -8s; }
    @keyframes floatOrb {
      0%, 100% { transform: translate(0, 0) scale(1); }
      50% { transform: translate(20px, -30px) scale(1.05); }
    }

    /* ── NAVBAR ── */
    .navbar {
      position: sticky;
      top: 0;
      z-index: 100;
      background: rgba(255,255,255,0.85);
      backdrop-filter: blur(16px);
      border-bottom: 1px solid rgba(30,111,217,0.10);
    }
    .nav-inner {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 2rem;
      height: 64px;
      display: flex;
      align-items: center;
      gap: 2rem;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      text-decoration: none;
      color: inherit;
      flex-shrink: 0;
    }
    .brand-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      background: rgba(30,111,217,0.08);
      border-radius: 8px;
    }
    .brand-name {
      font-size: 1.05rem;
      letter-spacing: -0.02em;
      color: #0D1B3E;
    }
    .brand-name strong { color: #1E6FD9; }
    .nav-links {
      display: flex;
      gap: 0.25rem;
      margin-left: auto;
    }
    .nav-link {
      padding: 0.4rem 0.9rem;
      border-radius: 6px;
      text-decoration: none;
      font-size: 0.875rem;
      color: #4A5568;
      transition: all 0.2s;
    }
    .nav-link:hover { background: rgba(30,111,217,0.07); color: #1E6FD9; }
    .nav-cta { display: flex; gap: 0.6rem; flex-shrink: 0; }
    .btn-nav-outline {
      padding: 0.45rem 1.1rem;
      border-radius: 8px;
      border: 1.5px solid #1E6FD9;
      color: #1E6FD9;
      font-size: 0.875rem;
      text-decoration: none;
      font-weight: 500;
      transition: all 0.2s;
    }
    .btn-nav-outline:hover { background: rgba(30,111,217,0.06); }
    .btn-nav-filled {
      padding: 0.45rem 1.1rem;
      border-radius: 8px;
      background: #1E6FD9;
      color: white;
      font-size: 0.875rem;
      text-decoration: none;
      font-weight: 500;
      transition: all 0.2s;
    }
    .btn-nav-filled:hover { background: #1558B0; }

    /* ── HERO ── */
    .hero {
      position: relative;
      z-index: 1;
      max-width: 1200px;
      margin: 0 auto;
      padding: 5rem 2rem 4rem;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 4rem;
      align-items: center;
    }
    .hero-content { display: flex; flex-direction: column; gap: 1.5rem; }

    .hero-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.35rem 0.9rem;
      background: rgba(30,111,217,0.08);
      border: 1px solid rgba(30,111,217,0.2);
      border-radius: 100px;
      font-size: 0.8rem;
      font-weight: 600;
      color: #1E6FD9;
      letter-spacing: 0.03em;
      width: fit-content;
      animation: fadeSlideIn 0.6s ease both;
    }
    .badge-dot {
      width: 7px; height: 7px;
      background: #1E6FD9;
      border-radius: 50%;
      animation: pulse 2s ease-in-out infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.5; transform: scale(0.8); }
    }

    .hero-title {
      font-size: clamp(2rem, 3.5vw, 3rem);
      font-weight: 800;
      line-height: 1.2;
      letter-spacing: -0.03em;
      color: #0D1B3E;
      animation: fadeSlideIn 0.6s 0.1s ease both;
    }
    .title-accent {
      background: linear-gradient(135deg, #1E6FD9 0%, #5BA4F5 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .hero-desc {
      font-size: 1rem;
      line-height: 1.7;
      color: #4A5568;
      max-width: 480px;
      animation: fadeSlideIn 0.6s 0.2s ease both;
    }

    .hero-actions {
      display: flex;
      gap: 0.75rem;
      align-items: center;
      flex-wrap: wrap;
      animation: fadeSlideIn 0.6s 0.3s ease both;
    }
    .btn-primary {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.8rem 1.7rem;
      background: linear-gradient(135deg, #1E6FD9, #2B8AFF);
      color: white;
      font-size: 0.95rem;
      font-weight: 600;
      border-radius: 10px;
      text-decoration: none;
      box-shadow: 0 4px 20px rgba(30,111,217,0.35);
      transition: all 0.25s;
    }
    .btn-primary:hover { transform: translateY(-2px); box-shadow: 0 8px 28px rgba(30,111,217,0.45); }
    .btn-ghost {
      display: inline-flex;
      align-items: center;
      padding: 0.8rem 1.4rem;
      color: #4A5568;
      font-size: 0.95rem;
      font-weight: 500;
      border-radius: 10px;
      text-decoration: none;
      border: 1.5px solid rgba(30,111,217,0.2);
      transition: all 0.2s;
    }
    .btn-ghost:hover { border-color: #1E6FD9; color: #1E6FD9; background: rgba(30,111,217,0.04); }

    .hero-trust {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
      animation: fadeSlideIn 0.6s 0.4s ease both;
    }
    .trust-item {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      font-size: 0.8rem;
      color: #4A5568;
      font-weight: 500;
    }
    .trust-sep { color: #CBD5E0; font-size: 1.2rem; }

    @keyframes fadeSlideIn {
      from { opacity: 0; transform: translateY(16px); }
      to { opacity: 1; transform: translateY(0); }
    }

    /* ── DASHBOARD CARD ── */
    .hero-visual {
      animation: fadeSlideIn 0.7s 0.2s ease both;
    }
    .dashboard-card {
      background: white;
      border-radius: 16px;
      box-shadow: 0 20px 60px rgba(30,111,217,0.15), 0 4px 16px rgba(0,0,0,0.06);
      border: 1px solid rgba(30,111,217,0.10);
      overflow: hidden;
    }
    .dc-header {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.85rem 1.25rem;
      background: #F8FAFF;
      border-bottom: 1px solid rgba(30,111,217,0.08);
    }
    .dc-dots { display: flex; gap: 5px; }
    .dc-dots span {
      width: 10px; height: 10px;
      border-radius: 50%;
      background: #E2E8F0;
    }
    .dc-dots span:first-child { background: #FCA5A5; }
    .dc-dots span:nth-child(2) { background: #FCD34D; }
    .dc-dots span:last-child { background: #86EFAC; }
    .dc-title { font-size: 0.78rem; color: #718096; font-weight: 500; }

    .dc-body { padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; }

    .kpi-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; }
    .kpi-card {
      padding: 0.9rem;
      border-radius: 10px;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .kpi-green { background: #F0FFF4; }
    .kpi-red { background: #FFF5F5; }
    .kpi-blue { background: #EBF8FF; }
    .kpi-label { font-size: 0.7rem; color: #718096; font-weight: 500; }
    .kpi-value { font-size: 1.35rem; font-weight: 800; color: #0D1B3E; }
    .kpi-trend { font-size: 0.7rem; font-weight: 600; }
    .kpi-trend.up { color: #38A169; }
    .kpi-trend.down { color: #E53E3E; }

    .mini-chart { background: #F8FAFF; border-radius: 10px; padding: 0.9rem; }
    .chart-label { font-size: 0.72rem; color: #718096; font-weight: 500; margin-bottom: 0.6rem; }
    .bars {
      display: flex;
      align-items: flex-end;
      gap: 6px;
      height: 60px;
    }
    .bar-group { display: flex; align-items: flex-end; gap: 2px; }
    .bar { width: 10px; border-radius: 3px 3px 0 0; transition: height 1s ease; }
    .bar-prev { background: rgba(30,111,217,0.25); }
    .bar-curr { background: #1E6FD9; }
    .chart-legend { display: flex; gap: 1rem; margin-top: 0.4rem; }
    .leg { font-size: 0.68rem; font-weight: 500; display: flex; align-items: center; gap: 4px; }
    .leg::before { content: ''; display: inline-block; width: 10px; height: 4px; border-radius: 2px; }
    .leg-prev::before { background: rgba(30,111,217,0.25); }
    .leg-curr::before { background: #1E6FD9; }

    .ai-insight {
      display: flex;
      align-items: flex-start;
      gap: 0.6rem;
      background: rgba(30,111,217,0.05);
      border: 1px solid rgba(30,111,217,0.12);
      border-radius: 8px;
      padding: 0.7rem 0.85rem;
    }
    .ai-icon {
      flex-shrink: 0;
      width: 26px; height: 26px;
      background: rgba(30,111,217,0.1);
      border-radius: 6px;
      display: flex; align-items: center; justify-content: center;
    }
    .ai-text { font-size: 0.75rem; color: #4A5568; line-height: 1.5; }

    /* ── FEATURES ── */
    .features-section {
      position: relative;
      z-index: 1;
      max-width: 1200px;
      margin: 0 auto;
      padding: 5rem 2rem;
      text-align: center;
    }
    .section-label {
      display: inline-block;
      font-size: 0.78rem;
      font-weight: 700;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      color: #1E6FD9;
      background: rgba(30,111,217,0.08);
      padding: 0.3rem 0.9rem;
      border-radius: 100px;
      margin-bottom: 1rem;
    }
    .section-title {
      font-size: clamp(1.6rem, 2.5vw, 2.2rem);
      font-weight: 800;
      letter-spacing: -0.03em;
      color: #0D1B3E;
      margin-bottom: 3rem;
      line-height: 1.3;
    }
    .features-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1.25rem;
    }
    .feat-card {
      background: white;
      border-radius: 14px;
      padding: 1.75rem;
      text-align: left;
      border: 1px solid rgba(30,111,217,0.08);
      box-shadow: 0 2px 12px rgba(30,111,217,0.06);
      transition: all 0.3s;
      animation: fadeSlideIn 0.6s ease both;
    }
    .feat-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 32px rgba(30,111,217,0.14);
      border-color: rgba(30,111,217,0.2);
    }
    .feat-icon {
      width: 44px; height: 44px;
      background: rgba(30,111,217,0.08);
      border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      margin-bottom: 1rem;
    }
    .feat-title { font-size: 1rem; font-weight: 700; color: #0D1B3E; margin-bottom: 0.5rem; }
    .feat-desc { font-size: 0.875rem; color: #718096; line-height: 1.65; }

    /* ── STATS ── */
    .stats-section {
      position: relative;
      z-index: 1;
      background: linear-gradient(135deg, #0D1B3E 0%, #1E3A5F 100%);
      padding: 3.5rem 2rem;
    }
    .stats-inner {
      max-width: 1200px;
      margin: 0 auto;
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 2rem;
    }
    .stat-item { text-align: center; }
    .stat-value {
      font-size: 2.5rem;
      font-weight: 900;
      color: white;
      letter-spacing: -0.04em;
      background: linear-gradient(135deg, #fff 40%, #7CC3FF);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    .stat-label { font-size: 0.85rem; color: rgba(255,255,255,0.6); margin-top: 0.25rem; font-weight: 500; }

    /* ── CTA ── */
    .cta-section {
      position: relative;
      z-index: 1;
      padding: 5rem 2rem;
      max-width: 1200px;
      margin: 0 auto;
    }
    .cta-box {
      background: linear-gradient(135deg, #1E6FD9 0%, #2B8AFF 60%, #5BA4F5 100%);
      border-radius: 20px;
      padding: 4rem 3rem;
      text-align: center;
      position: relative;
      overflow: hidden;
      box-shadow: 0 20px 60px rgba(30,111,217,0.30);
    }
    .cta-box::before {
      content: '';
      position: absolute;
      inset: 0;
      background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
    }
    .cta-icon {
      width: 64px; height: 64px;
      background: rgba(255,255,255,0.15);
      border-radius: 16px;
      display: flex; align-items: center; justify-content: center;
      margin: 0 auto 1.5rem;
      position: relative;
    }
    .cta-title {
      font-size: clamp(1.5rem, 2.5vw, 2rem);
      font-weight: 800;
      color: white;
      letter-spacing: -0.03em;
      margin-bottom: 0.75rem;
      position: relative;
    }
    .cta-desc {
      font-size: 1rem;
      color: rgba(255,255,255,0.8);
      margin-bottom: 2rem;
      position: relative;
      max-width: 480px;
      margin-left: auto;
      margin-right: auto;
    }
    .cta-actions {
      display: flex;
      gap: 0.75rem;
      justify-content: center;
      flex-wrap: wrap;
      position: relative;
    }
    .btn-cta-white {
      padding: 0.85rem 2rem;
      background: white;
      color: #1E6FD9;
      font-weight: 700;
      font-size: 0.95rem;
      border-radius: 10px;
      text-decoration: none;
      transition: all 0.2s;
      box-shadow: 0 4px 16px rgba(0,0,0,0.1);
    }
    .btn-cta-white:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.15); }
    .btn-cta-outline {
      padding: 0.85rem 2rem;
      border: 2px solid rgba(255,255,255,0.5);
      color: white;
      font-weight: 600;
      font-size: 0.95rem;
      border-radius: 10px;
      text-decoration: none;
      transition: all 0.2s;
    }
    .btn-cta-outline:hover { background: rgba(255,255,255,0.1); border-color: white; }

    /* ── FOOTER ── */
    .footer {
      position: relative;
      z-index: 1;
      background: #0D1B3E;
      padding: 1.75rem 2rem;
    }
    .footer-inner {
      max-width: 1200px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .footer-copy { font-size: 0.82rem; color: rgba(255,255,255,0.4); }

    /* ── RESPONSIVE ── */
    @media (max-width: 900px) {
      .hero { grid-template-columns: 1fr; }
      .hero-visual { display: none; }
      .features-grid { grid-template-columns: 1fr 1fr; }
      .stats-inner { grid-template-columns: repeat(2, 1fr); }
      .nav-links { display: none; }
    }
    @media (max-width: 600px) {
      .features-grid { grid-template-columns: 1fr; }
      .stats-inner { grid-template-columns: 1fr 1fr; }
      .cta-box { padding: 2.5rem 1.5rem; }
    }
  `]
})
export class HomePage implements OnInit, OnDestroy {

  bars = [
    { prev: 38, curr: 28 },
    { prev: 45, curr: 32 },
    { prev: 30, curr: 22 },
    { prev: 52, curr: 40 },
    { prev: 41, curr: 30 },
    { prev: 35, curr: 18 },
  ];

  features = [
    {
      title: 'Import Excel intelligent',
      desc: 'Importez vos fichiers N et N-1 via un template standard ou un fichier personnalisé avec système de mapping automatique.',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>`
    },
    {
      title: 'Analyse comparative KPI',
      desc: 'Calcul automatique des variations absolues et relatives, classifiées en niveaux faible, modéré ou critique.',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>`
    },
    {
      title: 'Synthèse IA par LLM',
      desc: 'L\'IA génère des commentaires contextualisés, identifie les causes probables et propose des plans d\'action concrets.',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M4.22 4.22l2.12 2.12M17.66 17.66l2.12 2.12M2 12h3M19 12h3M4.22 19.78l2.12-2.12M17.66 6.34l2.12-2.12"/></svg>`
    },
    {
      title: 'Tableaux de bord interactifs',
      desc: 'Visualisez vos performances QHSE avec des graphiques dynamiques et des tableaux comparatifs clairs et lisibles.',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>`
    },
    {
      title: 'Export PDF & Rapports',
      desc: 'Générez des rapports complets au format PDF, partageables avec votre équipe et vos parties prenantes.',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>`
    },
    {
      title: 'Sécurité & Authentification',
      desc: 'Authentification JWT à deux facteurs avec contrôle d\'accès par rôle (Administrateur / Analyste).',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`
    },
  ];

  stats = [
    { value: '4', label: 'Catégories QHSE couvertes' },
    { value: '16+', label: 'Indicateurs de performance' },
    { value: '3', label: 'Niveaux de criticité' },
    { value: '100%', label: 'Automatisé par IA' },
  ];

  ngOnInit() {}
  ngOnDestroy() {}
}