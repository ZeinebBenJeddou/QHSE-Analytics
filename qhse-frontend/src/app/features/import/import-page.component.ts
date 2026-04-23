import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatStepperModule, MatStepper } from '@angular/material/stepper';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatToolbarModule } from '@angular/material/toolbar';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { ImportService } from '../../core/services/import.service';
import { ImportMode } from '../../shared/enums/import-mode.enum';
import { TypeValeur } from '../../shared/enums/type-valeur.enum';
import { ColonneDetecteeResponse } from '../../shared/models/colonne-detectee-response';
import { UserMappingTemplateResponse } from '../../shared/models/user-mapping-template-response';

@Component({
  selector: 'app-import-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatStepperModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatChipsModule,
    MatToolbarModule,
    FormsModule,
    LoadingSpinnerComponent
  ],
  template: `
    <div class="import-page-container">
      <mat-toolbar color="primary">
        <h1 class="import-title">Import de données</h1>
      </mat-toolbar>

      <div class="import-content">
        <mat-stepper [linear]="true" #stepper>
          <!-- Step 1: Mode Selection -->
          <mat-step [stepControl]="modeForm" [completed]="modeForm.valid && currentStep > 0">
            <ng-template matStepLabel>Mode d'import</ng-template>
            <div class="step-content">
              <h2>Sélectionnez le mode d'import</h2>
              <form [formGroup]="modeForm">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Mode</mat-label>
                  <mat-select formControlName="mode" (selectionChange)="onModeChange($event.value)">
                    <mat-option [value]="ImportMode.TEMPLATE_OFFICIEL">Template Officiel</mat-option>
                    <mat-option [value]="ImportMode.FICHIER_LIBRE">Fichier Libre</mat-option>
                  </mat-select>
                </mat-form-field>

                <button mat-raised-button color="primary" 
                        (click)="downloadTemplate()" 
                        *ngIf="selectedMode === ImportMode.TEMPLATE_OFFICIEL"
                        [disabled]="isLoading">
                  <mat-icon>download</mat-icon>
                  Télécharger le template
                </button>
              </form>
              <button mat-raised-button matStepperNext [disabled]="!modeForm.valid">Suivant</button>
            </div>
          </mat-step>

          <!-- Step 2: File Upload & Process -->
          <mat-step [stepControl]="uploadForm" [completed]="uploadForm.valid && currentStep > 1">
            <ng-template matStepLabel>Upload et traitement</ng-template>
            <div class="step-content">
              <h2>Téléchargez votre fichier et lancez le traitement automatique</h2>
              <form [formGroup]="uploadForm">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Période N-1</mat-label>
                  <input matInput type="number" formControlName="periodeN1" placeholder="2023">
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Période N</mat-label>
                  <input matInput type="number" formControlName="periodeN" placeholder="2024">
                </mat-form-field>

                <div class="file-upload">
                  <input type="file" #fileInput hidden (change)="onFileSelected($event)" accept=".xlsx,.xls,.csv">
                  <button mat-raised-button (click)="fileInput.click()" [disabled]="isLoading">
                    <mat-icon>attach_file</mat-icon>
                    Sélectionner fichier
                  </button>
                  <span *ngIf="selectedFile" class="file-name">{{ selectedFile.name }}</span>
                </div>

                <div *ngIf="selectedMode === ImportMode.FICHIER_LIBRE" class="mapping-panel">
                  <h3>Paramètres Fichier Libre</h3>

                  <div class="mapping-row">
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Template de mapping</mat-label>
                      <mat-select [(ngModel)]="selectedMappingId" [ngModelOptions]="{standalone: true}">
                        <mat-option [value]="null">Aucun</mat-option>
                        <mat-option *ngFor="let mapping of savedMappings" [value]="mapping.id">
                          {{ mapping.nom }}
                        </mat-option>
                      </mat-select>
                    </mat-form-field>

                    <button mat-stroked-button color="primary" type="button" (click)="loadMappings()" [disabled]="isLoading">
                      <mat-icon>refresh</mat-icon>
                      Charger les templates
                    </button>
                  </div>

                  <div class="mapping-row">
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Ligne d'entête</mat-label>
                      <input matInput type="number" [(ngModel)]="ligneEntete" [ngModelOptions]="{standalone: true}" min="1" />
                    </mat-form-field>
                    <button mat-stroked-button color="accent" type="button" (click)="detectColumns()" [disabled]="!selectedFile || isLoading">
                      <mat-icon>search</mat-icon>
                      Détecter les colonnes
                    </button>
                  </div>

                  <div *ngIf="detectedColumns.length > 0" class="mapping-preview">
                    <h4>Aperçu des colonnes détectées</h4>
                    <div class="table-container">
                      <table mat-table [dataSource]="detectedColumns" class="mapping-table">
                        <ng-container matColumnDef="nomColonne">
                          <th mat-header-cell *matHeaderCellDef>Colonne</th>
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
                              <mat-select [(ngModel)]="column.typeValeur" [ngModelOptions]="{standalone: true}">
                                <mat-option [value]="TypeValeur.VALEUR_N1">Valeur N-1</mat-option>
                                <mat-option [value]="TypeValeur.VALEUR_N">Valeur N</mat-option>
                                <mat-option [value]="TypeValeur.IGNORE">Ignorer</mat-option>
                              </mat-select>
                            </mat-form-field>
                          </td>
                        </ng-container>
                        <tr mat-header-row *matHeaderRowDef="['nomColonne','kpiSuggere','typeValeur']"></tr>
                        <tr mat-row *matRowDef="let row; columns: ['nomColonne','kpiSuggere','typeValeur'];"></tr>
                      </table>
                    </div>
                  </div>
                </div>
              </form>
              <div class="step-actions">
                <button mat-raised-button matStepperPrevious>Précédent</button>
                <button mat-raised-button color="primary" (click)="uploadAndProcessFile(stepper)" [disabled]="!uploadForm.valid || !selectedFile || isLoading || (selectedMode === ImportMode.FICHIER_LIBRE && !selectedMappingId)">
                  <mat-icon *ngIf="isLoading">pending</mat-icon>
                  {{ isLoading ? 'Traitement en cours...' : 'Télécharger et traiter' }}
                </button>
              </div>
            </div>
          </mat-step>

          <!-- Step 3: Results -->
          <mat-step>
            <ng-template matStepLabel>Résultats</ng-template>
            <div class="step-content">
              <h2>Résultats du traitement automatique</h2>
              <p *ngIf="resultats">Traitement complété avec succès!</p>
              <div class="result-actions">
                <button mat-stroked-button color="primary" (click)="router.navigate(['/resultats'], { queryParams: { sessionId: currentSessionId } })">
                  Voir les résultats détaillés
                </button>
              </div>
            </div>
          </mat-step>
        </mat-stepper>
      </div>
    </div>
  `,
  styles: [`
    .import-page-container {
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .import-title {
      margin: 0;
    }

    .import-content {
      flex: 1;
      overflow: auto;
      padding: 2rem 1rem;
    }

    .step-content {
      padding: 2rem;
    }

    .full-width {
      width: 100%;
      margin-bottom: 1rem;
    }

    .file-upload {
      display: flex;
      gap: 1rem;
      align-items: center;
      margin: 1rem 0;
    }

    .file-name {
      padding: 0.5rem 1rem;
      background-color: #f0f0f0;
      border-radius: 4px;
      color: #0b4a94;
      font-weight: 500;
    }

    .step-actions {
      display: flex;
      gap: 1rem;
      justify-content: flex-end;
      margin-top: 2rem;
    }
  `]
})
export class ImportPage implements OnInit {
  @ViewChild(MatStepper) stepper!: MatStepper;

