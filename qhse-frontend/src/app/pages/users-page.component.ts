import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../shared/services/user.service';
import { AuthStorageService } from '../shared/services/auth-storage.service';
import { CreateUserRequest, UpdateUserRequest, User, UserListResponse } from '../shared/models/user.models';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
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
    MatSelectModule,
    MatTableModule,
    MatChipsModule,
    MatDialogModule
  ],
  template: `
    <section class="users-shell">
      <div class="page-header">
        <div>
          <p class="eyebrow">Administration</p>
          <h1>Gestion des utilisateurs</h1>
          <p>Gérez les accès à la plateforme : créez, modifiez et supprimez des comptes utilisateur.</p>
        </div>
        <div class="header-actions">
          <a mat-flat-button color="primary" routerLink="/dashboard">Retour au tableau de bord</a>
        </div>
      </div>

      <div class="stats-grid">
        <mat-card class="stat-card">
          <div class="stat-icon"><mat-icon>admin_panel_settings</mat-icon></div>
          <div>
            <p class="stat-title">Administrateurs</p>
            <p class="stat-number">{{ userStats.totalAdmins }}</p>
          </div>
        </mat-card>

        <mat-card class="stat-card">
          <div class="stat-icon"><mat-icon>analytics</mat-icon></div>
          <div>
            <p class="stat-title">Analystes</p>
            <p class="stat-number">{{ userStats.totalAnalystes }}</p>
          </div>
        </mat-card>

        <mat-card class="stat-card">
          <div class="stat-icon"><mat-icon>check_circle</mat-icon></div>
          <div>
            <p class="stat-title">Utilisateurs actifs</p>
            <p class="stat-number">{{ userStats.totalActifs }}</p>
          </div>
        </mat-card>

        <mat-card class="stat-card">
          <div class="stat-icon"><mat-icon>block</mat-icon></div>
          <div>
            <p class="stat-title">Utilisateurs inactifs</p>
            <p class="stat-number">{{ userStats.totalInactifs }}</p>
          </div>
        </mat-card>
      </div>

      <mat-card class="form-card">
        <h2>{{ isEditing ? 'Modifier un utilisateur' : 'Créer un nouvel utilisateur' }}</h2>
        <form [formGroup]="userForm" (ngSubmit)="submit()">
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
              <input matInput type="email" formControlName="email" />
            </mat-form-field>
          </div>

          <div class="form-actions">
            <button mat-flat-button color="primary" type="submit" [disabled]="userForm.invalid">
              {{ isEditing ? 'Mettre à jour' : 'Créer l\'utilisateur' }}
            </button>
            <button mat-button type="button" *ngIf="isEditing" (click)="resetForm()">Annuler</button>
          </div>
        </form>
      </mat-card>

      <mat-card class="table-card">
        <h2>Liste des utilisateurs</h2>
        <table mat-table [dataSource]="users" class="users-table">
          <ng-container matColumnDef="nom">
            <th mat-header-cell *matHeaderCellDef>Nom</th>
            <td mat-cell *matCellDef="let user">{{ user.nom }}</td>
          </ng-container>

          <ng-container matColumnDef="prenom">
            <th mat-header-cell *matHeaderCellDef>Prénom</th>
            <td mat-cell *matCellDef="let user">{{ user.prenom }}</td>
          </ng-container>

          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Email</th>
            <td mat-cell *matCellDef="let user">{{ user.email }}</td>
          </ng-container>

          <ng-container matColumnDef="role">
            <th mat-header-cell *matHeaderCellDef>Rôle</th>
            <td mat-cell *matCellDef="let user">
              <mat-chip [color]="user.role === 'ADMIN' ? 'primary' : 'accent'" selected>
                {{ user.role }}
              </mat-chip>
            </td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Statut</th>
            <td mat-cell *matCellDef="let user">
              <mat-chip [color]="user.active ? 'primary' : 'warn'" selected>
                {{ user.active ? 'Actif' : 'Inactif' }}
              </mat-chip>
              <mat-chip *ngIf="!user.verified" color="accent" selected>Non vérifié</mat-chip>
            </td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let user">
              <div class="action-buttons">
                <button mat-icon-button color="primary" aria-label="Modifier" (click)="editUser(user)">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button color="success" aria-label="Vérifier le compte" *ngIf="!user.verified" (click)="verifyUser(user.id)">
                  <mat-icon>verified</mat-icon>
                </button>
                <button mat-icon-button color="accent" aria-label="Réinitialiser mot de passe" (click)="resetPassword(user.id)">
                  <mat-icon>lock_reset</mat-icon>
                </button>
                <button mat-icon-button [color]="user.active ? 'warn' : 'primary'"
                        [attr.aria-label]="user.active ? 'Désactiver' : 'Activer'"
                        (click)="toggleUserStatus(user)">
                  <mat-icon>{{ user.active ? 'block' : 'check_circle' }}</mat-icon>
                </button>
                <button mat-icon-button color="warn" aria-label="Supprimer" (click)="deleteUser(user.id)">
                  <mat-icon>delete</mat-icon>
                </button>
              </div>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
        <p class="empty-message" *ngIf="users.length === 0">Aucun utilisateur trouvé.</p>
      </mat-card>
    </section>
  `,
  styles: [`
    .users-shell {
      max-width: 1400px;
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
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 1rem;
    }
    .stat-card {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1.5rem;
      border-radius: 1rem;
      box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
    }
    .stat-icon {
      width: 3rem;
      height: 3rem;
      display: grid;
      place-items: center;
      border-radius: 1rem;
      background: rgba(11, 74, 148, 0.08);
      color: #0b4a94;
      font-size: 1.25rem;
    }
    .stat-title {
      margin: 0 0 0.25rem;
      font-weight: 700;
      color: #0f172a;
      font-size: 0.9rem;
    }
    .stat-number {
      margin: 0;
      font-size: 1.75rem;
      color: #0b4a94;
    }
    .form-card,
    .table-card {
      padding: 1.5rem;
      border-radius: 1rem;
      box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
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
      margin-top: 1rem;
    }
    .users-table {
      width: 100%;
      margin-top: 1rem;
    }
    .action-buttons {
      display: flex;
      gap: 0.25rem;
    }
    .empty-message {
      margin: 2rem 0 0;
      color: #64748b;
      font-style: italic;
      text-align: center;
    }
    @media (max-width: 1200px) {
      .stats-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }
    @media (max-width: 768px) {
      .stats-grid {
        grid-template-columns: 1fr;
      }
      .form-grid {
        grid-template-columns: 1fr;
      }
      .users-table {
        font-size: 0.9rem;
      }
      .action-buttons {
        flex-direction: column;
        gap: 0.5rem;
      }
    }
  `]
})
export class UsersPage implements OnInit {
  users: User[] = [];
  userStats = {
    totalAdmins: 0,
    totalAnalystes: 0,
    totalActifs: 0,
    totalInactifs: 0
  };
  isEditing = false;
  editingId: number | null = null;
  displayedColumns: string[] = ['nom', 'prenom', 'email', 'role', 'status', 'actions'];

