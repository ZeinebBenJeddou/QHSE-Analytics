import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormControl } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { KpiService, CategorieKpiResponse } from '../../core/services/kpi.service';
import { KpiResponse } from '../../shared/models/kpi-response';
import { AuthStorageService } from '../../core/services/auth-storage.service';
import { KpiFormDialogComponent } from './kpi-form-dialog.component';

@Component({
  selector: 'app-kpi-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatPaginatorModule,
    MatSortModule,
    MatSnackBarModule,
    MatDialogModule,
    MatCardModule,
    MatToolbarModule,
    MatTooltipModule,
    MatChipsModule,
    LoadingSpinnerComponent
  ],
  template: `
    <div class="kpi-page-container">
      <mat-toolbar color="primary">
        <h1 class="kpi-title">Gestion des KPI</h1>
        <span class="spacer"></span>
        <button mat-raised-button color="accent" *ngIf="isAdmin" (click)="openCreateDialog()">
          <mat-icon>add</mat-icon>
          Nouveau KPI
        </button>
      </mat-toolbar>

      <div class="kpi-content">
        <!-- Filters -->
        <mat-card class="filter-card">
          <mat-card-content>
            <div class="filter-container">
              <mat-form-field appearance="outline">
                <mat-label>Catégorie</mat-label>
                <mat-select [formControl]="categoryControl" (selectionChange)="onCategoryChange($event.value)">
                  <mat-option value="">Toutes les catégories</mat-option>
                  <mat-option *ngFor="let cat of categories" [value]="cat.code">
                    {{ cat.libelle }}
                  </mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Rechercher</mat-label>
                <input matInput [formControl]="searchControl" 
                       placeholder="Nom du KPI...">
                <mat-icon matSuffix>search</mat-icon>
              </mat-form-field>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Table -->
        <app-loading-spinner [isLoading]="isLoading" message="Chargement des KPI..."></app-loading-spinner>

        <mat-card *ngIf="!isLoading" class="table-card">
          <div class="mat-elevation-z8">
            <table mat-table [dataSource]="dataSource" matSort class="kpi-table">
              <!-- Nom Column -->
              <ng-container matColumnDef="nom">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Nom</th>
                <td mat-cell *matCellDef="let element">{{ element.nom }}</td>
              </ng-container>

              <!-- Catégorie Column -->
              <ng-container matColumnDef="categorie">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Catégorie</th>
                <td mat-cell *matCellDef="let element">
                  <mat-chip-set>
                    <mat-chip [ngClass]="'category-' + element.categorieCode">
                      {{ element.categorieLibelle }}
                    </mat-chip>
                  </mat-chip-set>
                </td>
              </ng-container>

              <!-- Unité Column -->
              <ng-container matColumnDef="unite">
                <th mat-header-cell *matHeaderCellDef>Unité</th>
                <td mat-cell *matCellDef="let element">{{ element.unite }}</td>
              </ng-container>

              <!-- Seuils Column -->
              <ng-container matColumnDef="seuils">
                <th mat-header-cell *matHeaderCellDef>Seuils (F/M/C)</th>
                <td mat-cell *matCellDef="let element">
                  {{ element.seuilFaible }}/{{ element.seuilModere }}/{{ element.seuilCritique }}
                </td>
              </ng-container>

              <!-- Statut Column -->
              <ng-container matColumnDef="statut">
                <th mat-header-cell *matHeaderCellDef>Statut</th>
                <td mat-cell *matCellDef="let element">
                  <mat-chip-set>
                    <mat-chip [ngClass]="element.isActive ? 'status-active' : 'status-inactive'">
                      {{ element.isActive ? 'Actif' : 'Inactif' }}
                    </mat-chip>
                  </mat-chip-set>
                </td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let element" class="actions-cell">
                  <button mat-icon-button matTooltip="Voir détails" (click)="viewKpi(element)">
                    <mat-icon>visibility</mat-icon>
                  </button>
                  <button mat-icon-button matTooltip="Modifier" *ngIf="isAdmin" (click)="editKpi(element)">
                    <mat-icon>edit</mat-icon>
                  </button>
                  <button mat-icon-button matTooltip="Supprimer" *ngIf="isAdmin" (click)="deleteKpi(element.id)">
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
    .kpi-page-container {
      height: 100%;
      display: flex;
      flex-direction: column;
    }
    
    .kpi-title {
      margin: 0;
      font-size: 24px;
    }

    .spacer {
      flex: 1 1 auto;
    }

    .kpi-content {
      flex: 1;
      overflow: auto;
      padding: 1rem;
    }

    .filter-card {
      margin-bottom: 1rem;
    }

    .filter-container {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
    }

    mat-form-field {
      min-width: 200px;
    }

    .table-card {
      overflow-x: auto;
    }

    .kpi-table {
      width: 100%;
    }

    .actions-cell {
      text-align: center;
    }

    .category-QH,
    .category-HS,
    .category-E {
      background-color: #e3f2fd;
      color: #0b4a94;
    }

    .status-active {
      background-color: #c8e6c9;
      color: #1b5e20;
    }

    .status-inactive {
      background-color: #eeeeee;
      color: #616161;
    }
  `]
})
export class KpiPage implements OnInit, AfterViewInit {
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  displayedColumns: string[] = ['nom', 'categorie', 'unite', 'seuils', 'statut', 'actions'];
  dataSource = new MatTableDataSource<KpiResponse>();
  kpis: KpiResponse[] = [];
  categories: CategorieKpiResponse[] = [];
  
