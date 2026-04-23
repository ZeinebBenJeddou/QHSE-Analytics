import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { StagingDonneeResponse } from '../../shared/models/staging-donnee-response';
import { CorrectionRequest } from '../../shared/models/correction-request';

@Component({
  selector: 'app-correction-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Correction manuelle</h2>
    <mat-dialog-content [formGroup]="form" class="dialog-content">
      <mat-card>
        <mat-card-title>{{ data.kpiNom }}</mat-card-title>
        <mat-card-subtitle>Catégorie : {{ data.categorieCode }}</mat-card-subtitle>
        <div class="field-row">
          <div>
            <strong>Valeur brute N-1 :</strong> {{ data.valeurBruteN1 || '-' }}
          </div>
          <div>
            <strong>Valeur brute N :</strong> {{ data.valeurBruteN || '-' }}
          </div>
        </div>
      </mat-card>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Valeur N-1 corrigée</mat-label>
        <input matInput type="number" formControlName="valeurN1" placeholder="Valeur N-1" />
        <mat-icon matSuffix>edit</mat-icon>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Valeur N corrigée</mat-label>
        <input matInput type="number" formControlName="valeurN" placeholder="Valeur N" />
        <mat-icon matSuffix>edit</mat-icon>
      </mat-form-field>

      <div class="note" *ngIf="data.noteNettoyage">
        <strong>Note actuelle :</strong> {{ data.noteNettoyage }}
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()">Annuler</button>
      <button mat-flat-button color="primary" (click)="onSave()" [disabled]="form.invalid">Valider</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-content {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        min-width: 320px;
      }

      .full-width {
        width: 100%;
      }

      .field-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1rem;
        margin-top: 0.5rem;
      }

      .note {
        font-size: 0.95rem;
        color: rgba(0, 0, 0, 0.7);
      }
    `
  ]
})
export class CorrectionDialogComponent {
  form: FormGroup;

  constructor(
    private dialogRef: MatDialogRef<CorrectionDialogComponent>,
    private formBuilder: FormBuilder,
    @Inject(MAT_DIALOG_DATA) public data: StagingDonneeResponse
  ) {
    this.form = this.formBuilder.group({
      valeurN1: [data.valeurN1 ?? null, [Validators.required]],
      valeurN: [data.valeurN ?? null, [Validators.required]]
    });
  }

  onSave(): void {
    if (!this.form.valid) {
      return;
    }

    const request: CorrectionRequest = {
      stagingDonneeId: this.data.id,
      valeurN1: this.form.value.valeurN1,
      valeurN: this.form.value.valeurN
    };

    this.dialogRef.close(request);
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