  userForm = new FormGroup({
    nom: new FormControl('', Validators.required),
    prenom: new FormControl('', Validators.required),
    email: new FormControl('', [Validators.required, Validators.email])
  });

  constructor(
    public authStorage: AuthStorageService,
    private userService: UserService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe((response) => {
      this.users = response.users;
      this.userStats = {
        totalAdmins: response.totalAdmins,
        totalAnalystes: response.totalAnalystes,
        totalActifs: response.totalActifs,
        totalInactifs: response.totalInactifs
      };
    });
  }

  editUser(user: User): void {
    this.isEditing = true;
    this.editingId = user.id;
    this.userForm.setValue({
      nom: user.nom,
      prenom: user.prenom,
      email: user.email
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  resetForm(): void {
    this.isEditing = false;
    this.editingId = null;
    this.userForm.reset();
  }

  submit(): void {
    if (this.userForm.invalid) {
      return;
    }

    const payload: CreateUserRequest | UpdateUserRequest = this.userForm.value as CreateUserRequest;
    if (this.isEditing && this.editingId != null) {
      this.userService.updateUser(this.editingId, payload).subscribe(() => {
        this.loadUsers();
        this.resetForm();
      });
    } else {
      this.userService.createUser(payload as CreateUserRequest).subscribe(() => {
        this.loadUsers();
        this.userForm.reset();
      });
    }
  }

  deleteUser(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cet utilisateur ?')) {
      this.userService.deleteUser(id).subscribe(() => {
        this.loadUsers();
      });
    }
  }

  toggleUserStatus(user: User): void {
    const action = user.active ? 'désactiver' : 'activer';
    if (confirm(`Êtes-vous sûr de vouloir ${action} cet utilisateur ?`)) {
      const method = user.active ? this.userService.deactivateUser : this.userService.activateUser;
      method.call(this.userService, user.id).subscribe(() => {
        this.loadUsers();
      });
    }
  }

  resetPassword(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir réinitialiser le mot de passe de cet utilisateur ?')) {
      this.userService.resetPassword(id).subscribe(() => {
        alert('Mot de passe réinitialisé avec succès.');
      });
    }
  }

  verifyUser(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir vérifier ce compte utilisateur ? L\'utilisateur pourra se connecter immédiatement.')) {
      this.userService.verifyUser(id).subscribe(() => {
        this.loadUsers();
        alert('Compte vérifié avec succès. L\'utilisateur peut maintenant se connecter.');
      }, (error) => {
        if (error.status === 400) {
          alert('Ce compte est déjà vérifié.');
        } else {
          alert('Erreur lors de la vérification du compte.');
        }
      });
    }
  }

  trackByUser(_: number, user: User): number {
    return user.id;
  }
}