  ImportMode = ImportMode;
  TypeValeur = TypeValeur;

  modeForm: FormGroup;
  uploadForm: FormGroup;

  selectedMode: ImportMode | null = ImportMode.TEMPLATE_OFFICIEL;
  selectedFile: File | null = null;
  savedMappings: UserMappingTemplateResponse[] = [];
  selectedMappingId: number | null = null;
  detectedColumns: Array<ColonneDetecteeResponse & { typeValeur: TypeValeur }> = [];
  ligneEntete = 1;
  
  isLoading = false;
  currentStep = 0;
  resultats: any = null;
  currentSessionId: number | null = null;

  constructor(
    private formBuilder: FormBuilder,
    private importService: ImportService,
    private snackBar: MatSnackBar,
    public router: Router
  ) {
    this.modeForm = this.formBuilder.group({
      mode: [ImportMode.TEMPLATE_OFFICIEL, Validators.required]
    });

    this.uploadForm = this.formBuilder.group({
      periodeN1: ['', Validators.required],
      periodeN: ['', Validators.required]
    });
  }

  ngOnInit(): void {}

  onModeChange(mode: ImportMode): void {
    this.selectedMode = mode;
    if (mode === ImportMode.FICHIER_LIBRE) {
      this.loadMappings();
    }
  }

  downloadTemplate(): void {
    this.isLoading = true;
    this.importService.downloadTemplate().subscribe({
      next: (blob) => {
        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = 'template_qhse_v1.xlsx';
        link.click();
        this.isLoading = false;
        this.snackBar.open('Template téléchargé', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur:', err);
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];
    }
  }

  uploadAndProcessFile(stepper: MatStepper): void {
    if (!this.uploadForm.valid || !this.selectedFile || !this.selectedMode) return;

    this.isLoading = true;
    const { periodeN1, periodeN } = this.uploadForm.value;

    const mappingTemplateId = this.selectedMode === ImportMode.FICHIER_LIBRE ? this.selectedMappingId : null;

    if (this.selectedMode === ImportMode.FICHIER_LIBRE && !mappingTemplateId) {
      this.isLoading = false;
      this.snackBar.open('Veuillez sélectionner un template de mapping pour le mode Fichier Libre.', 'Fermer', {
        duration: 5000,
        horizontalPosition: 'end',
        verticalPosition: 'bottom'
      });
      return;
    }

    this.importService.uploadAndProcess(
      this.selectedMode,
      mappingTemplateId,
      periodeN1,
      periodeN,
      this.selectedFile
    ).subscribe({
      next: (response: any) => {
        this.currentSessionId = response.id;
        this.resultats = response;
        this.isLoading = false;
        this.snackBar.open('Fichier traité avec succès', 'Fermer', { duration: 3000 });
        this.currentStep = 2;
        stepper.next();
      },
      error: (err: any) => {
        this.isLoading = false;
        console.error('Erreur:', err);
        this.snackBar.open('Erreur lors du traitement automatique. Veuillez réessayer ou utiliser le mode manuel.', 'Fermer', {
          duration: 5000,
          horizontalPosition: 'end',
          verticalPosition: 'bottom'
        });
      }
    });
  }

  loadMappings(): void {
    this.importService.getMappings().subscribe({
      next: (mappings) => {
        this.savedMappings = mappings;
      },
      error: (err) => {
        console.error('Erreur chargement mappings:', err);
      }
    });
  }

  detectColumns(): void {
    if (!this.selectedFile || !this.ligneEntete) {
      return;
    }

    this.isLoading = true;
    this.importService.detectColumns(this.selectedFile, this.ligneEntete).subscribe({
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
}