  categoryControl = new FormControl('');
  searchControl = new FormControl('');
  
  isLoading = false;
  isAdmin = false;

  constructor(
    private kpiService: KpiService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private authStorage: AuthStorageService
  ) {
    this.isAdmin = this.authStorage.isAdmin();
  }

  ngOnInit(): void {
    this.loadCategories();
    this.loadKpis();
    this.searchControl.valueChanges.subscribe(() => this.applyFilters());
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  loadCategories(): void {
    this.kpiService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des catégories:', err);
      }
    });
  }

  loadKpis(categorie?: string): void {
    this.isLoading = true;
    this.kpiService.getKpis(categorie).subscribe({
      next: (data) => {
        this.kpis = data;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des KPI:', err);
        this.isLoading = false;
      }
    });
  }

  onCategoryChange(category: string): void {
    this.loadKpis(category || undefined);
  }

  onSearchChange(term: string): void {
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = this.kpis;
    const searchTerm = this.searchControl.value || '';

    if (searchTerm) {
      filtered = filtered.filter(kpi =>
        kpi.nom.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    this.dataSource.data = filtered;
  }

  viewKpi(kpi: KpiResponse): void {
    this.dialog.open(KpiFormDialogComponent, {
      width: '620px',
      data: { mode: 'view', kpi }
    });
  }

  editKpi(kpi: KpiResponse): void {
    const dialogRef = this.dialog.open(KpiFormDialogComponent, {
      width: '620px',
      data: { mode: 'edit', kpi }
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('KPI mis à jour', 'Fermer', { duration: 3000 });
        this.loadKpis(this.categoryControl.value || undefined);
      }
    });
  }

  deleteKpi(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce KPI ?')) {
      this.kpiService.deleteKpi(id).subscribe({
        next: () => {
          this.snackBar.open('KPI supprimé avec succès', 'Fermer', { duration: 3000 });
          this.loadKpis();
        },
        error: (err) => {
          console.error('Erreur lors de la suppression:', err);
        }
      });
    }
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(KpiFormDialogComponent, {
      width: '620px',
      data: { mode: 'create' }
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('KPI créé', 'Fermer', { duration: 3000 });
        this.loadKpis(this.categoryControl.value || undefined);
      }
    });
  }
}
