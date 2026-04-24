import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatStepperModule, MatStepper } from '@angular/material/stepper';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ImportService } from '../../core/services/import.service';

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
    MatSnackBarModule,
    MatToolbarModule
  ],
  template: `
    <div class="import-page-container">
      <mat-toolbar color="primary">
        <h1 class="import-title">Import de données</h1>
      </mat-toolbar>

      <div class="import-content">
        <mat-stepper [linear]="true" #stepper>
          <mat-step [completed]="true">
            <ng-template matStepLabel>Template officiel</ng-template>
            <div class="step-content">
              <h2>Import via template officiel</h2>
              <p>Téléchargez le template officiel, remplissez-le puis chargez-le pour un traitement automatique direct.</p>
              <button mat-raised-button color="primary" (click)="downloadTemplate()" [disabled]="isLoading">
                <mat-icon>download</mat-icon>
                Télécharger le template
              </button>
              <button mat-raised-button matStepperNext [disabled]="isLoading">Suivant</button>
            </div>
          </mat-step>

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
              </form>

              <div class="step-actions">
                <button mat-raised-button matStepperPrevious>Précédent</button>
                <button mat-raised-button color="primary" (click)="uploadAndProcessFile(stepper)" [disabled]="!uploadForm.valid || !selectedFile || isLoading">
                  <mat-icon *ngIf="isLoading">pending</mat-icon>
                  {{ isLoading ? 'Traitement en cours...' : 'Télécharger et traiter' }}
                </button>
              </div>
            </div>
          </mat-step>

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

  uploadForm: FormGroup;
  selectedFile: File | null = null;
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
    this.uploadForm = this.formBuilder.group({
      periodeN1: ['', Validators.required],
      periodeN: ['', Validators.required]
    });
  }

  ngOnInit(): void {}

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
        this.snackBar.open('Erreur lors du téléchargement du template.', 'Fermer', { duration: 5000 });
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
    if (!this.uploadForm.valid || !this.selectedFile) return;

    this.isLoading = true;
    const { periodeN1, periodeN } = this.uploadForm.value;

    this.importService.uploadAndProcess(
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
        this.snackBar.open('Erreur lors du traitement automatique. Veuillez réessayer.', 'Fermer', {
          duration: 5000,
          horizontalPosition: 'end',
          verticalPosition: 'bottom'
        });
      }
    });
  }
}
