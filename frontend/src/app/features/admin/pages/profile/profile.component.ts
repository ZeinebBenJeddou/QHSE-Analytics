import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router, RouterModule } from '@angular/router';
import { AdminService } from '../../../../core/services/admin.service';
import { ChangePasswordRequest, ProfileResponse, UpdateProfilRequest } from '../../models/admin.models';
import { TokenService } from '../../../../core/services/token.service';

@Component({
  selector: 'app-admin-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="admin-profile-shell">
      <div class="admin-header">
        <div>
          <h1>Mon profil</h1>
          <p>Informations de compte et rôle administrateur.</p>
        </div>
        <button mat-stroked-button color="primary" routerLink="/admin">Retour</button>
      </div>

      <div *ngIf="loading" class="loading-state">
        <mat-progress-spinner diameter="48" mode="indeterminate"></mat-progress-spinner>
        <span>Chargement des informations...</span>
      </div>

      <mat-card *ngIf="!loading && profile" class="profile-card">
        <h2>{{ profile.nom }} {{ profile.prenom }}</h2>
        <p><strong>Email:</strong> {{ profile.email }}</p>
        <p><strong>Rôle:</strong> {{ profile.role }}</p>
        <p><strong>Créé le:</strong> {{ profile.createdAt | date:'medium' }}</p>
      </mat-card>

      <mat-card *ngIf="!loading && profile" class="edit-card">
        <h2>Modifier le profil</h2>
        <form [formGroup]="profileForm" (ngSubmit)="saveProfile()" class="form-grid">
          <mat-form-field appearance="fill">
            <mat-label>Nom</mat-label>
            <input matInput formControlName="nom" />
          </mat-form-field>
          <mat-form-field appearance="fill">
            <mat-label>Prénom</mat-label>
            <input matInput formControlName="prenom" />
          </mat-form-field>
          <div class="form-actions">
            <button mat-flat-button color="primary" type="submit" [disabled]="profileForm.invalid || savingProfile">
              {{ savingProfile ? 'Enregistrement…' : 'Enregistrer les modifications' }}
            </button>
          </div>
        </form>
      </mat-card>

      <mat-card *ngIf="!loading && profile" class="edit-card">
        <h2>Changer le mot de passe</h2>
        <form [formGroup]="passwordForm" (ngSubmit)="changePassword()" class="form-grid">
          <mat-form-field appearance="fill">
            <mat-label>Ancien mot de passe</mat-label>
            <input matInput type="password" formControlName="ancienPassword" />
          </mat-form-field>
          <mat-form-field appearance="fill">
            <mat-label>Nouveau mot de passe</mat-label>
            <input matInput type="password" formControlName="nouveauPassword" />
          </mat-form-field>
          <mat-form-field appearance="fill">
            <mat-label>Confirmer le mot de passe</mat-label>
            <input matInput type="password" formControlName="confirmPassword" />
          </mat-form-field>
          <div class="form-actions">
            <button mat-flat-button color="accent" type="submit" [disabled]="passwordForm.invalid || savingPassword">
              {{ savingPassword ? 'Modification…' : 'Changer le mot de passe' }}
            </button>
          </div>
        </form>
      </mat-card>

      <div *ngIf="!loading && errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
    </div>
  `,
  styles: [
    '.admin-profile-shell { display: grid; gap: 20px; padding: 24px; }',
    '.admin-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }',
    '.profile-card, .edit-card { padding: 24px; }',
    '.form-grid { display: grid; gap: 16px; }',
    '.form-actions { display: flex; justify-content: flex-end; }',
    '.loading-state, .error-message { display: flex; align-items: center; gap: 12px; padding: 16px; }'
  ]
})
export class AdminProfileComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly tokenService = inject(TokenService);

  profile: ProfileResponse | null = null;
  loading = false;
  savingProfile = false;
  savingPassword = false;
  errorMessage = '';

  profileForm = this.fb.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required]
  });

  passwordForm = this.fb.group({
    ancienPassword: ['', Validators.required],
    nouveauPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.errorMessage = '';

    this.adminService.getCurrentProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.profileForm.setValue({ nom: profile.nom, prenom: profile.prenom });
      },
      error: () => {
        this.errorMessage = 'Impossible de charger le profil.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      return;
    }

    this.savingProfile = true;
    const payload = this.profileForm.value as UpdateProfilRequest;

    this.adminService.updateProfile(payload).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.snackBar.open('Profil mis à jour avec succès.', 'Fermer', { duration: 4000 });
      },
      error: () => {
        this.snackBar.open('Impossible de mettre à jour le profil.', 'Fermer', { duration: 4000 });
      },
      complete: () => {
        this.savingProfile = false;
      }
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      return;
    }

    const payload = this.passwordForm.value as ChangePasswordRequest;
    this.savingPassword = true;

    this.adminService.changePassword(payload).subscribe({
      next: (response) => {
        this.snackBar.open(response.message, 'Fermer', { duration: 5000 });
        this.tokenService.removeToken();
        this.router.navigate(['/auth/login']);
      },
      error: (err: any) => {
        const message = err.error?.message || 'Impossible de changer le mot de passe.';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      },
      complete: () => {
        this.savingPassword = false;
      }
    });
  }
}
