import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { ImportService } from '../../core/services/import.service';
import { ResultatGlobalResponse } from '../../shared/models/resultat-global-response';
import { NiveauVariation } from '../../shared/enums/niveau-variation.enum';
import { StatusColorService } from '../../shared/services/status-color.service';

@Component({
  selector: 'app-resultats-page',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatIconModule,
    MatToolbarModule,
    MatButtonModule,
    MatExpansionModule,
    LoadingSpinnerComponent
  ],
  template: `
    <div class="resultats-container">
      <mat-toolbar color="primary">
        <h1 class="title">Résultats de l'import</h1>
      </mat-toolbar>

      <div class="content">
        <app-loading-spinner [isLoading]="isLoading" message="Chargement des résultats..."></app-loading-spinner>

        <div *ngIf="!isLoading && resultat" class="results-section">
          <!-- Summary Cards -->
          <div class="summary-cards">
            <mat-card class="summary-card">
              <mat-card-content>
                <div class="metric-label">Période N-1</div>
                <div class="metric-value">{{ resultat.periodeN1 }}</div>
              </mat-card-content>
            </mat-card>

            <mat-card class="summary-card">
              <mat-card-content>
                <div class="metric-label">Période N</div>
                <div class="metric-value">{{ resultat.periodeN }}</div>
              </mat-card-content>
            </mat-card>

            <mat-card class="summary-card">
              <mat-card-content>
                <div class="metric-label">Total KPI</div>
                <div class="metric-value">{{ resultat.resultats.length }}</div>
              </mat-card-content>
            </mat-card>
          </div>

          <!-- Variation Levels Distribution -->
          <div class="distribution-cards">
            <mat-card class="level-card faible">
              <mat-card-content>
                <mat-icon>trending_up</mat-icon>
                <div>
                  <div class="level-label">FAIBLE</div>
                  <div class="level-count">{{ resultat.nombreFaible }}</div>
                </div>
              </mat-card-content>
            </mat-card>

            <mat-card class="level-card modere">
              <mat-card-content>
                <mat-icon>trending_flat</mat-icon>
                <div>
                  <div class="level-label">MODÉRÉ</div>
                  <div class="level-count">{{ resultat.nombreModere }}</div>
                </div>
              </mat-card-content>
            </mat-card>

            <mat-card class="level-card critique">
              <mat-card-content>
                <mat-icon>trending_down</mat-icon>
                <div>
                  <div class="level-label">CRITIQUE</div>
                  <div class="level-count">{{ resultat.nombreCritique }}</div>
                </div>
              </mat-card-content>
            </mat-card>
          </div>

          <!-- AI Analysis -->
          <mat-card *ngIf="resultat.analyseGlobaleIa" class="analysis-card">
            <mat-card-header>
              <mat-card-title>
                <mat-icon>auto_awesome</mat-icon>
                Analyse Globale IA
              </mat-card-title>
            </mat-card-header>
            <mat-card-content>
              {{ resultat.analyseGlobaleIa }}
            </mat-card-content>
          </mat-card>

          <!-- Results Table -->
          <mat-card class="results-table-card">
            <mat-card-header>
              <mat-card-title>Détails des KPI</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="mat-elevation-z8 table-container">
                <table mat-table [dataSource]="resultatDataSource" class="results-table">
                  <!-- KPI -->
                  <ng-container matColumnDef="kpiNom">
                    <th mat-header-cell *matHeaderCellDef>KPI</th>
                    <td mat-cell *matCellDef="let element">{{ element.kpiNom }}</td>
                  </ng-container>

                  <!-- Catégorie -->
                  <ng-container matColumnDef="categorie">
                    <th mat-header-cell *matHeaderCellDef>Catégorie</th>
                    <td mat-cell *matCellDef="let element">{{ element.categorieLibelle }}</td>
                  </ng-container>

                  <!-- Valeurs -->
                  <ng-container matColumnDef="valeurs">
                    <th mat-header-cell *matHeaderCellDef>N-1 / N</th>
                    <td mat-cell *matCellDef="let element">
                      {{ element.valeurN1 }} / {{ element.valeurN }}
                    </td>
                  </ng-container>

                  <!-- Variation -->
                  <ng-container matColumnDef="variation">
                    <th mat-header-cell *matHeaderCellDef>Variation</th>
                    <td mat-cell *matCellDef="let element">
                      <span class="variation">
                        {{ element.variationRelative | number:'1.2-2' }}%
                      </span>
                    </td>
                  </ng-container>

                  <!-- Niveau -->
                  <ng-container matColumnDef="niveau">
                    <th mat-header-cell *matHeaderCellDef>Niveau</th>
                    <td mat-cell *matCellDef="let element">
                      <mat-chip-set>
                        <mat-chip [ngClass]="'niveau-' + (element.niveauVariation || '').toLowerCase()">
                          {{ element.niveauVariation }}
                        </mat-chip>
                      </mat-chip-set>
                    </td>
                  </ng-container>

                  <!-- Tendance -->
                  <ng-container matColumnDef="tendance">
                    <th mat-header-cell *matHeaderCellDef>Tendance</th>
                    <td mat-cell *matCellDef="let element">
                      <mat-icon class="tendance-icon">
                        {{ getTendanceIcon(element.tendance) }}
                      </mat-icon>
                      {{ element.tendance }}
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="resultatColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: resultatColumns;"></tr>
                </table>
              </div>
            </mat-card-content>
          </mat-card>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .resultats-container {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .title {
      margin: 0;
    }

    .content {
      flex: 1;
      overflow: auto;
      padding: 2rem 1rem;
    }

    .summary-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }

    .summary-card {
      text-align: center;
    }

    .metric-label {
      color: #666;
      font-size: 0.9rem;
      margin-bottom: 0.5rem;
    }

    .metric-value {
      font-size: 2rem;
      font-weight: bold;
      color: #0b4a94;
    }

    .distribution-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }

    .level-card {
      display: flex;
      align-items: center;
      padding: 1.5rem;
    }

    .level-card mat-icon {
      font-size: 2rem;
      width: 2rem;
      height: 2rem;
      margin-right: 1rem;
    }

    .level-label {
      font-weight: 600;
      margin-bottom: 0.25rem;
    }

    .level-count {
      font-size: 1.5rem;
      font-weight: bold;
    }

    .faible {
      background-color: #c8e6c9;
      color: #1b5e20;
    }

    .modere {
      background-color: #ffe0b2;
      color: #e65100;
    }

    .critique {
      background-color: #ffcdd2;
      color: #b71c1c;
    }

    .analysis-card {
      margin-bottom: 2rem;
    }

    .analysis-card mat-icon {
      margin-right: 0.5rem;
      vertical-align: middle;
    }

    .table-container {
      overflow-x: auto;
    }

    .results-table {
      width: 100%;
    }

    .variation {
      font-weight: 600;
      color: #0b4a94;
    }

    .niveau-faible {
      background-color: #c8e6c9;
      color: #1b5e20;
    }

    .niveau-modere {
      background-color: #ffe0b2;
      color: #e65100;
    }

    .niveau-critique {
      background-color: #ffcdd2;
      color: #b71c1c;
    }

    .tendance-icon {
      margin-right: 0.5rem;
      vertical-align: middle;
    }
  `]
})
export class ResultatsPage implements OnInit {
  resultat: ResultatGlobalResponse | null = null;
  resultatDataSource = new MatTableDataSource<any>();
  resultatColumns = ['kpiNom', 'categorie', 'valeurs', 'variation', 'niveau', 'tendance'];
  
  isLoading = false;
  sessionId: number | null = null;

  constructor(
    private importService: ImportService,
    private route: ActivatedRoute,
    private statusColorService: StatusColorService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.sessionId = params['sessionId'];
      if (this.sessionId) {
        this.loadResultats();
      }
    });
  }

  loadResultats(): void {
    if (!this.sessionId) return;
    this.isLoading = true;
    this.importService.getResultats(this.sessionId).subscribe({
      next: (response) => {
        this.resultat = response;
        this.resultatDataSource.data = response.resultats;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur:', err);
      }
    });
  }

  getTendanceIcon(tendance: string): string {
    switch (tendance) {
      case 'HAUSSE':
        return 'trending_up';
      case 'BAISSE':
        return 'trending_down';
      case 'STABLE':
        return 'trending_flat';
      default:
        return 'trending_flat';
    }
  }
}
