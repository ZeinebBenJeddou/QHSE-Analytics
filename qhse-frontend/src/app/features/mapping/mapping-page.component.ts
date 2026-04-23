import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ImportService } from '../../core/services/import.service';
import { ColonneDetecteeResponse } from '../../shared/models/colonne-detectee-response';
import { UserMappingTemplateResponse } from '../../shared/models/user-mapping-template-response';
import { SaveMappingRequest } from '../../shared/models/save-mapping-request';
import { TypeValeur } from '../../shared/enums/type-valeur.enum';

type DetectColumn = ColonneDetecteeResponse & { typeValeur: TypeValeur };

@Component({
  selector: 'app-mapping-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatSnackBarModule,
    MatToolbarModule
  ],
  template: `
    <div class="mapping-page-container">
      <mat-toolbar color="primary">
        <h1>Gestion des mappings</h1>
      </mat-toolbar>

      <div class="mapping-content">
        <mat-card class="mapping-card">
          <h2>Créer un nouveau mapping</h2>
          <form [formGroup]="mappingForm" class="mapping-form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nom du template</mat-label>
              <input matInput formControlName="nom" placeholder="Template client" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Ligne d'entête</mat-label>
              <input matInput type="number" formControlName="ligneEntete" placeholder="1" />
            </mat-form-field>

            <div class="file-upload-row">
              <input type="file" hidden #fileInput (change)="onFileSelected($event)" accept=".xlsx,.xls,.csv" />
              <button mat-stroked-button color="primary" (click)="fileInput.click()">
                <mat-icon>attach_file</mat-icon>
                Sélectionner le fichier
              </button>
              <span class="file-name" *ngIf="selectedFile">{{ selectedFile.name }}</span>
            </div>

            <div class="actions-row">
              <button mat-raised-button color="primary" (click)="detectColumns()" [disabled]="!selectedFile || mappingForm.invalid || isLoading">
                <mat-icon>search</mat-icon>
                Détecter les colonnes
              </button>
              <button mat-raised-button color="accent" (click)="saveMapping()" [disabled]="!canSaveMapping() || isLoading">
                <mat-icon>save</mat-icon>
                Sauvegarder le mapping
              </button>
            </div>
          </form>
        </mat-card>

        <mat-card class="preview-card" *ngIf="detectedColumns.length > 0">
          <h2>Aperçu des colonnes détectées</h2>
          <div class="table-wrapper">
            <table mat-table [dataSource]="detectedColumns" class="mapping-table">
              <ng-container matColumnDef="index">
                <th mat-header-cell *matHeaderCellDef>#</th>
                <td mat-cell *matCellDef="let column">{{ column.indexColonne + 1 }}</td>
              </ng-container>

              <ng-container matColumnDef="nomColonne">
                <th mat-header-cell *matHeaderCellDef>Nom colonne</th>
                <td mat-cell *matCellDef="let column">{{ column.nomColonne }}</td>
              </ng-container>

              <ng-container matColumnDef="kpiSuggere">
                <th mat-header-cell *matHeaderCellDef>KPI suggéré</th>
                <td mat-cell *matCellDef="let column">{{ column.kpiSuggere?.nom || '-' }}</td>
              </ng-container>

              <ng-container matColumnDef="typeValeur">
                <th mat-header-cell *matHeaderCellDef>Type de valeur</th>
                <td mat-cell *matCellDef="let column">
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-select [(value)]="column.typeValeur" (selectionChange)="onTypeChange(column, $event.value)">
                      <mat-option [value]="TypeValeur.VALEUR_N1">Valeur N-1</mat-option>
                      <mat-option [value]="TypeValeur.VALEUR_N">Valeur N</mat-option>
                      <mat-option [value]="TypeValeur.IGNORE">Ignorer</mat-option>
                    </mat-select>
                  </mat-form-field>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="mappingColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: mappingColumns;"></tr>
            </table>
          </div>
        </mat-card>

        <mat-card class="saved-mappings-card">
          <h2>Templates enregistrés</h2>
          <div *ngIf="savedMappings.length === 0" class="empty-state">
            Aucun template enregistré pour le moment.
          </div>
          <div *ngIf="savedMappings.length > 0" class="saved-list">
            <div *ngFor="let template of savedMappings" class="template-item">
              <div>
                <strong>{{ template.nom }}</strong>
                <div>Ligne entête : {{ template.ligneEntete }}</div>
                <div class="small-text">{{ template.colonnes.length }} colonnes</div>
              </div>
              <button mat-icon-button color="warn" matTooltip="Supprimer ce template" (click)="deleteMapping(template.id)">
                <mat-icon>delete</mat-icon>
              </button>
            </div>
          </div>
          <button mat-stroked-button color="primary" (click)="loadMappings()" [disabled]="isLoading">
            <mat-icon>refresh</mat-icon>
            Actualiser
          </button>
        </mat-card>
      </div>
    </div>
  `,
  styles: [
    `
      .mapping-page-container {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .mapping-content {
        display: grid;
        grid-template-columns: 1fr;
        gap: 1rem;
        padding: 1rem;
      }

      .mapping-card,
      .preview-card,
      .saved-mappings-card {
        padding: 1rem;
      }

      .mapping-form {
        display: grid;
        gap: 1rem;
      }

      .full-width {
        width: 100%;
      }

      .file-upload-row,
      .actions-row {
        display: flex;
        gap: 1rem;
        align-items: center;
        flex-wrap: wrap;
      }

      .file-name {
        font-weight: 500;
        color: #0b4a94;
      }

      .table-wrapper {
        overflow-x: auto;
      }

      .mapping-table {
        width: 100%;
      }

      .saved-list {
        display: grid;
        gap: 0.75rem;
      }

      .template-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
      }

      .small-text {
        font-size: 0.85rem;
        color: rgba(0, 0, 0, 0.65);
      }

      .empty-state {
        padding: 1rem 0;
        color: rgba(0, 0, 0, 0.7);
      }
    `
  ]
})
export class MappingPage implements OnInit {
  mappingForm: FormGroup;
  selectedFile: File | null = null;
  detectedColumns: DetectColumn[] = [];
  savedMappings: UserMappingTemplateResponse[] = [];
  mappingColumns = ['index', 'nomColonne', 'kpiSuggere', 'typeValeur'];
  TypeValeur = TypeValeur;
  isLoading = false;

