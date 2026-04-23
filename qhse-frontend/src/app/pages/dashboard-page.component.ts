import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { KpiService } from '../core/services/kpi.service';
import { ImportService } from '../core/services/import.service';
import { AdminDashboardService } from '../core/services/admin-dashboard.service';
import { AuthStorageService } from '../core/services/auth-storage.service';
import { KpiResponse } from '../shared/models/kpi-response';
import { ImportSessionResponse } from '../shared/models/import-session-response';
import { ImportStatus } from '../shared/enums/import-status.enum';
import {
  AdminStatsResponse,
  AdminGraphiquesDataResponse,
  AdminRepartitionResponse,
  AdminAnalysteItemResponse,
  AdminKpiCritiqueResponse
} from '../shared/models/admin-dashboard';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatToolbarModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    BaseChartDirective
  ],
  template: `
    <div class="dashboard-wrapper">

      <!-- Animated background — identical to homepage -->
      <div class="bg-grid"></div>
      <div class="bg-orb orb-1"></div>
      <div class="bg-orb orb-2"></div>
      <div class="bg-orb orb-3"></div>

      <!-- ── TOPBAR ── -->
      <header class="topbar">
        <div class="topbar-inner">
          <!-- Brand -->
          <div class="brand">
            <div class="brand-icon">
              <svg width="22" height="22" viewBox="0 0 28 28" fill="none">
                <path d="M14 2L24 8V20L14 26L4 20V8L14 2Z" stroke="#1E6FD9" stroke-width="2" fill="none"/>
                <path d="M14 7L20 10.5V17.5L14 21L8 17.5V10.5L14 7Z" fill="#1E6FD9" opacity="0.3"/>
                <circle cx="14" cy="14" r="3" fill="#1E6FD9"/>
              </svg>
            </div>
            <span class="brand-name">QHSE <strong>Analytics</strong></span>
          </div>

          <!-- Page title -->
          <div class="topbar-title">
            <mat-icon class="title-icon">analytics</mat-icon>
            <div>
              <h1 class="page-title">Tableau de bord</h1>
              <p class="page-subtitle">Suivez vos indicateurs en temps réel</p>
            </div>
          </div>

          <!-- Actions -->
          <div class="topbar-actions">
            <button class="btn-import" (click)="goToImport()">
              <mat-icon>cloud_upload</mat-icon>
              Nouvel import
            </button>
          </div>
        </div>
      </header>

      <!-- ── MAIN ── -->
      <main class="dashboard-main">
        <ng-container *ngIf="isAdmin !== null; else loadingDashboard">

          <!-- ── USER DASHBOARD ── -->
          <ng-container *ngIf="!isAdmin; else adminDashboard">

            <!-- KPI Summary Cards -->
            <div class="summary-grid">

              <div class="stat-card" style="animation-delay:0s">
                <div class="stat-card-glow"></div>
                <div class="stat-top">
                  <div class="stat-icon-wrap">
                    <mat-icon>assessment</mat-icon>
                  </div>
                  <span class="stat-badge">KPI</span>
                </div>
                <div class="stat-value">{{ totalKpis }}</div>
                <p class="stat-label">KPI enregistrés</p>
              </div>

              <div class="stat-card" style="animation-delay:.08s">
                <div class="stat-card-glow"></div>
                <div class="stat-top">
                  <div class="stat-icon-wrap icon-green">
                    <mat-icon>check_circle</mat-icon>
                  </div>
                  <span class="stat-badge badge-green">Actifs</span>
                </div>
                <div class="stat-value">{{ activeKpis }}</div>
                <p class="stat-label">Statut actif</p>
              </div>

              <div class="stat-card" style="animation-delay:.16s">
                <div class="stat-card-glow"></div>
                <div class="stat-top">
                  <div class="stat-icon-wrap icon-purple">
                    <mat-icon>cloud_upload</mat-icon>
                  </div>
                  <span class="stat-badge badge-purple">Imports</span>
                </div>
                <div class="stat-value">{{ totalImports }}</div>
                <p class="stat-label">Sessions totales</p>
              </div>

              <div class="stat-card" style="animation-delay:.24s">
                <div class="stat-card-glow"></div>
                <div class="stat-top">
                  <div class="stat-icon-wrap icon-amber">
                    <mat-icon>history</mat-icon>
                  </div>
                  <span class="stat-badge badge-amber">Récent</span>
                </div>
                <div class="stat-value small-val" *ngIf="latestImport">{{ latestImport.nomFichier | slice:0:14 }}…</div>
                <div class="stat-value" *ngIf="!latestImport">—</div>
                <p class="stat-label" *ngIf="latestImport">{{ latestImport.periodeN1 }} → {{ latestImport.periodeN }}</p>
                <p class="stat-label" *ngIf="!latestImport">Aucun import</p>
              </div>

            </div>

            <!-- Distribution & Table row -->
            <div class="content-row">

              <!-- Status Distribution -->
              <div class="glass-card distribution-card" style="animation-delay:.1s">
                <div class="card-header-row">
                  <div class="card-header-left">
                    <mat-icon class="card-header-icon">donut_large</mat-icon>
                    <h3>Distribution des imports</h3>
                  </div>
                </div>
                <div class="status-grid">
                  <div class="status-pill pill-pending">
                    <span class="pill-dot"></span>
                    <span class="pill-label">En attente</span>
                    <span class="pill-count">{{ countImports(ImportStatus.EN_ATTENTE) }}</span>
                  </div>
                  <div class="status-pill pill-processing">
                    <span class="pill-dot"></span>
                    <span class="pill-label">En traitement</span>
                    <span class="pill-count">{{ countImports(ImportStatus.EN_TRAITEMENT) }}</span>
                  </div>
                  <div class="status-pill pill-success">
                    <span class="pill-dot"></span>
                    <span class="pill-label">Traité</span>
                    <span class="pill-count">{{ countImports(ImportStatus.TRAITE) }}</span>
                  </div>
                  <div class="status-pill pill-error">
                    <span class="pill-dot"></span>
                    <span class="pill-label">Erreur</span>
                    <span class="pill-count">{{ countImports(ImportStatus.ERREUR) }}</span>
                  </div>
                </div>
              </div>

              <!-- Quick Actions -->
              <div class="glass-card actions-card" style="animation-delay:.18s">
                <div class="card-header-row">
                  <div class="card-header-left">
                    <mat-icon class="card-header-icon">bolt</mat-icon>
                    <h3>Actions rapides</h3>
                  </div>
                </div>
                <div class="actions-list">
                  <button class="action-item" (click)="goToKpi()">
                    <div class="action-icon-wrap">
                      <mat-icon>tune</mat-icon>
                    </div>
                    <span>Gérer les KPI</span>
                    <mat-icon class="action-arrow">chevron_right</mat-icon>
                  </button>
                  <button class="action-item" (click)="goToHistorique()">
                    <div class="action-icon-wrap">
                      <mat-icon>history</mat-icon>
                    </div>
                    <span>Historique</span>
                    <mat-icon class="action-arrow">chevron_right</mat-icon>
                  </button>
                  <button class="action-item" (click)="goToMapping()">
                    <div class="action-icon-wrap">
                      <mat-icon>link</mat-icon>
                    </div>
                    <span>Mappings</span>
                    <mat-icon class="action-arrow">chevron_right</mat-icon>
                  </button>
                </div>
              </div>

            </div>

            <!-- Recent Sessions Table -->
            <div class="glass-card table-card" style="animation-delay:.26s">
              <div class="card-header-row">
                <div class="card-header-left">
                  <mat-icon class="card-header-icon">table_chart</mat-icon>
                  <h3>Dernières sessions d'import</h3>
                </div>
                <button class="btn-icon-ghost" (click)="goToHistorique()">
                  <mat-icon>arrow_forward</mat-icon>
                  <span>Tout voir</span>
                </button>
              </div>

              <div class="table-wrapper" *ngIf="!isLoading">
                <table mat-table [dataSource]="recentImports" class="qhse-table" *ngIf="recentImports.length > 0">
                  <ng-container matColumnDef="fichier">
                    <th mat-header-cell *matHeaderCellDef>Fichier</th>
                    <td mat-cell *matCellDef="let s">{{ s.nomFichier }}</td>
                  </ng-container>
                  <ng-container matColumnDef="periode">
                    <th mat-header-cell *matHeaderCellDef>Période</th>
                    <td mat-cell *matCellDef="let s">{{ s.periodeN1 }} → {{ s.periodeN }}</td>
                  </ng-container>
                  <ng-container matColumnDef="statut">
                    <th mat-header-cell *matHeaderCellDef>Statut</th>
                    <td mat-cell *matCellDef="let s">
                      <span class="status-tag" [ngClass]="'tag-' + s.statut.toLowerCase()">{{ s.statut }}</span>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="total">
                    <th mat-header-cell *matHeaderCellDef>Total</th>
                    <td mat-cell *matCellDef="let s">{{ s.nombreTotal }}</td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="recentColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: recentColumns;"></tr>
                </table>
                <div class="empty-state" *ngIf="recentImports.length === 0">
                  <mat-icon>inbox</mat-icon>
                  <p>Aucune session d'import récente</p>
                </div>
              </div>
              <div class="spinner-wrap" *ngIf="isLoading">
                <mat-progress-spinner mode="indeterminate" diameter="36" class="blue-spinner"></mat-progress-spinner>
              </div>
            </div>

          </ng-container>

          <!-- ── ADMIN DASHBOARD ── -->
          <ng-template #adminDashboard>

            <mat-tab-group class="qhse-tabs">

              <!-- TAB: Vue d'ensemble -->
              <mat-tab label="Vue d'ensemble">
                <div class="tab-content">

                  <div class="summary-grid">
                    <div class="stat-card" style="animation-delay:0s">
                      <div class="stat-card-glow"></div>
                      <div class="stat-top">
                        <div class="stat-icon-wrap"><mat-icon>assessment</mat-icon></div>
                        <span class="stat-badge">KPI</span>
                      </div>
                      <div class="stat-value">{{ totalKpis }}</div>
                      <p class="stat-label">KPI enregistrés</p>
                    </div>
                    <div class="stat-card" style="animation-delay:.08s">
                      <div class="stat-card-glow"></div>
                      <div class="stat-top">
                        <div class="stat-icon-wrap icon-green"><mat-icon>check_circle</mat-icon></div>
                        <span class="stat-badge badge-green">Actifs</span>
                      </div>
                      <div class="stat-value">{{ activeKpis }}</div>
                      <p class="stat-label">Statut actif</p>
                    </div>
                    <div class="stat-card" style="animation-delay:.16s">
                      <div class="stat-card-glow"></div>
                      <div class="stat-top">
                        <div class="stat-icon-wrap icon-purple"><mat-icon>cloud_upload</mat-icon></div>
                        <span class="stat-badge badge-purple">Imports</span>
                      </div>
                      <div class="stat-value">{{ totalImports }}</div>
                      <p class="stat-label">Sessions totales</p>
                    </div>
                    <div class="stat-card" style="animation-delay:.24s">
                      <div class="stat-card-glow"></div>
                      <div class="stat-top">
                        <div class="stat-icon-wrap icon-amber"><mat-icon>history</mat-icon></div>
                        <span class="stat-badge badge-amber">Récent</span>
                      </div>
                      <div class="stat-value small-val" *ngIf="latestImport">{{ latestImport.nomFichier | slice:0:14 }}…</div>
                      <div class="stat-value" *ngIf="!latestImport">—</div>
                      <p class="stat-label" *ngIf="latestImport">{{ latestImport.periodeN1 }} → {{ latestImport.periodeN }}</p>
                    </div>
                  </div>

                  <div class="content-row">
                    <div class="glass-card distribution-card" style="animation-delay:.1s">
                      <div class="card-header-row">
                        <div class="card-header-left">
                          <mat-icon class="card-header-icon">donut_large</mat-icon>
                          <h3>Distribution des imports</h3>
                        </div>
                      </div>
                      <div class="status-grid">
                        <div class="status-pill pill-pending">
                          <span class="pill-dot"></span>
                          <span class="pill-label">En attente</span>
                          <span class="pill-count">{{ countImports(ImportStatus.EN_ATTENTE) }}</span>
                        </div>
                        <div class="status-pill pill-processing">
                          <span class="pill-dot"></span>
                          <span class="pill-label">En traitement</span>
                          <span class="pill-count">{{ countImports(ImportStatus.EN_TRAITEMENT) }}</span>
                        </div>
                        <div class="status-pill pill-success">
                          <span class="pill-dot"></span>
                          <span class="pill-label">Traité</span>
                          <span class="pill-count">{{ countImports(ImportStatus.TRAITE) }}</span>
                        </div>
                        <div class="status-pill pill-error">
                          <span class="pill-dot"></span>
                          <span class="pill-label">Erreur</span>
                          <span class="pill-count">{{ countImports(ImportStatus.ERREUR) }}</span>
                        </div>
                      </div>
                    </div>

                    <div class="glass-card actions-card" style="animation-delay:.18s">
                      <div class="card-header-row">
                        <div class="card-header-left">
                          <mat-icon class="card-header-icon">bolt</mat-icon>
                          <h3>Actions rapides</h3>
                        </div>
                      </div>
                      <div class="actions-list">
                        <button class="action-item" (click)="goToKpi()">
                          <div class="action-icon-wrap"><mat-icon>tune</mat-icon></div>
                          <span>Gérer les KPI</span>
                          <mat-icon class="action-arrow">chevron_right</mat-icon>
                        </button>
                        <button class="action-item" (click)="goToHistorique()">
                          <div class="action-icon-wrap"><mat-icon>history</mat-icon></div>
                          <span>Historique</span>
                          <mat-icon class="action-arrow">chevron_right</mat-icon>
                        </button>
                        <button class="action-item" (click)="goToMapping()">
                          <div class="action-icon-wrap"><mat-icon>link</mat-icon></div>
                          <span>Mappings</span>
                          <mat-icon class="action-arrow">chevron_right</mat-icon>
                        </button>
                        <button class="action-item" (click)="goToUsers()">
                          <div class="action-icon-wrap"><mat-icon>people</mat-icon></div>
                          <span>Utilisateurs</span>
                          <mat-icon class="action-arrow">chevron_right</mat-icon>
                        </button>
                      </div>
                    </div>
                  </div>

                  <div class="glass-card table-card" style="animation-delay:.26s">
                    <div class="card-header-row">
                      <div class="card-header-left">
                        <mat-icon class="card-header-icon">table_chart</mat-icon>
                        <h3>Dernières sessions d'import</h3>
                      </div>
                      <button class="btn-icon-ghost" (click)="goToHistorique()">
                        <mat-icon>arrow_forward</mat-icon>
                        <span>Tout voir</span>
                      </button>
                    </div>
                    <div class="table-wrapper" *ngIf="!isLoading">
                      <table mat-table [dataSource]="recentImports" class="qhse-table" *ngIf="recentImports.length > 0">
                        <ng-container matColumnDef="fichier">
                          <th mat-header-cell *matHeaderCellDef>Fichier</th>
                          <td mat-cell *matCellDef="let s">{{ s.nomFichier }}</td>
                        </ng-container>
                        <ng-container matColumnDef="periode">
                          <th mat-header-cell *matHeaderCellDef>Période</th>
                          <td mat-cell *matCellDef="let s">{{ s.periodeN1 }} → {{ s.periodeN }}</td>
                        </ng-container>
                        <ng-container matColumnDef="statut">
                          <th mat-header-cell *matHeaderCellDef>Statut</th>
                          <td mat-cell *matCellDef="let s">
                            <span class="status-tag" [ngClass]="'tag-' + s.statut.toLowerCase()">{{ s.statut }}</span>
                          </td>
                        </ng-container>
                        <ng-container matColumnDef="total">
                          <th mat-header-cell *matHeaderCellDef>Total</th>
                          <td mat-cell *matCellDef="let s">{{ s.nombreTotal }}</td>
                        </ng-container>
                        <tr mat-header-row *matHeaderRowDef="recentColumns"></tr>
                        <tr mat-row *matRowDef="let row; columns: recentColumns;"></tr>
                      </table>
                      <div class="empty-state" *ngIf="recentImports.length === 0">
                        <mat-icon>inbox</mat-icon>
                        <p>Aucune session d'import récente</p>
                      </div>
                    </div>
                    <div class="spinner-wrap" *ngIf="isLoading">
                      <mat-progress-spinner mode="indeterminate" diameter="36" class="blue-spinner"></mat-progress-spinner>
                    </div>
                  </div>
                </div>
              </mat-tab>

              <!-- TAB: Statistiques -->
              <mat-tab label="Statistiques">
                <div class="tab-content">
                  <div class="charts-grid" *ngIf="graphiquesData">
                    <div class="glass-card chart-card">
                      <div class="card-header-row">
                        <div class="card-header-left">
                          <mat-icon class="card-header-icon">show_chart</mat-icon>
                          <h3>Évolution des imports</h3>
                        </div>
                      </div>
                      <canvas baseChart
                              [data]="graphiquesData.importsEvolution"
                              [options]="lineChartOptions"
                              [type]="lineChartType">
                      </canvas>
                    </div>

                    <div class="glass-card chart-card">
                      <div class="card-header-row">
                        <div class="card-header-left">
                          <mat-icon class="card-header-icon">pie_chart</mat-icon>
                          <h3>Répartition par catégorie</h3>
                        </div>
                      </div>
                      <canvas baseChart
                              [data]="graphiquesData.repartitionCategories"
                              [options]="pieChartOptions"
                              [type]="pieChartType">
                      </canvas>
                    </div>

                    <div class="glass-card chart-card">
                      <div class="card-header-row">
                        <div class="card-header-left">
                          <mat-icon class="card-header-icon">bar_chart</mat-icon>
                          <h3>KPI par analyste</h3>
                        </div>
                      </div>
                      <canvas baseChart
                              [data]="graphiquesData.kpiParAnalyste"
                              [options]="barChartOptions"
                              [type]="barChartType">
                      </canvas>
                    </div>

                    <div class="glass-card chart-card">
                      <div class="card-header-row">
                        <div class="card-header-left">
                          <mat-icon class="card-header-icon">trending_up</mat-icon>
                          <h3>Performance mensuelle</h3>
                        </div>
                      </div>
                      <canvas baseChart
                              [data]="graphiquesData.performanceMensuelle"
                              [options]="lineChartOptions"
                              [type]="lineChartType">
                      </canvas>
                    </div>
                  </div>

                  <div class="glass-card" *ngIf="kpisCritiques.length > 0" style="animation-delay:.3s">
                    <div class="card-header-row">
                      <div class="card-header-left">
                        <mat-icon class="card-header-icon" style="color:#EF4444">warning</mat-icon>
                        <h3>KPI critiques</h3>
                      </div>
                    </div>
                    <div class="kpi-critique-list">
                      <div *ngFor="let kpi of kpisCritiques" class="kpi-critique-item">
                        <div class="kpi-name">{{ kpi.nom }}</div>
                        <div class="kpi-meta">
                          <span class="kpi-value-text">{{ kpi.valeur }} {{ kpi.unite }}</span>
                          <span class="criticite-badge">{{ kpi.niveauCriticite }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </mat-tab>

              <!-- TAB: Analystes -->
              <mat-tab label="Analystes">
                <div class="tab-content">
                  <div class="glass-card" style="animation-delay:0s">
                    <div class="card-header-row">
                      <div class="card-header-left">
                        <mat-icon class="card-header-icon">people</mat-icon>
                        <h3>Gestion des analystes</h3>
                      </div>
                    </div>
                    <div class="table-wrapper">
                      <table mat-table [dataSource]="analystes" class="qhse-table">
                        <ng-container matColumnDef="nom">
                          <th mat-header-cell *matHeaderCellDef>Nom</th>
                          <td mat-cell *matCellDef="let a">{{ a.nom }}</td>
                        </ng-container>
                        <ng-container matColumnDef="prenom">
                          <th mat-header-cell *matHeaderCellDef>Prénom</th>
                          <td mat-cell *matCellDef="let a">{{ a.prenom }}</td>
                        </ng-container>
                        <ng-container matColumnDef="email">
                          <th mat-header-cell *matHeaderCellDef>Email</th>
                          <td mat-cell *matCellDef="let a">{{ a.email }}</td>
                        </ng-container>
                        <ng-container matColumnDef="kpisCount">
                          <th mat-header-cell *matHeaderCellDef>KPI gérés</th>
                          <td mat-cell *matCellDef="let a">{{ a.kpisCount }}</td>
                        </ng-container>
                        <ng-container matColumnDef="lastActivity">
                          <th mat-header-cell *matHeaderCellDef>Dernière activité</th>
                          <td mat-cell *matCellDef="let a">{{ a.lastActivity | date:'short' }}</td>
                        </ng-container>
                        <tr mat-header-row *matHeaderRowDef="analysteColumns"></tr>
                        <tr mat-row *matRowDef="let row; columns: analysteColumns;"></tr>
                      </table>
                    </div>
                  </div>
                </div>
              </mat-tab>

            </mat-tab-group>
          </ng-template>

        </ng-container>

        <!-- Loading state -->
        <ng-template #loadingDashboard>
          <div class="full-loading">
            <mat-progress-spinner mode="indeterminate" diameter="52" class="blue-spinner"></mat-progress-spinner>
            <p>Chargement du tableau de bord…</p>
          </div>
        </ng-template>

      </main>
    </div>
  `,
  styles: [`
    /* ────────────────────────────────────────
       RESET & TOKENS — match homepage palette
    ──────────────────────────────────────── */
    :host { display: block; }
    * { box-sizing: border-box; }

    /* CSS Variables */
    :host {
      --blue:        #1E6FD9;
      --blue-light:  #2B8AFF;
      --blue-pale:   rgba(30,111,217,0.08);
      --navy:        #0D1B3E;
      --navy-mid:    #1E3A5F;
      --text-muted:  #4A5568;
      --text-dim:    #718096;
      --border:      rgba(30,111,217,0.12);
      --radius-lg:   16px;
      --radius-md:   12px;
      --radius-sm:   8px;
      --shadow-card: 0 4px 24px rgba(30,111,217,0.10), 0 1px 6px rgba(0,0,0,0.05);
      --shadow-hover: 0 12px 40px rgba(30,111,217,0.18);
    }

    /* ── WRAPPER ── */
    .dashboard-wrapper {
      min-height: 100vh;
      background: #F4F7FF;
      font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
      color: var(--navy);
      position: relative;
      overflow-x: hidden;
    }

    /* ── ANIMATED BACKGROUND (exact copy from homepage) ── */
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
    .orb-1 { width:500px; height:500px; background:rgba(30,111,217,0.10); top:-150px; right:-100px; animation-delay:0s; }
    .orb-2 { width:350px; height:350px; background:rgba(100,180,255,0.08); bottom:10%; left:-80px; animation-delay:-4s; }
    .orb-3 { width:280px; height:280px; background:rgba(30,111,217,0.06); top:45%; left:40%; animation-delay:-8s; }
    @keyframes floatOrb {
      0%,100% { transform:translate(0,0) scale(1); }
      50%      { transform:translate(20px,-30px) scale(1.05); }
    }

    /* ── TOPBAR ── */
    .topbar {
      position: sticky;
      top: 0;
      z-index: 100;
      background: rgba(255,255,255,0.88);
      backdrop-filter: blur(18px);
      border-bottom: 1px solid var(--border);
    }
    .topbar-inner {
      max-width: 1280px;
      margin: 0 auto;
      padding: 0 2rem;
      height: 66px;
      display: flex;
      align-items: center;
      gap: 1.5rem;
    }

    /* Brand */
    .brand {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      flex-shrink: 0;
    }
    .brand-icon {
      width: 36px; height: 36px;
      background: var(--blue-pale);
      border-radius: var(--radius-sm);
      display: flex; align-items: center; justify-content: center;
    }
    .brand-name {
      font-size: 1rem;
      letter-spacing: -0.02em;
      color: var(--navy);
    }
    .brand-name strong { color: var(--blue); }

    /* Page title in topbar */
    .topbar-title {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-left: 1.5rem;
      padding-left: 1.5rem;
      border-left: 1px solid var(--border);
    }
    .title-icon { color: var(--blue); font-size: 1.4rem; width:1.4rem; height:1.4rem; }
    .page-title {
      font-size: 1rem;
      font-weight: 700;
      color: var(--navy);
      letter-spacing: -0.02em;
      line-height: 1.2;
    }
    .page-subtitle {
      font-size: 0.75rem;
      color: var(--text-dim);
      line-height: 1;
    }

    .topbar-actions { margin-left: auto; }

    /* Import button */
    .btn-import {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.55rem 1.25rem;
      background: linear-gradient(135deg, var(--blue), var(--blue-light));
      color: white;
      font-size: 0.875rem;
      font-weight: 600;
      border: none;
      border-radius: var(--radius-sm);
      cursor: pointer;
      box-shadow: 0 4px 16px rgba(30,111,217,0.30);
      transition: all 0.2s;
    }
    .btn-import mat-icon { font-size: 1.1rem; width:1.1rem; height:1.1rem; }
    .btn-import:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(30,111,217,0.40); }

    /* ── MAIN ── */
    .dashboard-main {
      position: relative;
      z-index: 1;
      max-width: 1280px;
      margin: 0 auto;
      padding: 2rem 2rem 4rem;
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    /* ── STAT CARDS ── */
    .summary-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1.25rem;
    }

    .stat-card {
      position: relative;
      background: white;
      border-radius: var(--radius-lg);
      border: 1px solid var(--border);
      padding: 1.5rem 1.5rem 1.25rem;
      box-shadow: var(--shadow-card);
      overflow: hidden;
      animation: fadeSlideIn 0.55s ease both;
      transition: transform 0.25s, box-shadow 0.25s;
    }
    .stat-card:hover {
      transform: translateY(-4px);
      box-shadow: var(--shadow-hover);
    }
    .stat-card-glow {
      position: absolute;
      top: -30px; right: -30px;
      width: 120px; height: 120px;
      background: radial-gradient(circle, rgba(30,111,217,0.12) 0%, transparent 70%);
      border-radius: 50%;
      pointer-events: none;
    }

    .stat-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 1rem;
    }
    .stat-icon-wrap {
      width: 38px; height: 38px;
      background: var(--blue-pale);
      border-radius: var(--radius-sm);
      display: flex; align-items: center; justify-content: center;
      color: var(--blue);
    }
    .stat-icon-wrap mat-icon { font-size: 1.2rem; width:1.2rem; height:1.2rem; }
    .icon-green  { background: rgba(34,197,94,0.10);  color: #16A34A; }
    .icon-purple { background: rgba(139,92,246,0.10); color: #7C3AED; }
    .icon-amber  { background: rgba(245,158,11,0.10); color: #D97706; }

    .stat-badge {
      font-size: 0.68rem;
      font-weight: 700;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      padding: 0.22rem 0.6rem;
      border-radius: 100px;
      background: var(--blue-pale);
      color: var(--blue);
    }
    .badge-green  { background: rgba(34,197,94,0.10);  color: #16A34A; }
    .badge-purple { background: rgba(139,92,246,0.10); color: #7C3AED; }
    .badge-amber  { background: rgba(245,158,11,0.10); color: #D97706; }

    .stat-value {
      font-size: 2.2rem;
      font-weight: 900;
      letter-spacing: -0.04em;
      color: var(--navy);
      line-height: 1;
      margin-bottom: 0.35rem;
    }
    .stat-value.small-val { font-size: 1.2rem; }
    .stat-label {
      font-size: 0.8rem;
      color: var(--text-dim);
      font-weight: 500;
    }

    /* ── GLASS CARDS ── */
    .glass-card {
      background: white;
      border-radius: var(--radius-lg);
      border: 1px solid var(--border);
      padding: 1.5rem;
      box-shadow: var(--shadow-card);
      animation: fadeSlideIn 0.55s ease both;
      transition: box-shadow 0.25s;
    }
    .glass-card:hover { box-shadow: var(--shadow-hover); }

    .card-header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 1.25rem;
    }
    .card-header-left {
      display: flex;
      align-items: center;
      gap: 0.6rem;
    }
    .card-header-icon {
      font-size: 1.2rem; width:1.2rem; height:1.2rem;
      color: var(--blue);
    }
    .card-header-left h3 {
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--navy);
      letter-spacing: -0.01em;
    }

    .btn-icon-ghost {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--blue);
      background: var(--blue-pale);
      border: none;
      border-radius: var(--radius-sm);
      padding: 0.35rem 0.75rem;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn-icon-ghost mat-icon { font-size: 1rem; width:1rem; height:1rem; }
    .btn-icon-ghost:hover { background: rgba(30,111,217,0.14); }

    /* ── CONTENT ROW ── */
    .content-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.25rem;
    }

    /* ── STATUS PILLS ── */
    .status-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
    }
    .status-pill {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      padding: 0.85rem 1rem;
      border-radius: var(--radius-md);
      font-size: 0.82rem;
      font-weight: 500;
    }
    .pill-dot {
      width: 8px; height: 8px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .pill-label { flex: 1; }
    .pill-count {
      font-size: 1.1rem;
      font-weight: 800;
      letter-spacing: -0.03em;
    }

    .pill-pending    { background: #FFFBEB; color: #B45309; }
    .pill-pending    .pill-dot { background: #F59E0B; }
    .pill-processing { background: #EFF6FF; color: #1D4ED8; }
    .pill-processing .pill-dot { background: #3B82F6; }
    .pill-success    { background: #F0FFF4; color: #15803D; }
    .pill-success    .pill-dot { background: #22C55E; }
    .pill-error      { background: #FFF5F5; color: #B91C1C; }
    .pill-error      .pill-dot { background: #EF4444; }

    /* ── ACTIONS LIST ── */
    .actions-list { display: flex; flex-direction: column; gap: 0.5rem; }
    .action-item {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      width: 100%;
      padding: 0.75rem 0.85rem;
      background: transparent;
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      cursor: pointer;
      font-size: 0.875rem;
      font-weight: 500;
      color: var(--navy);
      text-align: left;
      transition: all 0.2s;
    }
    .action-item:hover {
      background: var(--blue-pale);
      border-color: rgba(30,111,217,0.25);
      color: var(--blue);
    }
    .action-item:hover .action-icon-wrap { background: rgba(30,111,217,0.15); color: var(--blue); }
    .action-icon-wrap {
      width: 32px; height: 32px;
      border-radius: var(--radius-sm);
      background: #F1F5F9;
      display: flex; align-items: center; justify-content: center;
      color: var(--text-muted);
      flex-shrink: 0;
      transition: all 0.2s;
    }
    .action-icon-wrap mat-icon { font-size: 1.05rem; width:1.05rem; height:1.05rem; }
    .action-item span { flex: 1; }
    .action-arrow { font-size: 1rem; width:1rem; height:1rem; color: var(--text-dim); }

    /* ── TABLE CARD ── */
    .table-card { padding-bottom: 0.5rem; }
    .table-wrapper { overflow-x: auto; }

    .qhse-table {
      width: 100%;
      background: transparent !important;
    }
    .qhse-table th.mat-header-cell {
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      color: var(--text-dim);
      background: #F8FAFF;
      border-bottom: 1px solid var(--border);
      padding: 0.85rem 1rem;
    }
    .qhse-table td.mat-cell {
      font-size: 0.85rem;
      color: var(--navy);
      padding: 0.85rem 1rem;
      border-bottom: 1px solid rgba(30,111,217,0.06);
    }
    .qhse-table tr.mat-row:hover td { background: #F8FAFF; }

    /* Status tags in table */
    .status-tag {
      display: inline-block;
      padding: 0.25rem 0.7rem;
      border-radius: 100px;
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.05em;
      text-transform: uppercase;
    }
    .tag-en_attente    { background: #FFFBEB; color: #B45309; }
    .tag-en_traitement { background: #EFF6FF; color: #1D4ED8; }
    .tag-traite        { background: #F0FFF4; color: #15803D; }
    .tag-erreur        { background: #FFF5F5; color: #B91C1C; }

    /* ── EMPTY STATE ── */
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
      padding: 3rem;
      color: var(--text-dim);
      font-size: 0.875rem;
    }
    .empty-state mat-icon { font-size: 2.5rem; width:2.5rem; height:2.5rem; opacity:0.4; }

    /* ── SPINNER ── */
    .spinner-wrap {
      display: flex;
      justify-content: center;
      padding: 2rem;
    }
    .blue-spinner ::ng-deep circle { stroke: var(--blue) !important; }

    .full-loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      min-height: 60vh;
      color: var(--text-dim);
      font-size: 0.875rem;
    }

    /* ── TABS ── */
    .qhse-tabs {
      --mdc-tab-indicator-active-indicator-color: var(--blue);
    }
    .qhse-tabs ::ng-deep .mat-mdc-tab-header {
      background: white;
      border-radius: var(--radius-lg) var(--radius-lg) 0 0;
      border: 1px solid var(--border);
      border-bottom: none;
      padding: 0 1rem;
    }
    .qhse-tabs ::ng-deep .mat-mdc-tab.mdc-tab--active .mdc-tab__text-label { color: var(--blue); font-weight: 700; }
    .qhse-tabs ::ng-deep .mat-mdc-tab .mdc-tab__text-label { color: var(--text-muted); font-weight: 500; }

    .tab-content {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      padding-top: 1.5rem;
    }

    /* ── CHARTS ── */
    .charts-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1.25rem;
    }
    .chart-card { min-height: 280px; }

    /* ── KPI CRITIQUE ── */
    .kpi-critique-list { display: flex; flex-direction: column; gap: 0.5rem; }
    .kpi-critique-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0.85rem 1rem;
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      transition: background 0.2s;
    }
    .kpi-critique-item:hover { background: #F8FAFF; }
    .kpi-name { font-weight: 600; font-size: 0.875rem; color: var(--navy); }
    .kpi-meta { display: flex; align-items: center; gap: 0.75rem; }
    .kpi-value-text { font-size: 0.82rem; color: var(--text-dim); }
    .criticite-badge {
      padding: 0.22rem 0.65rem;
      border-radius: 100px;
      font-size: 0.7rem;
      font-weight: 700;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      background: #FFF5F5;
      color: #B91C1C;
    }

    /* ── ANIMATION ── */
    @keyframes fadeSlideIn {
      from { opacity: 0; transform: translateY(14px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    /* ── RESPONSIVE ── */
    @media (max-width: 1100px) {
      .summary-grid { grid-template-columns: repeat(2, 1fr); }
    }
    @media (max-width: 800px) {
      .summary-grid { grid-template-columns: repeat(2, 1fr); }
      .content-row  { grid-template-columns: 1fr; }
      .charts-grid  { grid-template-columns: 1fr; }
      .topbar-title { display: none; }
    }
    @media (max-width: 500px) {
      .summary-grid { grid-template-columns: 1fr; }
      .dashboard-main { padding: 1rem 1rem 3rem; }
    }
  `]
})
export class DashboardPage implements OnInit {
  totalKpis = 0;
  activeKpis = 0;
  totalImports = 0;
  latestImport: ImportSessionResponse | null = null;
  recentImports: ImportSessionResponse[] = [];
  recentColumns = ['fichier', 'periode', 'statut', 'total'];
  analysteColumns = ['nom', 'prenom', 'email', 'kpisCount', 'lastActivity'];
  isLoading = false;
  isAdmin: boolean | null = null;
  ImportStatus = ImportStatus;

