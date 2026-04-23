import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { AnalyseIaService } from '../../core/services/analyse-ia.service';
import { AnalyseCompleteResponse, AnalyseCategorieResponse, AnalyseGlobaleResponse } from '../../shared/models/analyse-ia-response';
import { NiveauVariation } from '../../shared/enums/niveau-variation.enum';

@Component({
  selector: 'app-analyse-ia-page',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatTabsModule,
    MatExpansionModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    LoadingSpinnerComponent
  ],
  template: `
    <div class="analyse-ia-container">
      <mat-toolbar color="primary">
        <h1 class="title">
          <mat-icon>auto_awesome</mat-icon>
          Analyse IA avec Groq
        </h1>
        <span class="spacer"></span>
        <button mat-raised-button color="accent" (click)="regenererAnalyse()" [disabled]="isRegenerating">
          <mat-icon>refresh</mat-icon>
          Régénérer
        </button>
      </mat-toolbar>

      <div class="content">
        <app-loading-spinner [isLoading]="isLoading" message="Chargement de l'analyse IA..."></app-loading-spinner>

        <div *ngIf="!isLoading && analyseComplete" class="analysis-content">
          <!-- Summary -->
          <mat-card class="summary-card">
            <mat-card-header>
              <mat-card-title>Résumé de l'Analyse</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="summary-info">
                <div class="info-item">
                  <strong>Session d'import:</strong> {{ analyseComplete.importSessionId }}
                </div>
                <div class="info-item">
                  <strong>Période N-1:</strong> {{ analyseComplete.periodeN1 }}
                </div>
                <div class="info-item">
                  <strong>Période N:</strong> {{ analyseComplete.periodeN }}
                </div>
                <div class="info-item">
                  <strong>Nombre de KPI analysés:</strong> {{ analyseComplete.analysesKpis.length }}
                </div>
                <div class="info-item">
                  <strong>Nombre de catégories:</strong> {{ analyseComplete.analysesCategories.length }}
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <!-- Tabs for different analyses -->
          <mat-tab-group>
            <!-- KPI Analyses -->
            <mat-tab label="Analyses des KPI">
              <div class="tab-content">
                <mat-accordion>
                  <mat-expansion-panel *ngFor="let kpi of analyseComplete.analysesKpis" class="kpi-panel">
                    <mat-expansion-panel-header>
                      <mat-panel-title>
                        <mat-icon>analytics</mat-icon>
                        {{ kpi.kpiNom }}
                      </mat-panel-title>
                      <mat-panel-description>
                        {{ kpi.categorieLibelle }} - Variation: {{ kpi.variationRelative | number:'1.2-2' }}%
                      </mat-panel-description>
                    </mat-expansion-panel-header>
                    <div class="kpi-content">
                      <div class="kpi-values">
                        <mat-chip-set>
                          <mat-chip color="primary">N-1: {{ kpi.valeurN1 }}</mat-chip>
                          <mat-chip color="accent">N: {{ kpi.valeurN }}</mat-chip>
                          <mat-chip [color]="getNiveauColor(kpi.niveauVariation)">
                            {{ kpi.niveauVariation }}
                          </mat-chip>
                        </mat-chip-set>
                      </div>
                      <div class="analyse-text" *ngIf="kpi.analyseIa">
                        <h4>Analyse IA:</h4>
                        <p>{{ kpi.analyseIa }}</p>
                      </div>
                    </div>
                  </mat-expansion-panel>
                </mat-accordion>
              </div>
            </mat-tab>

            <!-- Category Analyses -->
            <mat-tab label="Analyses par Catégorie">
              <div class="tab-content">
                <mat-card *ngFor="let categorie of analyseComplete.analysesCategories" class="categorie-card">
                  <mat-card-header>
                    <mat-card-title>
                      <mat-icon>category</mat-icon>
                      {{ categorie.categorieLibelle }} ({{ categorie.categorieCode }})
                    </mat-card-title>
                    <mat-card-subtitle>
                      Créé le {{ categorie.createdAt | date:'short' }}
                    </mat-card-subtitle>
                  </mat-card-header>
                  <mat-card-content>
                    <div class="categorie-content">
                      {{ categorie.contenu }}
                    </div>
                  </mat-card-content>
                </mat-card>
              </div>
            </mat-tab>

            <!-- Global Analysis -->
            <mat-tab label="Analyse Globale">
              <div class="tab-content">
                <mat-card *ngIf="analyseComplete.analyseGlobale" class="globale-card">
                  <mat-card-header>
                    <mat-card-title>
                      <mat-icon>assessment</mat-icon>
                      Synthèse Générale
                    </mat-card-title>
                    <mat-card-subtitle>
                      Créé le {{ analyseComplete.analyseGlobale.createdAt | date:'short' }}
                    </mat-card-subtitle>
                  </mat-card-header>
                  <mat-card-content>
                    <div class="globale-content">
                      <h4>Synthèse:</h4>
                      <p>{{ analyseComplete.analyseGlobale.synthese }}</p>
                      <h4>Plan d'Actions:</h4>
                      <p>{{ analyseComplete.analyseGlobale.planActions }}</p>
                    </div>
                  </mat-card-content>
                </mat-card>
              </div>
            </mat-tab>
          </mat-tab-group>
        </div>

        <div *ngIf="!isLoading && !analyseComplete" class="no-data">
          <mat-icon>info</mat-icon>
          <p>Aucune analyse IA disponible pour cette session d'import.</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .analyse-ia-container {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }

    .title {
      display: flex;
      align-items: center;
      margin: 0;
    }

    .title mat-icon {
      margin-right: 0.5rem;
    }

    .spacer {
      flex: 1 1 auto;
    }

    .content {
      flex: 1;
      overflow: auto;
      padding: 2rem 1rem;
    }

    .summary-card {
      margin-bottom: 2rem;
    }

    .summary-info {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
    }

    .info-item {
      padding: 0.5rem;
      background-color: #f5f5f5;
      border-radius: 4px;
    }

    .tab-content {
      padding: 1rem 0;
    }

    .kpi-panel {
      margin-bottom: 1rem;
    }

    .kpi-content {
      padding: 1rem 0;
    }

    .kpi-values {
      margin-bottom: 1rem;
    }

    .analyse-text {
      background-color: #f9f9f9;
      padding: 1rem;
      border-radius: 4px;
      border-left: 4px solid #0b4a94;
    }

    .analyse-text h4 {
      margin: 0 0 0.5rem 0;
      color: #0b4a94;
    }

    .categorie-card {
      margin-bottom: 1rem;
    }

    .categorie-content {
      white-space: pre-wrap;
      line-height: 1.6;
    }

    .globale-card {
      margin-bottom: 1rem;
    }

    .globale-content h4 {
      color: #0b4a94;
      margin-top: 1.5rem;
      margin-bottom: 0.5rem;
    }

    .globale-content h4:first-child {
      margin-top: 0;
    }

    .globale-content p {
      white-space: pre-wrap;
      line-height: 1.6;
    }

    .no-data {
      text-align: center;
      padding: 3rem;
      color: #666;
    }

    .no-data mat-icon {
      font-size: 4rem;
      width: 4rem;
      height: 4rem;
      margin-bottom: 1rem;
    }
  `]
})
export class AnalyseIaPage implements OnInit {
  analyseComplete: AnalyseCompleteResponse | null = null;
  isLoading = false;
  isRegenerating = false;
  importId: number | null = null;

