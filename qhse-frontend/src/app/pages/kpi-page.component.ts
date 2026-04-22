import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { KpiService } from '../shared/services/kpi.service';
import { AuthStorageService } from '../shared/services/auth-storage.service';
import { CategorieKpi, CreateKpiRequest, Kpi, UpdateKpiRequest } from '../shared/models/kpi.models';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterModule } from '@angular/router';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, MatCardModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    <section class="kpi-shell">
      <div class="page-header">
        <div>
          <p class="eyebrow">Gestion des KPI</p>
          <h1>Catalogue des indicateurs</h1>
          <p>Consultez les KPI actifs et gérez les ressources métier selon votre rôle.</p>
        </div>
        <div class="header-actions">
          <a mat-flat-button color="primary" routerLink="/dashboard">Retour au tableau de bord</a>
        </div>
      </div>

      <mat-card class="filter-card">
        <div class="filter-row">
          <mat-form-field appearance="fill" class="filter-field">
            <mat-label>Filtrer par catégorie</mat-label>
            <mat-select [value]="selectedCategory" (selectionChange)="loadActiveKpis($event.value)">
              <mat-option [value]="''">Toutes les catégories</mat-option>
              <mat-option *ngFor="let category of categories" [value]="category.code">
                {{ category.libelle }}
              </mat-option>
            </mat-select>
          </mat-form-field>
          <div class="summary-block">
            <span>{{ activeKpis.length }} KPI actifs</span>
            <span *ngIf="authStorage.isAdmin()">{{ inactiveKpis.length }} KPI inactifs</span>
          </div>
        </div>
      </mat-card>

      <mat-card class="form-card" *ngIf="authStorage.isAdmin()">
        <h2>{{ isEditing ? 'Modifier un KPI' : 'Créer un nouveau KPI' }}</h2>
        <form [formGroup]="kpiForm" (ngSubmit)="submit()">
          <div class="form-grid">
            <mat-form-field appearance="fill">
              <mat-label>Nom du KPI</mat-label>
              <input matInput formControlName="nom" />
            </mat-form-field>

            <mat-form-field appearance="fill" class="full-span">
              <mat-label>Définition</mat-label>
              <input matInput formControlName="definition" />
            </mat-form-field>

            <mat-form-field appearance="fill">
              <mat-label>Unité</mat-label>
              <mat-select formControlName="unite">
                <mat-option value="POURCENTAGE">Pourcentage</mat-option>
                <mat-option value="NOMBRE">Nombre</mat-option>
                <mat-option value="KWH">kWh</mat-option>
                <mat-option value="KG">Kg</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="fill">
              <mat-label>Catégorie</mat-label>
              <mat-select formControlName="categorieCode">
                <mat-option *ngFor="let category of categories" [value]="category.code">
                  {{ category.libelle }}
                </mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="fill">
              <mat-label>Seuil faible</mat-label>
              <input matInput type="number" formControlName="seuilFaible" />
            </mat-form-field>

            <mat-form-field appearance="fill">
              <mat-label>Seuil modéré</mat-label>
              <input matInput type="number" formControlName="seuilModere" />
            </mat-form-field>

            <mat-form-field appearance="fill">
              <mat-label>Seuil critique</mat-label>
              <input matInput type="number" formControlName="seuilCritique" />
            </mat-form-field>

            <mat-form-field appearance="fill">
              <mat-label>Ordre</mat-label>
              <input matInput type="number" formControlName="ordre" />
            </mat-form-field>
          </div>

          <div class="form-actions">
            <button mat-flat-button color="primary" type="submit" [disabled]="kpiForm.invalid">
              {{ isEditing ? 'Mettre à jour' : 'Créer le KPI' }}
            </button>
            <button mat-button type="button" *ngIf="isEditing" (click)="resetForm()">Annuler</button>
          </div>
        </form>
      </mat-card>

      <section class="kpi-list-section">
        <h2>KPI actifs</h2>
        <div class="kpi-list">
          <mat-card class="kpi-card" *ngFor="let kpi of activeKpis; trackBy: trackByKpi">
            <div class="kpi-header">
              <div>
                <h3>{{ kpi.nom }}</h3>
                <p class="meta">{{ kpi.categorieLibelle || kpi.categorieCode }} • {{ kpi.unite }}</p>
              </div>
              <div class="card-actions" *ngIf="authStorage.isAdmin()">
                <button mat-icon-button color="primary" aria-label="Modifier" (click)="editKpi(kpi)">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button color="warn" aria-label="Supprimer" (click)="deleteKpi(kpi.id)">
                  <mat-icon>delete</mat-icon>
                </button>
              </div>
            </div>
            <p class="kpi-definition">{{ kpi.definition }}</p>
            <div class="kpi-meta-row">
              <span>Seuil faible : {{ kpi.seuilFaible }}</span>
              <span>Seuil modéré : {{ kpi.seuilModere }}</span>
              <span>Seuil critique : {{ kpi.seuilCritique }}</span>
              <span>Ordre : {{ kpi.ordre }}</span>
            </div>
          </mat-card>
          <p class="empty-message" *ngIf="activeKpis.length === 0">Aucun KPI actif trouvé.</p>
        </div>
      </section>

      <section class="kpi-list-section" *ngIf="authStorage.isAdmin()">
        <h2>KPI inactifs</h2>
        <div class="kpi-list">
          <mat-card class="kpi-card" *ngFor="let kpi of inactiveKpis; trackBy: trackByKpi">
            <div class="kpi-header">
              <div>
                <h3>{{ kpi.nom }}</h3>
                <p class="meta">{{ kpi.categorieLibelle || kpi.categorieCode }} • {{ kpi.unite }}</p>
              </div>
              <div class="card-actions">
                <button mat-icon-button color="accent" aria-label="Restaurer" (click)="restoreKpi(kpi.id)">
                  <mat-icon>restore</mat-icon>
                </button>
              </div>
            </div>
            <p class="kpi-definition">{{ kpi.definition }}</p>
            <div class="kpi-meta-row">
              <span>Seuil faible : {{ kpi.seuilFaible }}</span>
              <span>Seuil modéré : {{ kpi.seuilModere }}</span>
              <span>Seuil critique : {{ kpi.seuilCritique }}</span>
              <span>Ordre : {{ kpi.ordre }}</span>
            </div>
          </mat-card>
          <p class="empty-message" *ngIf="inactiveKpis.length === 0">Aucun KPI inactif trouvé.</p>
        </div>
      </section>
    </section>
  `,
  styles: [`
    .kpi-shell {
      max-width: 1200px;
      margin: 0 auto;
      padding: 1rem;
      display: grid;
      gap: 1.5rem;
    }
    .page-header {
      display: flex;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 1rem;
      align-items: center;
    }
    .eyebrow {
      margin: 0 0 0.5rem;
      color: #0b4a94;
      text-transform: uppercase;
      letter-spacing: 0.1em;
      font-size: 0.85rem;
    }
    h1 {
      margin: 0;
      font-size: clamp(2rem, 2.5vw, 2.5rem);
      color: #0f172a;
    }
    .header-actions {
      display: flex;
      gap: 1rem;
    }
    .filter-card,
    .form-card,
    .kpi-card {
      padding: 1.5rem;
      border-radius: 1rem;
      box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
    }
    .filter-row {
      display: grid;
      gap: 1rem;
      grid-template-columns: 1fr auto;
      align-items: center;
    }
    .filter-field {
      min-width: 280px;
    }
    .summary-block {
      display: flex;
      align-items: center;
      gap: 1rem;
      color: #334155;
      font-weight: 600;
    }
    .form-grid {
      display: grid;
      gap: 1rem;
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
    .full-span {
      grid-column: span 3;
    }
    .form-actions {
      display: flex;
      gap: 1rem;
      justify-content: flex-start;
      margin-top: 1rem;
    }
    .kpi-list-section {
      display: grid;
      gap: 1rem;
    }
    .kpi-list {
      display: grid;
      gap: 1rem;
    }
    .kpi-card {
      display: grid;
      gap: 1rem;
    }
    .kpi-header {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      align-items: flex-start;
    }
    .kpi-header h3 {
      margin: 0;
      font-size: 1.2rem;
      color: #0f172a;
    }
    .meta {
      margin: 0.4rem 0 0;
      color: #475569;
      font-size: 0.95rem;
    }
    .card-actions {
      display: flex;
      gap: 0.5rem;
    }
    .kpi-definition {
      margin: 0;
      color: #334155;
      line-height: 1.65;
    }
    .kpi-meta-row {
      display: grid;
      gap: 1rem;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      color: #475569;
      font-size: 0.95rem;
    }
    .empty-message {
      margin: 0;
      color: #64748b;
      font-style: italic;
    }
    @media (max-width: 900px) {
      .form-grid {
        grid-template-columns: 1fr;
      }
      .filter-row {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class KpiPage implements OnInit {
  categories: CategorieKpi[] = [];
  activeKpis: Kpi[] = [];
  inactiveKpis: Kpi[] = [];
  selectedCategory = '';
  isEditing = false;
  editingId: number | null = null;

  kpiForm = new FormGroup({
    nom: new FormControl('', Validators.required),
    definition: new FormControl('', Validators.required),
    unite: new FormControl('POURCENTAGE', Validators.required),
    categorieCode: new FormControl('', Validators.required),
    seuilFaible: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    seuilModere: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    seuilCritique: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    ordre: new FormControl<number | null>(null, [Validators.required, Validators.min(1)])
  });

  constructor(public authStorage: AuthStorageService, private kpiService: KpiService) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadActiveKpis();
    if (this.authStorage.isAdmin()) {
      this.loadInactiveKpis();
    }
  }

  loadCategories(): void {
    this.kpiService.getAllCategories().subscribe((categories) => {
      this.categories = categories;
    });
  }

  loadActiveKpis(categoryCode: string = this.selectedCategory): void {
    this.selectedCategory = categoryCode ?? '';
    this.kpiService.getKpis(this.selectedCategory || undefined).subscribe((kpis) => {
      this.activeKpis = kpis;
    });
  }

  loadInactiveKpis(): void {
    this.kpiService.getInactiveKpis().subscribe((kpis) => {
      this.inactiveKpis = kpis;
    });
  }

  editKpi(kpi: Kpi): void {
    this.isEditing = true;
    this.editingId = kpi.id;
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
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  resetForm(): void {
    this.isEditing = false;
    this.editingId = null;
    this.kpiForm.reset({ unite: 'POURCENTAGE' });
  }

  submit(): void {
    if (this.kpiForm.invalid) {
      return;
    }

    const payload: CreateKpiRequest | UpdateKpiRequest = this.kpiForm.value as CreateKpiRequest;
    if (this.isEditing && this.editingId != null) {
      this.kpiService.updateKpi(this.editingId, payload).subscribe(() => {
        this.loadActiveKpis();
        this.loadInactiveKpis();
        this.resetForm();
      });
    } else {
      this.kpiService.createKpi(payload as CreateKpiRequest).subscribe(() => {
        this.loadActiveKpis();
        this.kpiForm.reset({ unite: 'POURCENTAGE' });
      });
    }
  }

  deleteKpi(id: number): void {
    this.kpiService.deleteKpi(id).subscribe(() => {
      this.loadActiveKpis();
      this.loadInactiveKpis();
    });
  }

  restoreKpi(id: number): void {
    this.kpiService.restoreKpi(id).subscribe(() => {
      this.loadActiveKpis();
      this.loadInactiveKpis();
    });
  }

  trackByKpi(_: number, kpi: Kpi): number {
    return kpi.id;
  }
}
