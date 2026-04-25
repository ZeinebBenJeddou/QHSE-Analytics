import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterModule } from '@angular/router';
import { AdminService } from '../../../../core/services/admin.service';
import { CategorieKpiResponse, CreateKpiRequest, KpiResponse, UpdateKpiRequest } from '../../models/admin.models';

@Component({
  selector: 'app-admin-kpis',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './kpis.component.html',
  styleUrls: ['./kpis.component.css']
})
export class AdminKpisComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  kpis: KpiResponse[] = [];
  categories: CategorieKpiResponse[] = [];
  loading = false;
  saving = false;
  editMode = false;
  editingKpiId: number | null = null;
  errorMessage = '';

  kpiForm = this.fb.group({
    nom: ['', Validators.required],
    definition: ['', Validators.required],
    unite: ['', Validators.required],
    categorieCode: ['', Validators.required],
    seuilFaible: [0, [Validators.required, Validators.min(0)]],
    seuilModere: [0, [Validators.required, Validators.min(0)]],
    seuilCritique: [0, [Validators.required, Validators.min(0)]],
    ordre: [0, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    this.loadKpis();
    this.loadCategories();
  }

  loadKpis(): void {
    this.loading = true;
    this.errorMessage = '';

    this.adminService.getKpis().subscribe({
      next: (kpis) => {
        this.kpis = kpis;
      },
      error: () => {
        this.errorMessage = 'Unable to load KPI list.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  loadCategories(): void {
    this.adminService.getKpiCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: () => {
        this.errorMessage = 'Unable to load KPI categories.';
      }
    });
  }

  submitKpi(): void {
    if (this.kpiForm.invalid) {
      return;
    }

    this.saving = true;
    const payload = this.kpiForm.value as CreateKpiRequest;

    const request = this.editMode && this.editingKpiId
      ? this.adminService.updateKpi(this.editingKpiId, payload as UpdateKpiRequest)
      : this.adminService.createKpi(payload);

    request.subscribe({
      next: () => {
        this.snackBar.open(this.editMode ? 'KPI updated.' : 'KPI created.', 'Close', { duration: 3000 });
        this.resetForm();
        this.loadKpis();
      },
      error: () => {
        this.snackBar.open('Unable to save KPI.', 'Close', { duration: 3000 });
      },
      complete: () => {
        this.saving = false;
      }
    });
  }

  editKpi(kpi: KpiResponse): void {
    this.editMode = true;
    this.editingKpiId = kpi.id;
    this.kpiForm.setValue({
      nom: kpi.nom,
      definition: kpi.definition,
      unite: kpi.unite,
      categorieCode: kpi.categorieCode,
      seuilFaible: kpi.seuilFaible,
      seuilModere: kpi.seuilModere,
      seuilCritique: kpi.seuilCritique,
      ordre: kpi.ordre
    });
  }

  cancelEdit(): void {
    this.resetForm();
  }

  deleteKpi(kpi: KpiResponse): void {
    this.saving = true;
    this.adminService.deleteKpi(kpi.id).subscribe({
      next: () => {
        this.snackBar.open('KPI deleted.', 'Close', { duration: 3000 });
        this.loadKpis();
      },
      error: () => {
        this.snackBar.open('Unable to delete KPI.', 'Close', { duration: 3000 });
      },
      complete: () => {
        this.saving = false;
      }
    });
  }

  restoreKpi(kpi: KpiResponse): void {
    this.saving = true;
    this.adminService.restoreKpi(kpi.id).subscribe({
      next: () => {
        this.snackBar.open('KPI restored.', 'Close', { duration: 3000 });
        this.loadKpis();
      },
      error: () => {
        this.snackBar.open('Unable to restore KPI.', 'Close', { duration: 3000 });
      },
      complete: () => {
        this.saving = false;
      }
    });
  }

  private resetForm(): void {
    this.editMode = false;
    this.editingKpiId = null;
    this.kpiForm.reset({
      nom: '',
      definition: '',
      unite: '',
      categorieCode: '',
      seuilFaible: 0,
      seuilModere: 0,
      seuilCritique: 0,
      ordre: 0
    });
  }
}
