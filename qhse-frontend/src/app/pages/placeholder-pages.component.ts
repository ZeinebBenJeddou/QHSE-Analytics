import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { ProfilService } from '../core/services/profil.service';
import { ProfilResponse } from '../shared/models/profil-response';
import { UpdateProfilRequest } from '../shared/models/update-profil-request';
import { ChangePasswordRequest } from '../shared/models/change-password-request';
import { AdminUserService } from '../core/services/admin-user.service';
import { UserResponse } from '../shared/models/user-response';
import { CreateAnalysteRequest } from '../shared/models/create-analyste-request';
import { UpdateUserRequest } from '../shared/models/update-user-request';
import { UserListResponse } from '../shared/models/user-list-response';

@Component({
  selector: 'app-verify-page',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: '<mat-card><mat-card-content>Vérification...</mat-card-content></mat-card>'
})
export class VerifyPage {}

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: '<mat-card><mat-card-content>Inscription...</mat-card-content></mat-card>'
})
export class RegisterPage {}

@Component({
  selector: 'app-verify-otp-page',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: '<mat-card><mat-card-content>Vérification OTP...</mat-card-content></mat-card>'
})
export class VerifyOtpPage {}

@Component({
  selector: 'app-forgot-password-page',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: '<mat-card><mat-card-content>Mot de passe oublié...</mat-card-content></mat-card>'
})
export class ForgotPasswordPage {}

@Component({
  selector: 'app-reset-password-page',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: '<mat-card><mat-card-content>Réinitialisation...</mat-card-content></mat-card>'
})
export class ResetPasswordPage {}

