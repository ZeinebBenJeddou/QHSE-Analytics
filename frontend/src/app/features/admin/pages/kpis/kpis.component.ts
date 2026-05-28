import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AdminService } from '../../../../core/services/admin.service';
import {
  CategorieKpiResponse,
  CreateKpiRequest,
  Direction,
  KpiDeleteResponse,
  KpiResponse,
  UpdateKpiRequest,
} from '../../models/admin.models';
import { KpisResolvedData } from './kpis.resolver';
 
const seuilOrderValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const faible   = group.get('seuilFaible')?.value;
  const modere   = group.get('seuilModere')?.value;
  const critique = group.get('seuilCritique')?.value;
  if (faible == null || modere == null || critique == null) return null;
  if (faible >= modere)   return { seuilFaibleGeMod: true };
  if (modere >= critique) return { seuilModereGeCrit: true };
  return null;
};

@Component({
  selector: 'app-admin-kpis',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './kpis.component.html',
  styleUrls: ['./kpis.component.css'],
})
export class AdminKpisComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly snackBar     = inject(MatSnackBar);
  private readonly fb           = inject(FormBuilder);
  private readonly route        = inject(ActivatedRoute);
  private readonly cdr          = inject(ChangeDetectorRef);
  private readonly dialog       = inject(MatDialog);
 
  kpis:         KpiResponse[]         = [];
  inactiveKpis: KpiResponse[]         = [];
  categories:   CategorieKpiResponse[] = [];
  unitOptions = [
    { label: 'Pourcentage (%)', value: 'POURCENTAGE' as const },
    { label: 'Nombre',          value: 'NOMBRE'      as const },
    { label: 'kWh',             value: 'KWH'         as const },
    { label: 'kg',              value: 'KG'          as const },
  ];

  directionOptions: { label: string; value: Direction }[] = [
    { label: '↑ Plus c\'est élevé, mieux c\'est', value: 'HIGHER_IS_BETTER' },
    { label: '↓ Plus c\'est bas, mieux c\'est',   value: 'LOWER_IS_BETTER'  },
  ];
 
  loading        = false;
  saving         = false;
  editMode       = false;
  editingKpiId:  number | null = null;
  errorMessage   = '';
 
  kpiForm = this.fb.group({
    nom:           ['', Validators.required],
    definition:    ['', Validators.required],
    unite:         ['', Validators.required],
    categorieCode: ['', Validators.required],
    seuilFaible:   [null as number | null, [Validators.required, Validators.min(0)]],
    seuilModere:   [null as number | null, [Validators.required, Validators.min(0)]],
    seuilCritique: [null as number | null, [Validators.required, Validators.min(0)]],
    direction:     [null as Direction | null],
  }, { validators: seuilOrderValidator });

  get seuilError(): string | null {
    if (this.kpiForm.hasError('seuilFaibleGeMod'))
      return 'Le seuil faible doit être inférieur au seuil modéré.';
    if (this.kpiForm.hasError('seuilModereGeCrit'))
      return 'Le seuil modéré doit être inférieur au seuil critique.';
    return null;
  }
 
  ngOnInit(): void {
    const resolved = this.route.snapshot.data['kpis'] as KpisResolvedData | null;
    if (resolved) {
      this.kpis       = resolved.kpis;
      this.categories = resolved.categories;
    } else {
      this.loadKpis();
    }
  }

  private loadKpis(): void {
    this.loading = true;
    this.adminService.getKpis().subscribe({
      next: (kpis) => { this.kpis = kpis; },
      complete: () => { this.loading = false; },
    });
    this.adminService.getInactiveKpis().subscribe({
      next: (kpis) => { this.inactiveKpis = kpis; },
    });
    this.adminService.getKpiCategories().subscribe({
      next: (categories) => { this.categories = categories; },
    });
  }
 
  submitKpi(): void {
    if (this.kpiForm.invalid) return;
    this.saving = true;
    this.cdr.detectChanges();
    const payload = this.kpiForm.value as unknown as CreateKpiRequest;
 
    const request =
      this.editMode && this.editingKpiId
        ? this.adminService.updateKpi(this.editingKpiId, payload as UpdateKpiRequest)
        : this.adminService.createKpi(payload);
 
    request.subscribe({
      next: () => {
        this.snackBar.open(
          this.editMode ? 'KPI mis à jour.' : 'KPI créé.',
          'Fermer',
          { duration: 3000 }
        );
        this.resetForm();
        this.refreshKpis();
      },
      error: (err) => {
        const status = err?.status;
        let msg = 'Impossible d\'enregistrer le KPI.';
        if (status === 409) {
          msg = 'Un KPI avec ce nom existe déjà dans cette catégorie.';
        } else if (status === 400) {
          msg = err?.error?.message ?? 'Données invalides.';
        }
        this.snackBar.open(msg, 'Fermer', { duration: 4000 });
        this.saving = false;
        this.cdr.detectChanges();
      },
    });
  }
 
  editKpi(kpi: KpiResponse): void {
    this.editMode     = true;
    this.editingKpiId = kpi.id;
    this.kpiForm.setValue({
      nom:           kpi.nom,
      definition:    kpi.definition,
      unite:         kpi.unite,
      categorieCode: kpi.categorieCode,
      seuilFaible:   kpi.seuilFaible,
      seuilModere:   kpi.seuilModere,
      seuilCritique: kpi.seuilCritique,
      direction:     kpi.direction ?? null,
    });
  
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
 
  cancelEdit(): void {
    this.resetForm();
  }
 
  deleteKpi(kpi: KpiResponse): void {
    const ref = this.dialog.open(KpiDeleteConfirmDialog, {
      width: '420px',
      data: { nom: kpi.nom },
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      // saving=true set here (inside async callback) to avoid NG0100
      this.saving = true;
      this.cdr.detectChanges();
      this.adminService.deleteKpi(kpi.id).subscribe({
        next: (res) => {
          const msg = res.deleted
            ? 'KPI supprimé définitivement.'
            : 'KPI désactivé (données historiques conservées).';
          this.snackBar.open(msg, 'Fermer', { duration: 4000 });
          this.refreshKpis();
        },
        error: () => {
          this.snackBar.open('Impossible de supprimer le KPI.', 'Fermer', { duration: 3000 });
          this.saving = false;
          this.cdr.detectChanges();
        },
      });
    });
  }

  restoreKpi(kpi: KpiResponse): void {
    this.saving = true;
    this.cdr.detectChanges();
    this.adminService.restoreKpi(kpi.id).subscribe({
      next: () => {
        this.snackBar.open('KPI restauré.', 'Fermer', { duration: 3000 });
        this.refreshKpis();
      },
      error: () => {
        this.snackBar.open('Impossible de restaurer le KPI.', 'Fermer', { duration: 3000 });
        this.saving = false;
        this.cdr.detectChanges();
      },
    });
  }
 
  private refreshKpis(): void {
    this.adminService.getKpis().subscribe({
      next: (kpis) => {
        this.kpis = kpis;
        this.cdr.detectChanges();
      },
    });
    this.adminService.getInactiveKpis().subscribe({
      next: (kpis) => {
        this.inactiveKpis = kpis;
        this.saving = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.cdr.detectChanges();
      },
    });
  }
 
  private resetForm(): void {
    this.editMode     = false;
    this.editingKpiId = null;
    this.kpiForm.reset({
      nom: '', definition: '', unite: '', categorieCode: '',
      seuilFaible: null, seuilModere: null, seuilCritique: null,
      direction: null,
    });
  }
}

