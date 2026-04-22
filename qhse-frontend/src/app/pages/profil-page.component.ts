import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProfilService } from '../shared/services/profil.service';
import { AuthStorageService } from '../shared/services/auth-storage.service';
import { ChangePasswordRequest, Profil, UpdateProfilRequest } from '../shared/models/profil.models';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterModule } from '@angular/router';

@Component({
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTabsModule
  ],
  template: `
    <section class="profil-shell">
      <div class="page-header">
        <div>
          <p class="eyebrow">Mon profil</p>
          <h1>{{ authStorage.userName || 'Utilisateur' }}</h1>
          <p>Consultez et modifiez vos informations personnelles.</p>
        </div>
        <div class="header-actions">
          <a mat-flat-button color="primary" routerLink="/dashboard">Retour au tableau de bord</a>
        </div>
      </div>

      <mat-card class="profil-card">
        <mat-tab-group>
          <mat-tab label="Informations personnelles">
            <div class="tab-content">
              <form [formGroup]="profilForm" (ngSubmit)="updateProfil()">
                <div class="form-grid">
                  <mat-form-field appearance="fill">
                    <mat-label>Nom</mat-label>
                    <input matInput formControlName="nom" />
                  </mat-form-field>

                  <mat-form-field appearance="fill">
                    <mat-label>Prénom</mat-label>
                    <input matInput formControlName="prenom" />
                  </mat-form-field>

                  <mat-form-field appearance="fill" class="full-span">
                    <mat-label>Email</mat-label>
                    <input matInput type="email" formControlName="email" [readonly]="authStorage.userRole !== 'ADMIN'" />
                    <mat-hint *ngIf="authStorage.userRole !== 'ADMIN'">L'email ne peut pas être modifié</mat-hint>
                  </mat-form-field>

                  <mat-form-field appearance="fill">
                    <mat-label>Date de création</mat-label>
                    <input matInput [value]="profil?.createdAt | date:'dd/MM/yyyy HH:mm'" readonly />
                  </mat-form-field>
                </div>

                <div class="form-actions">
                  <button mat-flat-button color="primary" type="submit" [disabled]="profilForm.invalid || profilForm.pristine">
                    Mettre à jour
                  </button>
                  <button mat-button type="button" (click)="resetForm()">Annuler</button>
                </div>
              </form>
            </div>
          </mat-tab>

          <mat-tab label="Changer le mot de passe">
            <div class="tab-content">
              <form [formGroup]="passwordForm" (ngSubmit)="changePassword()">
                <div class="form-grid">
                  <mat-form-field appearance="fill" class="full-span">
                    <mat-label>Ancien mot de passe</mat-label>
                    <input matInput type="password" formControlName="ancienPassword" />
                  </mat-form-field>

                  <mat-form-field appearance="fill" class="full-span">
                    <mat-label>Nouveau mot de passe</mat-label>
                    <input matInput type="password" formControlName="nouveauPassword" />
                    <mat-hint>Au moins 8 caractères avec majuscule, minuscule, chiffre et caractère spécial</mat-hint>
                  </mat-form-field>

                  <mat-form-field appearance="fill" class="full-span">
                    <mat-label>Confirmer le nouveau mot de passe</mat-label>
                    <input matInput type="password" formControlName="confirmPassword" />
                    <mat-error *ngIf="passwordForm.errors?.['passwordMismatch'] && passwordForm.get('confirmPassword')?.touched">
                      Les mots de passe ne correspondent pas
                    </mat-error>
                  </mat-form-field>
                </div>

                <div class="form-actions">
                  <button mat-flat-button color="primary" type="submit" [disabled]="passwordForm.invalid">
                    Changer le mot de passe
                  </button>
                </div>
              </form>
            </div>
          </mat-tab>
        </mat-tab-group>
      </mat-card>
    </section>
  `,
  styles: [`
    .profil-shell {
      max-width: 800px;
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
    .profil-card {
      padding: 0;
    }
    .tab-content {
      padding: 1.5rem;
    }
    .form-grid {
      display: grid;
      gap: 1rem;
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .full-span {
      grid-column: span 2;
    }
    .form-actions {
      display: flex;
      gap: 1rem;
      justify-content: flex-start;
      margin-top: 1.5rem;
    }
    @media (max-width: 600px) {
      .form-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ProfilPage implements OnInit {
  profil: Profil | null = null;

  profilForm = new FormGroup({
    nom: new FormControl('', Validators.required),
    prenom: new FormControl('', Validators.required),
    email: new FormControl('', [Validators.required, Validators.email])
  });

  passwordForm = new FormGroup({
    ancienPassword: new FormControl('', Validators.required),
    nouveauPassword: new FormControl('', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/)
    ]),
    confirmPassword: new FormControl('', Validators.required)
  });

  constructor(
    public authStorage: AuthStorageService,
    private profilService: ProfilService
  ) {}

  ngOnInit(): void {
    this.loadProfil();
    this.passwordForm.setValidators(this.passwordMatchValidator);
  }

  loadProfil(): void {
    this.profilService.getProfil().subscribe((profil) => {
      this.profil = profil;
      this.profilForm.patchValue({
        nom: profil.nom,
        prenom: profil.prenom,
        email: profil.email
      });

      // Désactiver l'email si l'utilisateur n'est pas admin
      if (this.authStorage.userRole !== 'ADMIN') {
        this.profilForm.get('email')?.disable();
      } else {
        this.profilForm.get('email')?.enable();
      }
    });
  }

  updateProfil(): void {
    if (this.profilForm.invalid) {
      return;
    }

    const request: UpdateProfilRequest = {
      nom: this.profilForm.value.nom!,
      prenom: this.profilForm.value.prenom!,
      ...(this.authStorage.userRole === 'ADMIN' && { email: this.profilForm.value.email! })
    };

    this.profilService.updateProfil(request).subscribe(() => {
      this.loadProfil();
      this.authStorage.setAuth({
        accessToken: this.authStorage.accessToken!,
        refreshToken: this.authStorage.refreshToken!,
        email: this.authStorage.userRole === 'ADMIN' ? this.profilForm.value.email! : this.profil!.email,
        nom: request.nom,
        prenom: request.prenom,
        role: this.profil!.role
      });
      alert('Profil mis à jour avec succès.');
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      return;
    }

    const request: ChangePasswordRequest = this.passwordForm.value as ChangePasswordRequest;

    this.profilService.changePassword(request).subscribe(() => {
      this.passwordForm.reset();
      alert('Mot de passe changé avec succès.');
    }, (error) => {
      if (error.status === 400) {
        alert('L\'ancien mot de passe est incorrect.');
      } else {
        alert('Erreur lors du changement de mot de passe.');
      }
    });
  }

  resetForm(): void {
    if (this.profil) {
      this.profilForm.patchValue({
        nom: this.profil.nom,
        prenom: this.profil.prenom,
        email: this.profil.email
      });
    }
  }

  private passwordMatchValidator(control: any): { [key: string]: any } | null {
    const nouveauPassword = control.get('nouveauPassword');
    const confirmPassword = control.get('confirmPassword');

    if (nouveauPassword && confirmPassword && nouveauPassword.value !== confirmPassword.value) {
      return { passwordMismatch: true };
    }
    return null;
  }
}
