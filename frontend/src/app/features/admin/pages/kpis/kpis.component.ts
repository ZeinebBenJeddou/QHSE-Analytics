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
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AdminService } from '../../../../core/services/admin.service';
import {
  CategorieKpiResponse,
  CreateKpiRequest,
  KpiResponse,
  UpdateKpiRequest,
} from '../../models/admin.models';
import { KpisResolvedData } from './kpis.resolver';
 
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
 
  kpis:       KpiResponse[]         = [];
  categories: CategorieKpiResponse[] = [];
  unitOptions = [
    { label: 'Pourcentage (%)', value: 'POURCENTAGE' as const },
    { label: 'Nombre', value: 'NOMBRE' as const },
    { label: 'kWh', value: 'KWH' as const },
    { label: 'kg', value: 'KG' as const },
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
    seuilFaible:   [null as number | null, [Validators.required, Validators.min(1)]],
    seuilModere:   [null as number | null, [Validators.required, Validators.min(1)]],
    seuilCritique: [null as number | null, [Validators.required, Validators.min(1)]],
    ordre:         [null as number | null, [Validators.required, Validators.min(1)]],
  });
 
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
    this.adminService.getKpiCategories().subscribe({
      next: (categories) => { this.categories = categories; },
    });
  }
 
  submitKpi(): void {
    if (this.kpiForm.invalid) return;
    this.saving = true;
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
      error: () => {
        this.snackBar.open('Impossible d\'enregistrer le KPI.', 'Fermer', { duration: 3000 });
        this.saving = false;
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
      ordre:         kpi.ordre,
    });
  
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
 
  cancelEdit(): void {
    this.resetForm();
  }
 
  deleteKpi(kpi: KpiResponse): void {
    this.saving = true;
    this.adminService.deleteKpi(kpi.id).subscribe({
      next: () => {
        this.snackBar.open('KPI supprimé.', 'Fermer', { duration: 3000 });
        this.refreshKpis();
      },
      error: () => {
        this.snackBar.open('Impossible de supprimer le KPI.', 'Fermer', { duration: 3000 });
        this.saving = false;
      },
    });
  }
 
  restoreKpi(kpi: KpiResponse): void {
    this.saving = true;
    this.adminService.restoreKpi(kpi.id).subscribe({
      next: () => {
        this.snackBar.open('KPI restauré.', 'Fermer', { duration: 3000 });
        this.refreshKpis();
      },
      error: () => {
        this.snackBar.open('Impossible de restaurer le KPI.', 'Fermer', { duration: 3000 });
        this.saving = false;
      },
    });
  }
 
  private refreshKpis(): void {
    this.adminService.getKpis().subscribe({
      next: (kpis) => { this.kpis = kpis; },
      complete: () => { this.saving = false; },
    });
  }
 
  private resetForm(): void {
    this.editMode     = false;
    this.editingKpiId = null;
    this.kpiForm.reset({
      nom: '', definition: '', unite: '', categorieCode: '',
      seuilFaible: null, seuilModere: null, seuilCritique: null, ordre: null,
    });
  }
}