// ─── Dialog de confirmation suppression ───────────────────────────────────────

import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'kpi-delete-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <div class="confirm-dialog">
      <div class="confirm-icon">
        <mat-icon>warning_amber</mat-icon>
      </div>
      <h2 class="confirm-title">Supprimer ce KPI ?</h2>
      <p class="confirm-body">
        <strong>{{ data.nom }}</strong> sera supprimé définitivement
        s'il n'a pas de données historiques, ou désactivé dans le cas contraire.
      </p>
      <div class="confirm-actions">
        <button mat-stroked-button (click)="close(false)">Annuler</button>
        <button mat-flat-button class="confirm-delete-btn" (click)="close(true)">
          <mat-icon>delete_outline</mat-icon>
          Supprimer
        </button>
      </div>
    </div>
  `,
  styles: [`
    .confirm-dialog {
      padding: 24px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      text-align: center;
      font-family: 'DM Sans', sans-serif;
    }
    .confirm-icon mat-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: #D97706;
    }
    .confirm-title {
      margin: 0;
      font-size: 1.1rem;
      font-weight: 600;
      color: #0D1B3E;
    }
    .confirm-body {
      margin: 0;
      font-size: 0.88rem;
      color: #4A5568;
      line-height: 1.5;
    }
    .confirm-actions {
      display: flex;
      gap: 12px;
      margin-top: 8px;
      justify-content: center;
    }
    .confirm-delete-btn {
      background: #E53E3E !important;
      color: white !important;
    }
  `],
})
export class KpiDeleteConfirmDialog {
  readonly data     = inject<{ nom: string }>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<KpiDeleteConfirmDialog>);
  close(result: boolean) { this.dialogRef.close(result); }
}