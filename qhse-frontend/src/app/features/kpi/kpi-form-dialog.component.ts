import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { KpiService, CategorieKpiResponse } from '../../core/services/kpi.service';
import { KpiResponse } from '../../shared/models/kpi-response';
import { CreateKpiRequest } from '../../shared/models/create-kpi-request';
import { UpdateKpiRequest } from '../../shared/models/update-kpi-request';
import { UniteKpi } from '../../shared/enums/unite-kpi.enum';

export interface KpiFormDialogData {
  mode: 'create' | 'edit' | 'view';
  kpi?: KpiResponse;
}

@Component({
  selector: 'app-kpi-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>{{ title }}</h2>
    <mat-dialog-content [formGroup]="form" class="dialog-content">
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Nom du KPI</mat-label>
        <input matInput formControlName="nom" [readonly]="isView" />
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Définition</mat-label>
        <input matInput formControlName="definition" [readonly]="isView" />
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Unité</mat-label>
        <mat-select formControlName="unite" [disabled]="isView">
          <mat-option [value]="UniteKpi.POURCENTAGE">Pourcentage</mat-option>
          <mat-option [value]="UniteKpi.NOMBRE">Nombre</mat-option>
          <mat-option [value]="UniteKpi.KWH">kWh</mat-option>
          <mat-option [value]="UniteKpi.KG">kg</mat-option>
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Catégorie</mat-label>
        <mat-select formControlName="categorieCode" [disabled]="isView">
          <mat-option *ngFor="let cat of categories" [value]="cat.code">{{ cat.libelle }}</mat-option>
        </mat-select>
      </mat-form-field>

      <div class="threshold-row">
        <mat-form-field appearance="outline" class="threshold-field">
          <mat-label>Seuil faible</mat-label>
          <input matInput type="number" formControlName="seuilFaible" [readonly]="isView" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="threshold-field">
          <mat-label>Seuil modéré</mat-label>
          <input matInput type="number" formControlName="seuilModere" [readonly]="isView" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="threshold-field">
          <mat-label>Seuil critique</mat-label>
          <input matInput type="number" formControlName="seuilCritique" [readonly]="isView" />
        </mat-form-field>
      </div>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Ordre</mat-label>
        <input matInput type="number" formControlName="ordre" [readonly]="isView" />
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()">Annuler</button>
      <button mat-flat-button color="primary" (click)="onSave()" *ngIf="!isView" [disabled]="form.invalid">
        {{ actionLabel }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-content {
        display: grid;
        gap: 1rem;
        min-width: 360px;
      }

      .full-width {
        width: 100%;
      }

      .threshold-row {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 1rem;
      }

      .threshold-field {
        width: 100%;
      }
    `
  ]
})
export class KpiFormDialogComponent implements OnInit {
  form: FormGroup;
  categories: CategorieKpiResponse[] = [];
  isView = false;
  title = 'Nouveau KPI';
  actionLabel = 'Créer';
  UniteKpi = UniteKpi;

  constructor(
    private dialogRef: MatDialogRef<KpiFormDialogComponent>,
    private formBuilder: FormBuilder,
    private kpiService: KpiService,
    @Inject(MAT_DIALOG_DATA) public data: KpiFormDialogData
  ) {
    this.form = this.formBuilder.group({
      nom: ['', Validators.required],
      definition: ['', Validators.required],
      unite: [UniteKpi.NOMBRE, Validators.required],
      categorieCode: ['', Validators.required],
      seuilFaible: [0, Validators.required],
      seuilModere: [0, Validators.required],
      seuilCritique: [0, Validators.required],
      ordre: [0, Validators.required]
    });
  }

  ngOnInit(): void {
    this.isView = this.data.mode === 'view';
    this.title = this.data.mode === 'edit' ? 'Modifier le KPI' : this.data.mode === 'view' ? 'Détails du KPI' : 'Nouveau KPI';
    this.actionLabel = this.data.mode === 'edit' ? 'Mettre à jour' : 'Créer';

    if (this.data.kpi) {
      this.form.patchValue({
        nom: this.data.kpi.nom,
        definition: this.data.kpi.definition,
        unite: this.data.kpi.unite,
        categorieCode: this.data.kpi.categorieCode,
        seuilFaible: this.data.kpi.seuilFaible,
        seuilModere: this.data.kpi.seuilModere,
        seuilCritique: this.data.kpi.seuilCritique,
        ordre: this.data.kpi.ordre
      });
    }

    this.kpiService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (err) => {
        console.error('Erreur chargement catégories KPI:', err);
      }
    });
  }

  onSave(): void {
    if (this.form.invalid) {
      return;
    }

    const formValue = this.form.value;

    if (this.data.mode === 'edit' && this.data.kpi) {
      const request: UpdateKpiRequest = {
        nom: formValue.nom,
        definition: formValue.definition,
        unite: formValue.unite,
        categorieCode: formValue.categorieCode,
        seuilFaible: formValue.seuilFaible,
        seuilModere: formValue.seuilModere,
        seuilCritique: formValue.seuilCritique,
        ordre: formValue.ordre
      };
      this.kpiService.updateKpi(this.data.kpi.id, request).subscribe({
        next: (updated) => this.dialogRef.close(updated),
        error: (err) => console.error('Erreur mise à jour KPI:', err)
      });
      return;
    }

    const request: CreateKpiRequest = {
      nom: formValue.nom,
      definition: formValue.definition,
      unite: formValue.unite,
      categorieCode: formValue.categorieCode,
      seuilFaible: formValue.seuilFaible,
      seuilModere: formValue.seuilModere,
      seuilCritique: formValue.seuilCritique,
      ordre: formValue.ordre
    };

    this.kpiService.createKpi(request).subscribe({
      next: (created) => this.dialogRef.close(created),
      error: (err) => console.error('Erreur création KPI:', err)
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
