import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AdminService } from '../../../../core/services/admin.service';
import { CreateAnalysteRequest, UserResponse } from '../../models/admin.models';
 
@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
  ],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css'],
})
export class AdminUsersComponent implements OnInit {
  private readonly adminService       = inject(AdminService);
  private readonly snackBar           = inject(MatSnackBar);
  private readonly fb                 = inject(FormBuilder);
  private readonly route              = inject(ActivatedRoute);
 
  users: UserResponse[]     = [];
  loading                   = false;
  creating                  = false;
  actionInProgressId: number | null = null;
  errorMessage              = '';
 
  createUserForm = this.fb.group({
    nom:    ['', Validators.required],
    prenom: ['', Validators.required],
    email:  ['', [Validators.required, Validators.email]],
  });
 
  ngOnInit(): void {
    const resolved = this.route.snapshot.data['users'] as UserResponse[] | undefined;
    if (resolved !== undefined) {
      this.users = resolved;
    } else {
      this.refreshUsers();
    }
  }
 
  submitCreateUser(): void {
    if (this.createUserForm.invalid) return;
    this.creating     = true;
    this.errorMessage = '';
    const payload     = this.createUserForm.value as CreateAnalysteRequest;
 
    this.adminService.createUser(payload).subscribe({
      next: () => {
        this.createUserForm.reset();
        this.snackBar.open('Analyste créé avec succès.', 'Fermer', { duration: 3000 });
        this.refreshUsers();
      },
      error: () => {
        this.snackBar.open('Impossible de créer l\'analyste.', 'Fermer', { duration: 3000 });
        this.errorMessage = 'Impossible de créer l\'analyste. Vérifiez les informations et réessayez.';
        this.creating = false;
      },
    });
  }
 
  verifyUser(user: UserResponse): void {
    this.actionInProgressId = user.id;
    this.adminService.verifyUser(user.id).subscribe({
      next:     () => { this.snackBar.open('Compte vérifié.', 'Fermer', { duration: 3000 }); this.refreshUsers(); },
      error:    () => { this.snackBar.open('Échec de la vérification.', 'Fermer', { duration: 3000 }); this.actionInProgressId = null; },
    });
  }
 
  toggleActivation(user: UserResponse): void {
    this.actionInProgressId = user.id;
    const action = user.active
      ? this.adminService.deactivateUser(user.id)
      : this.adminService.activateUser(user.id);
 
    action.subscribe({
      next:  () => { this.snackBar.open(user.active ? 'Compte désactivé.' : 'Compte activé.', 'Fermer', { duration: 3000 }); this.refreshUsers(); },
      error: () => { this.snackBar.open('Impossible de mettre à jour l\'activation.', 'Fermer', { duration: 3000 }); this.actionInProgressId = null; },
    });
  }
 
  toggleAdmin(user: UserResponse): void {
    this.actionInProgressId = user.id;
    const action = user.role === 'ADMIN'
      ? this.adminService.demoteToAnalyste(user.id)
      : this.adminService.promoteToAdmin(user.id);
 
    action.subscribe({
      next:  () => { this.snackBar.open(user.role === 'ADMIN' ? 'Rétrogradé en analyste.' : 'Promu administrateur.', 'Fermer', { duration: 3000 }); this.refreshUsers(); },
      error: () => { this.snackBar.open('Impossible de modifier le rôle.', 'Fermer', { duration: 3000 }); this.actionInProgressId = null; },
    });
  }
 
  resetUserPassword(user: UserResponse): void {
    this.actionInProgressId = user.id;
    this.adminService.resetUserPassword(user.id).subscribe({
      next:     () => { this.snackBar.open('Email de réinitialisation envoyé.', 'Fermer', { duration: 3000 }); },
      error:    () => { this.snackBar.open('Impossible d\'envoyer l\'email.', 'Fermer', { duration: 3000 }); },
      complete: () => { this.actionInProgressId = null; },
    });
  }
 
  deleteUser(user: UserResponse): void {
    this.actionInProgressId = user.id;
    this.adminService.deleteUser(user.id).subscribe({
      next:  () => { this.snackBar.open('Utilisateur supprimé.', 'Fermer', { duration: 3000 }); this.refreshUsers(); },
      error: () => { this.snackBar.open('Impossible de supprimer l\'utilisateur.', 'Fermer', { duration: 3000 }); this.actionInProgressId = null; },
    });
  }
 
  private refreshUsers(): void {
    this.loading = true;
    this.adminService.getUsers().subscribe({
      next:     (response) => { this.users = response.users; },
      error:    () => { this.errorMessage = 'Impossible de recharger la liste.'; },
      complete: () => { this.loading = false; this.creating = false; this.actionInProgressId = null; },
    });
  }
}
 