  // Admin dashboard data
  adminStats: AdminStatsResponse | null = null;
  analystes: AdminAnalysteItemResponse[] = [];
  kpisCritiques: AdminKpiCritiqueResponse[] = [];
  repartition: AdminRepartitionResponse | null = null;
  graphiquesData: AdminGraphiquesDataResponse | null = null;

  // Chart configurations
  public barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    plugins: { legend: { display: true, position: 'top' } },
  };
  public barChartType: ChartType = 'bar';

  public pieChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    plugins: { legend: { display: true, position: 'right' } },
  };
  public pieChartType: ChartType = 'pie';

  public lineChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    plugins: { legend: { display: true, position: 'top' } },
  };
  public lineChartType: ChartType = 'line';

  constructor(
    private kpiService: KpiService,
    private importService: ImportService,
    private adminDashboardService: AdminDashboardService,
    private authStorage: AuthStorageService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authStorage.user?.role === 'ADMIN';
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.isLoading = true;

    this.kpiService.getKpis().subscribe({
      next: (kpis: KpiResponse[]) => {
        this.totalKpis = kpis.length;
        this.activeKpis = kpis.filter(kpi => kpi.isActive).length;
      },
      error: (err) => console.error('Erreur chargement KPI:', err)
    });

    this.importService.getHistorique().subscribe({
      next: (history) => {
        this.totalImports = history.length;
        this.recentImports = history.slice(0, 5);
        this.latestImport = history[0] ?? null;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement historique:', err);
        this.isLoading = false;
      }
    });

    if (this.isAdmin) {
      this.loadAdminData();
    }
  }

  loadAdminData(): void {
    this.adminDashboardService.getStats().subscribe({
      next: (stats) => this.adminStats = stats,
      error: (err) => console.error('Erreur chargement stats admin:', err)
    });

    this.adminDashboardService.getAnalystes().subscribe({
      next: (analystes) => this.analystes = analystes,
      error: (err) => console.error('Erreur chargement analystes:', err)
    });

    this.adminDashboardService.getKpisCritiques().subscribe({
      next: (kpisCritiques) => this.kpisCritiques = kpisCritiques,
      error: (err) => console.error('Erreur chargement KPIs critiques:', err)
    });

    this.adminDashboardService.getRepartition().subscribe({
      next: (repartition) => this.repartition = repartition,
      error: (err) => console.error('Erreur chargement répartition:', err)
    });

    this.adminDashboardService.getGraphiques().subscribe({
      next: (graphiques) => this.graphiquesData = graphiques,
      error: (err) => console.error('Erreur chargement graphiques:', err)
    });
  }

  countImports(status: ImportStatus): number {
    return this.recentImports.filter(i => i.statut === status).length;
  }

  getStatusClass(status: ImportStatus): string {
    switch (status) {
      case ImportStatus.EN_ATTENTE:   return 'pill-pending';
      case ImportStatus.EN_TRAITEMENT:return 'pill-processing';
      case ImportStatus.TRAITE:       return 'pill-success';
      case ImportStatus.ERREUR:       return 'pill-error';
      default: return '';
    }
  }

  goToImport():     void { this.router.navigate(['/import']); }
  goToKpi():        void { this.router.navigate(['/kpis']); }
  goToHistorique(): void { this.router.navigate(['/historique']); }
  goToMapping():    void { this.router.navigate(['/mapping']); }
  goToUsers():      void { this.router.navigate(['/users']); }
}