  constructor(
    private formBuilder: FormBuilder,
    private importService: ImportService,
    private snackBar: MatSnackBar
  ) {
    this.mappingForm = this.formBuilder.group({
      nom: ['', Validators.required],
      ligneEntete: [1, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.loadMappings();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];
    }
  }

  detectColumns(): void {
    if (!this.selectedFile || this.mappingForm.invalid) {
      return;
    }

    const ligneEntete = this.mappingForm.value.ligneEntete;
    this.isLoading = true;
    this.importService.detectColumns(this.selectedFile, ligneEntete).subscribe({
      next: (columns) => {
        this.detectedColumns = columns.map((column) => ({
          ...column,
          typeValeur: TypeValeur.IGNORE
        }));
        this.isLoading = false;
        this.snackBar.open('Colonnes détectées', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur détection colonnes:', err);
      }
    });
  }

  onTypeChange(column: DetectColumn, type: TypeValeur): void {
    column.typeValeur = type;
  }

  canSaveMapping(): boolean {
    return this.detectedColumns.length > 0 && this.mappingForm.valid;
  }

  saveMapping(): void {
    if (!this.canSaveMapping()) {
      return;
    }

    const request: SaveMappingRequest = {
      nom: this.mappingForm.value.nom,
      ligneEntete: this.mappingForm.value.ligneEntete,
      colonnes: this.detectedColumns.map((column) => ({
        nomColonne: column.nomColonne,
        indexColonne: column.indexColonne,
        kpiId: column.kpiSuggere?.id,
        typeValeur: column.typeValeur
      }))
    };

    this.isLoading = true;
    this.importService.saveMapping(request).subscribe({
      next: () => {
        this.isLoading = false;
        this.snackBar.open('Mapping sauvegardé', 'Fermer', { duration: 3000 });
        this.loadMappings();
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur sauvegarde mapping:', err);
      }
    });
  }

  loadMappings(): void {
    this.isLoading = true;
    this.importService.getMappings().subscribe({
      next: (mappings) => {
        this.savedMappings = mappings;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur chargement mappings:', err);
      }
    });
  }

  deleteMapping(mappingId: number): void {
    if (!confirm('Supprimer ce template de mapping ?')) {
      return;
    }

    this.isLoading = true;
    this.importService.deleteMapping(mappingId).subscribe({
      next: () => {
        this.isLoading = false;
        this.snackBar.open('Template supprimé', 'Fermer', { duration: 3000 });
        this.loadMappings();
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur suppression mapping:', err);
      }
    });
  }
}
