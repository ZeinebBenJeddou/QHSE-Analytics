import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { ImportService } from '../../core/services/import.service';
import { ImportSessionResponse } from '../../shared/models/import-session-response';
import { ImportStatus } from '../../shared/enums/import-status.enum';
import { ImportMode } from '../../shared/enums/import-mode.enum';

@Component({
  selector: 'app-historique-page',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatToolbarModule,
    MatTooltipModule,
    MatSnackBarModule,
    LoadingSpinnerComponent
  ],
  template: `
    <div class="historique-container">
      <mat-toolbar color="primary">
        <h1 class="title">Historique des imports</h1>
        <span class="spacer"></span>
        <button mat-raised-button color="accent" (click)="goToImport()">
          <mat-icon>add</mat-icon>
          Nouvel import
        </button>
      </mat-toolbar>

      <div class="content">
        <app-loading-spinner [isLoading]="isLoading" message="Chargement de l'historique..."></app-loading-spinner>

        <mat-card *ngIf="!isLoading" class="table-card">
          <div class="mat-elevation-z8">
            <table mat-table [dataSource]="dataSource" matSort class="historique-table">
              <!-- Date Column -->
              <ng-container matColumnDef="date">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Date</th>
                <td mat-cell *matCellDef="let element">
                  {{ element.createdAt | date:'dd/MM/yyyy HH:mm' }}
                </td>
              </ng-container>

              <!-- Fichier Column -->
              <ng-container matColumnDef="fichier">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Fichier</th>
                <td mat-cell *matCellDef="let element">{{ element.nomFichier }}</td>
              </ng-container>

              <!-- Mode Column -->
              <ng-container matColumnDef="mode">
                <th mat-header-cell *matHeaderCellDef>Mode</th>
                <td mat-cell *matCellDef="let element">
                  <mat-chip-set>
                    <mat-chip [ngClass]="'mode-' + element.mode.toLowerCase()">
                      {{ element.mode }}
                    </mat-chip>
                  </mat-chip-set>
                </td>
              </ng-container>

              <!-- Périodes Column -->
              <ng-container matColumnDef="periodes">
                <th mat-header-cell *matHeaderCellDef>Périodes</th>
                <td mat-cell *matCellDef="let element">
                  {{ element.periodeN1 }} - {{ element.periodeN }}
                </td>
              </ng-container>

              <!-- Statut Column -->
              <ng-container matColumnDef="statut">
                <th mat-header-cell *matHeaderCellDef>Statut</th>
                <td mat-cell *matCellDef="let element">
                  <mat-chip-set>
                    <mat-chip [ngClass]="'status-' + element.statut.toLowerCase()">
                      {{ element.statut }}
                    </mat-chip>
                  </mat-chip-set>
                </td>
              </ng-container>

              <!-- Nombre Column -->
              <ng-container matColumnDef="nombre">
                <th mat-header-cell *matHeaderCellDef>Total</th>
                <td mat-cell *matCellDef="let element">{{ element.nombreTotal }}</td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let element" class="actions-cell">
                  <button mat-icon-button matTooltip="Voir résultats"
                          (click)="viewResults(element.id)">
                    <mat-icon>visibility</mat-icon>
                  </button>
                  <button mat-icon-button matTooltip="Analyse IA"
                          (click)="viewAnalyseIa(element.id)">
                    <mat-icon>auto_awesome</mat-icon>
                  </button>
                  <button mat-icon-button matTooltip="Supprimer"
                          (click)="deleteImport(element.id)">
                    <mat-icon color="warn">delete</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
            </table>
            <mat-paginator [pageSizeOptions]="[5, 10, 25]" showFirstLastButtons></mat-paginator>
          </div>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .historique-container {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .title {
      margin: 0;
    }

    .spacer {
      flex: 1 1 auto;
    }

    .content {
      flex: 1;
      overflow: auto;
      padding: 1rem;
    }

    .table-card {
      overflow-x: auto;
    }

    .historique-table {
      width: 100%;
    }

    .actions-cell {
      text-align: center;
    }

    .mode-template_officiel {
      background-color: #c8e6c9;
      color: #1b5e20;
    }

    .mode-fichier_libre {
      background-color: #bbdefb;
      color: #0d47a1;
    }

    .status-en_attente {
      background-color: #fff9c4;
      color: #f57f17;
    }

    .status-en_traitement {
      background-color: #ffe0b2;
      color: #e65100;
    }

    .status-traite {
      background-color: #c8e6c9;
      color: #1b5e20;
    }

    .status-erreur {
      background-color: #ffcdd2;
      color: #b71c1c;
    }
  `]
})
export class HistoriquePage implements OnInit {
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  displayedColumns: string[] = ['date', 'fichier', 'mode', 'periodes', 'statut', 'nombre', 'actions'];
  dataSource = new MatTableDataSource<ImportSessionResponse>();
  
  isLoading = false;
  ImportStatus = ImportStatus;
  ImportMode = ImportMode;

  constructor(
    private importService: ImportService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadHistorique();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  loadHistorique(): void {
    this.isLoading = true;
    this.importService.getHistorique().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.isLoading = false;
      }
    });
  }

  viewResults(sessionId: number): void {
    this.router.navigate(['/resultats'], { queryParams: { sessionId } });
  }

  viewAnalyseIa(sessionId: number): void {
    this.router.navigate(['/analyse-ia', sessionId]);
  }

  deleteImport(sessionId: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cet import ?')) {
      this.importService.deleteSession(sessionId).subscribe({
        next: () => {
          this.snackBar.open('Import supprimé', 'Fermer', { duration: 3000 });
          this.loadHistorique();
        },
        error: (err) => {
          console.error('Erreur:', err);
        }
      });
    }
  }

  goToImport(): void {
    this.router.navigate(['/import']);
  }
}