  constructor(
    private analyseIaService: AnalyseIaService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.importId = +params['importId'];
      if (this.importId) {
        this.loadAnalyseComplete();
      }
    });
  }

  loadAnalyseComplete(): void {
    if (!this.importId) return;
    this.isLoading = true;
    this.analyseIaService.getAnalyseComplete(this.importId).subscribe({
      next: (response) => {
        this.analyseComplete = response;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur lors du chargement de l\'analyse:', err);
      }
    });
  }

  regenererAnalyse(): void {
    if (!this.importId) return;
    this.isRegenerating = true;
    this.cdr.detectChanges(); // Force change detection
    this.analyseIaService.regenerer(this.importId).subscribe({
      next: (response) => {
        this.analyseComplete = response;
        this.isRegenerating = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isRegenerating = false;
        this.cdr.detectChanges();
        console.error('Erreur lors de la régénération:', err);
      }
    });
  }

  getNiveauColor(niveau: NiveauVariation | undefined): string {
    if (!niveau) return 'basic';
    switch (niveau) {
      case NiveauVariation.FAIBLE:
        return 'primary';
      case NiveauVariation.MODERE:
        return 'accent';
      case NiveauVariation.CRITIQUE:
        return 'warn';
      default:
        return 'basic';
    }
  }
}