@Component({
  selector: 'app-profil-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatSnackBarModule,
    MatDividerModule
  ],
  template: `
    <div class="profil-page">
      <mat-toolbar color="primary">
        <span>Mon profil</span>
      </mat-toolbar>

      <div class="profil-content">
        <mat-card>
          <mat-card-title>Informations personnelles</mat-card-title>
          <mat-card-content>
            <p><strong>Nom :</strong> {{ profil?.nom }}</p>
            <p><strong>Prénom :</strong> {{ profil?.prenom }}</p>
            <p><strong>Email :</strong> {{ profil?.email }}</p>
            <p><strong>Rôle :</strong> {{ profil?.role }}</p>
            <p><strong>Créé le :</strong> {{ profil?.createdAt | date:'dd/MM/yyyy HH:mm' }}</p>
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-title>Modifier mon profil</mat-card-title>
          <mat-card-content>
            <form [formGroup]="profilForm" (ngSubmit)="saveProfile()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nom</mat-label>
                <input matInput formControlName="nom" />
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Prénom</mat-label>
                <input matInput formControlName="prenom" />
              </mat-form-field>

              <button mat-raised-button color="primary" type="submit" [disabled]="profilForm.invalid || isLoading">
                <mat-icon>save</mat-icon>
                Enregistrer
              </button>
            </form>
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-title>Changer mon mot de passe</mat-card-title>
          <mat-card-content>
            <form [formGroup]="passwordForm" (ngSubmit)="changePassword()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Ancien mot de passe</mat-label>
                <input matInput type="password" formControlName="ancienPassword" />
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nouveau mot de passe</mat-label>
                <input matInput type="password" formControlName="nouveauPassword" />
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Confirmer le nouveau mot de passe</mat-label>
                <input matInput type="password" formControlName="confirmPassword" />
              </mat-form-field>

              <button mat-raised-button color="accent" type="submit" [disabled]="passwordForm.invalid || isLoading">
                <mat-icon>lock</mat-icon>
                Changer le mot de passe
              </button>
            </form>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [
    `
      .profil-page {
        display: flex;
        flex-direction: column;
        height: 100%;
      }

      .profil-content {
        display: grid;
        gap: 1rem;
        padding: 1rem;
      }

      .full-width {
        width: 100%;
      }
    `
  ]
})
export class ProfilPage implements OnInit {
  profil: ProfilResponse | null = null;
  profilForm: FormGroup;
  passwordForm: FormGroup;
  isLoading = false;

  constructor(
    private profilService: ProfilService,
    private formBuilder: FormBuilder,
    private snackBar: MatSnackBar
  ) {
    this.profilForm = this.formBuilder.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required]
    });

    this.passwordForm = this.formBuilder.group({
      ancienPassword: ['', Validators.required],
      nouveauPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadProfil();
  }

  loadProfil(): void {
    this.isLoading = true;
    this.profilService.getProfil().subscribe({
      next: (profil) => {
        this.profil = profil;
        this.profilForm.patchValue({ nom: profil.nom, prenom: profil.prenom });
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur chargement profil:', err);
      }
    });
  }

  saveProfile(): void {
    if (this.profilForm.invalid) {
      return;
    }

    this.isLoading = true;
    const request: UpdateProfilRequest = this.profilForm.value;
    this.profilService.updateProfil(request).subscribe({
      next: (profil) => {
        this.profil = profil;
        this.isLoading = false;
        this.snackBar.open('Profil mis à jour', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur mise à jour profil:', err);
      }
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      return;
    }

    if (this.passwordForm.value.nouveauPassword !== this.passwordForm.value.confirmPassword) {
      this.snackBar.open('Les mots de passe ne correspondent pas', 'Fermer', { duration: 3000 });
      return;
    }

    this.isLoading = true;
    const request: ChangePasswordRequest = this.passwordForm.value;
    this.profilService.changePassword(request).subscribe({
      next: () => {
        this.isLoading = false;
        this.passwordForm.reset();
        this.snackBar.open('Mot de passe modifié', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur changement mot de passe:', err);
      }
    });
  }
}

@Component({
  selector: 'app-users-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatSnackBarModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatChipsModule,
    MatTooltipModule,
    MatDividerModule
  ],
  template: `
    <div class="users-page">
      <mat-toolbar color="primary">
        <span>Gestion des utilisateurs</span>
      </mat-toolbar>

      <div class="users-content">
        <mat-card class="summary-card">
          <div class="summary-row">
            <div>
              <div class="summary-label">Admins</div>
              <div class="summary-value">{{ stats?.totalAdmins ?? 0 }}</div>
            </div>
            <div>
              <div class="summary-label">Analystes</div>
              <div class="summary-value">{{ stats?.totalAnalystes ?? 0 }}</div>
            </div>
            <div>
              <div class="summary-label">Actifs</div>
              <div class="summary-value">{{ stats?.totalActifs ?? 0 }}</div>
            </div>
            <div>
              <div class="summary-label">Inactifs</div>
              <div class="summary-value">{{ stats?.totalInactifs ?? 0 }}</div>
            </div>
          </div>
        </mat-card>

        <mat-card class="form-card">
          <mat-card-title>Créer un nouvel analyste</mat-card-title>
          <mat-card-content>
            <form [formGroup]="createForm" (ngSubmit)="createAnalyste()" class="create-form">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nom</mat-label>
                <input matInput formControlName="nom" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Prénom</mat-label>
                <input matInput formControlName="prenom" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Email</mat-label>
                <input matInput type="email" formControlName="email" />
              </mat-form-field>
              <button mat-raised-button color="primary" type="submit" [disabled]="createForm.invalid || isLoading">
                <mat-icon>person_add</mat-icon>
                Créer analyste
              </button>
            </form>
          </mat-card-content>
        </mat-card>

        <mat-card *ngIf="selectedUser" class="form-card">
          <mat-card-title>Modifier utilisateur</mat-card-title>
          <mat-card-content>
            <form [formGroup]="editForm" (ngSubmit)="updateUser()" class="create-form">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nom</mat-label>
                <input matInput formControlName="nom" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Prénom</mat-label>
                <input matInput formControlName="prenom" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Email</mat-label>
                <input matInput type="email" formControlName="email" />
              </mat-form-field>
              <div class="edit-actions">
                <button mat-raised-button color="accent" type="submit" [disabled]="editForm.invalid || isLoading">
                  <mat-icon>save</mat-icon>
                  Enregistrer
                </button>
                <button mat-stroked-button type="button" (click)="clearSelection()">
                  <mat-icon>close</mat-icon>
                  Annuler
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>

        <mat-card class="table-card">
          <mat-card-title>Liste des utilisateurs</mat-card-title>
          <mat-card-content>
            <div class="table-wrapper">
              <table mat-table [dataSource]="dataSource" matSort class="users-table">
                <ng-container matColumnDef="nom">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Nom</th>
                  <td mat-cell *matCellDef="let user">{{ user.nom }}</td>
                </ng-container>

                <ng-container matColumnDef="prenom">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Prénom</th>
                  <td mat-cell *matCellDef="let user">{{ user.prenom }}</td>
                </ng-container>

                <ng-container matColumnDef="email">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Email</th>
                  <td mat-cell *matCellDef="let user">{{ user.email }}</td>
                </ng-container>

                <ng-container matColumnDef="role">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Rôle</th>
                  <td mat-cell *matCellDef="let user">{{ user.role }}</td>
                </ng-container>

                <ng-container matColumnDef="verified">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Vérifié</th>
                  <td mat-cell *matCellDef="let user">
                    <mat-chip [color]="user.verified ? 'primary' : 'warn'" selected>
                      {{ user.verified ? 'Oui' : 'Non' }}
                    </mat-chip>
                  </td>
                </ng-container>

                <ng-container matColumnDef="active">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Actif</th>
                  <td mat-cell *matCellDef="let user">
                    <mat-chip [color]="user.active ? 'primary' : 'warn'" selected>
                      {{ user.active ? 'Oui' : 'Non' }}
                    </mat-chip>
                  </td>
                </ng-container>

                <ng-container matColumnDef="createdAt">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Créé le</th>
                  <td mat-cell *matCellDef="let user">{{ user.createdAt | date:'dd/MM/yyyy' }}</td>
                </ng-container>

                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let user" class="action-cell">
                    <button mat-icon-button matTooltip="Modifier" (click)="selectUser(user)">
                      <mat-icon>edit</mat-icon>
                    </button>
                    <button mat-icon-button matTooltip="Vérifier" *ngIf="!user.verified" (click)="verifyUser(user)">
                      <mat-icon>verified</mat-icon>
                    </button>
                    <button mat-icon-button matTooltip="Activer / Désactiver" (click)="toggleActive(user)">
                      <mat-icon>{{ user.active ? 'toggle_off' : 'toggle_on' }}</mat-icon>
                    </button>
                    <button mat-icon-button matTooltip="Promouvoir / Rétrograder" (click)="toggleRole(user)" [disabled]="user.isSystemAdmin">
                      <mat-icon>{{ user.role === 'ADMIN' ? 'arrow_downward' : 'arrow_upward' }}</mat-icon>
                    </button>
                    <button mat-icon-button matTooltip="Réinitialiser le mot de passe" (click)="resetPassword(user)" [disabled]="user.isSystemAdmin">
                      <mat-icon>refresh</mat-icon>
                    </button>
                    <button mat-icon-button color="warn" matTooltip="Supprimer" (click)="deleteUser(user)" [disabled]="user.isSystemAdmin">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </div>
            <mat-paginator [pageSizeOptions]="[5, 10, 20]" showFirstLastButtons></mat-paginator>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [
    `
      .users-page {
        display: flex;
        flex-direction: column;
        height: 100%;
      }

      .users-content {
        display: grid;
        gap: 1rem;
        padding: 1rem;
      }

      .summary-card {
        padding: 1rem;
      }

      .summary-row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
        gap: 1rem;
      }

      .summary-label {
        font-size: 0.9rem;
        color: rgba(0,0,0,0.7);
      }

      .summary-value {
        font-size: 1.8rem;
        font-weight: 700;
      }

      .form-card {
        padding: 1rem;
      }

      .create-form {
        display: grid;
        gap: 1rem;
      }

      .full-width {
        width: 100%;
      }

      .table-card {
        overflow-x: auto;
      }

      .table-wrapper {
        overflow-x: auto;
      }

      .users-table {
        width: 100%;
      }

      .action-cell {
        display: flex;
        flex-wrap: wrap;
        gap: 0.25rem;
      }

      .edit-actions {
        display: flex;
        gap: 0.75rem;
        flex-wrap: wrap;
        align-items: center;
      }
    `
  ]
})
export class UsersPage implements OnInit, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  createForm: FormGroup;
  editForm: FormGroup;
  selectedUser: UserResponse | null = null;
  dataSource = new MatTableDataSource<UserResponse>([]);
  displayedColumns: string[] = ['nom', 'prenom', 'email', 'role', 'verified', 'active', 'createdAt', 'actions'];
  stats: UserListResponse | null = null;
  isLoading = false;

  constructor(
    private adminUserService: AdminUserService,
    private formBuilder: FormBuilder,
    private snackBar: MatSnackBar
  ) {
    this.createForm = this.formBuilder.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]]
    });

    this.editForm = this.formBuilder.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]]
    });
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadUsers(): void {
    this.isLoading = true;
    this.adminUserService.getUsers().subscribe({
      next: (response) => {
        this.stats = response;
        this.dataSource.data = response.users;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur chargement utilisateurs:', err);
      }
    });
  }

  createAnalyste(): void {
    if (this.createForm.invalid) {
      return;
    }

    this.isLoading = true;
    const request: CreateAnalysteRequest = this.createForm.value;
    this.adminUserService.createAnalyste(request).subscribe({
      next: () => {
        this.createForm.reset();
        this.loadUsers();
        this.snackBar.open('Analyste créé', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur création analyste:', err);
      }
    });
  }

  selectUser(user: UserResponse): void {
    this.selectedUser = user;
    this.editForm.patchValue({ nom: user.nom, prenom: user.prenom, email: user.email });
  }

  clearSelection(): void {
    this.selectedUser = null;
    this.editForm.reset();
  }

  updateUser(): void {
    if (!this.selectedUser || this.editForm.invalid) {
      return;
    }

    this.isLoading = true;
    const request: UpdateUserRequest = this.editForm.value;
    this.adminUserService.updateUser(this.selectedUser.id, request).subscribe({
      next: () => {
        this.clearSelection();
        this.loadUsers();
        this.snackBar.open('Utilisateur mis à jour', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur mise à jour utilisateur:', err);
      }
    });
  }

  verifyUser(user: UserResponse): void {
    this.isLoading = true;
    this.adminUserService.verifyUser(user.id).subscribe({
      next: () => {
        this.loadUsers();
        this.snackBar.open('Utilisateur vérifié', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur vérification utilisateur:', err);
      }
    });
  }

  toggleActive(user: UserResponse): void {
    this.isLoading = true;
    const action = user.active ? this.adminUserService.deactivateUser(user.id) : this.adminUserService.activateUser(user.id);
    action.subscribe({
      next: () => {
        this.loadUsers();
        this.snackBar.open(`Utilisateur ${user.active ? 'désactivé' : 'activé'}`, 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur activation/désactivation:', err);
      }
    });
  }

  toggleRole(user: UserResponse): void {
    this.isLoading = true;
    const action = user.role === 'ADMIN' ? this.adminUserService.demoteUser(user.id) : this.adminUserService.promoteUser(user.id);
    action.subscribe({
      next: () => {
        this.loadUsers();
        this.snackBar.open(`Rôle mis à jour`, 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur changement de rôle:', err);
      }
    });
  }

  resetPassword(user: UserResponse): void {
    this.isLoading = true;
    this.adminUserService.resetPassword(user.id).subscribe({
      next: () => {
        this.isLoading = false;
        this.snackBar.open('Email de réinitialisation envoyé', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur réinitialisation mot de passe:', err);
      }
    });
  }

  deleteUser(user: UserResponse): void {
    if (!confirm(`Supprimer l'utilisateur ${user.prenom} ${user.nom} ?`)) {
      return;
    }

    this.isLoading = true;
    this.adminUserService.deleteUser(user.id).subscribe({
      next: () => {
        this.clearSelection();
        this.loadUsers();
        this.snackBar.open('Utilisateur supprimé', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur suppression utilisateur:', err);
      }
    });
  }
}
