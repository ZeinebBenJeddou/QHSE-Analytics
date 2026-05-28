import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import {
  CreateAnalysteRequest, UpdateUserRequest,
  UserListResponse, UserResponse,
} from '../../models/admin.models';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterModule,
    MatCardModule, MatButtonModule, MatDialogModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatFormFieldModule,
    MatInputModule, MatTooltipModule,
  ],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css'],
})
export class AdminUsersComponent implements OnInit, OnDestroy {
  private readonly adminService = inject(AdminService);
  private readonly snackBar     = inject(MatSnackBar);
  private readonly dialog       = inject(MatDialog);
  private readonly fb           = inject(FormBuilder);
  private readonly route        = inject(ActivatedRoute);
  private readonly cdr          = inject(ChangeDetectorRef);
  private readonly destroy$     = new Subject<void>();
  private readonly searchInput$ = new Subject<string>();

  users: UserResponse[]     = [];
  loading                   = false;
  creating                  = false;
  saving                    = false;
  actionInProgressId: number | null = null;
  errorMessage              = '';

  currentPage   = 0;
  totalPages    = 0;
  totalElements = 0;
  readonly pageSize = 15;

  searchQuery = '';

  editingUser: UserResponse | null = null;

  createUserForm = this.fb.group({
    nom:    ['', Validators.required],
    prenom: ['', Validators.required],
    email:  ['', [Validators.required, Validators.email]],
  });

  editUserForm = this.fb.group({
    nom:    ['', Validators.required],
    prenom: ['', Validators.required],
    email:  ['', [Validators.required, Validators.email]],
  });

  ngOnInit(): void {
    this.searchInput$.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(q => {
      this.searchQuery = q;
      this.loadPage(0);
    });

    const resolved = this.route.snapshot.data['users'] as UserListResponse | null;
    if (resolved) {
      this.applyPage(resolved);
    } else {
      this.loadPage(0);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchChange(value: string): void {
    this.searchInput$.next(value);
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchInput$.next('');
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || this.loading) return;
    this.loadPage(page);
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  areUserActionsDisabled(user: UserResponse): boolean {
    return user.isSystemAdmin;
  }

  userActionsDisabledReason(user: UserResponse): string {
    if (user.isSystemAdmin) {
      return 'Administrateur initial - actions désactivées';
    }
    return 'Utilisateur administrateur - actions désactivées';
  }

  submitCreateUser(): void {
    if (this.createUserForm.invalid) return;
    this.creating     = true;
    this.errorMessage = '';
    this.cdr.detectChanges();
    const payload = this.createUserForm.value as CreateAnalysteRequest;

    this.adminService.createUser(payload).subscribe({
      next: () => {
        this.createUserForm.reset();
        this.snackBar.open('Analyste créé avec succès.', 'Fermer', { duration: 3000 });
        this.loadPage(this.currentPage);
      },
      error: () => {
        this.snackBar.open('Impossible de créer l\'analyste.', 'Fermer', { duration: 3000 });
        this.errorMessage = 'Impossible de créer l\'analyste. Vérifiez les informations et réessayez.';
        this.creating = false;
        this.cdr.detectChanges();
      },
    });
  }

  startEdit(user: UserResponse): void {
    if (this.areUserActionsDisabled(user)) return;
    this.editingUser = user;
    this.editUserForm.setValue({ nom: user.nom, prenom: user.prenom, email: user.email });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.editingUser = null;
    this.editUserForm.reset();
  }

  submitEditUser(): void {
    if (this.editUserForm.invalid || !this.editingUser) return;
    this.saving = true;
    this.cdr.detectChanges();
    const payload = this.editUserForm.value as UpdateUserRequest;

    this.adminService.updateUser(this.editingUser.id, payload).subscribe({
      next: () => {
        this.snackBar.open('Utilisateur mis à jour.', 'Fermer', { duration: 3000 });
        this.editingUser = null;
        this.editUserForm.reset();
        this.saving = false;
        this.loadPage(this.currentPage);
      },
      error: (err) => {
        const msg = err?.error?.message || 'Impossible de mettre à jour l\'utilisateur.';
        this.snackBar.open(msg, 'Fermer', { duration: 4000 });
        this.saving = false;
        this.cdr.detectChanges();
      },
    });
  }

  verifyUser(user: UserResponse): void {
    if (this.areUserActionsDisabled(user)) return;
    this.actionInProgressId = user.id;
    this.cdr.detectChanges();
    this.adminService.verifyUser(user.id).subscribe({
      next:  () => { this.snackBar.open('Compte vérifié.', 'Fermer', { duration: 3000 }); this.loadPage(this.currentPage); },
      error: () => { this.snackBar.open('Échec de la vérification.', 'Fermer', { duration: 3000 }); this.actionInProgressId = null; this.cdr.detectChanges(); },
    });
  }

  toggleActivation(user: UserResponse): void {
    if (this.areUserActionsDisabled(user)) return;
    this.actionInProgressId = user.id;
    this.cdr.detectChanges();
    const action = user.active
      ? this.adminService.deactivateUser(user.id)
      : this.adminService.activateUser(user.id);

    action.subscribe({
      next:  () => { this.snackBar.open(user.active ? 'Compte désactivé.' : 'Compte activé.', 'Fermer', { duration: 3000 }); this.loadPage(this.currentPage); },
      error: () => { this.snackBar.open('Impossible de mettre à jour l\'activation.', 'Fermer', { duration: 3000 }); this.actionInProgressId = null; this.cdr.detectChanges(); },
    });
  }

  toggleAdmin(user: UserResponse): void {
    if (this.areUserActionsDisabled(user)) return;
    const isPromoting = user.role !== 'ADMIN';
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: isPromoting ? 'Promouvoir en administrateur' : 'Rétrograder en analyste',
        message: isPromoting
          ? `${user.prenom} ${user.nom} obtiendra les droits administrateur. Sa session active sera déconnectée.`
          : `${user.prenom} ${user.nom} perdra ses droits administrateur. Sa session active sera déconnectée.`,
        confirmLabel: isPromoting ? 'Promouvoir' : 'Rétrograder',
        danger: !isPromoting,
      },
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      setTimeout(() => {
        this.actionInProgressId = user.id;
        this.cdr.detectChanges();
        const action = isPromoting
          ? this.adminService.promoteToAdmin(user.id)
          : this.adminService.demoteToAnalyste(user.id);

        action.subscribe({
          next:  () => { this.snackBar.open(isPromoting ? 'Promu administrateur.' : 'Rétrogradé en analyste.', 'Fermer', { duration: 3000 }); this.loadPage(this.currentPage); },
          error: () => { this.snackBar.open('Impossible de modifier le rôle.', 'Fermer', { duration: 3000 }); this.actionInProgressId = null; this.cdr.detectChanges(); },
        });
      });
    });
  }

  resetUserPassword(user: UserResponse): void {
    if (this.areUserActionsDisabled(user)) return;
    this.actionInProgressId = user.id;
    this.cdr.detectChanges();
    this.adminService.resetUserPassword(user.id).subscribe({
      next:     () => { this.snackBar.open('Email de réinitialisation envoyé.', 'Fermer', { duration: 3000 }); this.actionInProgressId = null; this.cdr.detectChanges(); },
      error:    () => { this.snackBar.open('Impossible d\'envoyer l\'email.', 'Fermer', { duration: 3000 }); this.actionInProgressId = null; this.cdr.detectChanges(); },
    });
  }

  deleteUser(user: UserResponse): void {
    if (this.areUserActionsDisabled(user)) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer l\'utilisateur',
        message: `Supprimer définitivement ${user.prenom} ${user.nom} (${user.email}) et toutes ses données ? Cette action est irréversible.`,
        confirmLabel: 'Supprimer',
        danger: true,
      },
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      setTimeout(() => {
        this.actionInProgressId = user.id;
        this.cdr.detectChanges();
        this.adminService.deleteUser(user.id).subscribe({
          next: () => {
            this.snackBar.open('Utilisateur supprimé.', 'Fermer', { duration: 3000 });
            const targetPage = this.users.length === 1 && this.currentPage > 0
              ? this.currentPage - 1 : this.currentPage;
            this.loadPage(targetPage);
          },
          error: () => { this.snackBar.open('Impossible de supprimer l\'utilisateur.', 'Fermer', { duration: 3000 }); this.actionInProgressId = null; this.cdr.detectChanges(); },
        });
      });
    });
  }

  private loadPage(page: number): void {
    this.loading = true;
    this.cdr.detectChanges();
    this.adminService.getUsers(page, this.pageSize, this.searchQuery).subscribe({
      next: (r) => {
        this.applyPage(r);
        this.loading            = false;
        this.creating           = false;
        this.actionInProgressId = null;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage       = 'Impossible de recharger la liste.';
        this.loading            = false;
        this.actionInProgressId = null;
        this.cdr.detectChanges();
      },
    });
  }

  private applyPage(r: UserListResponse): void {
    this.users         = r.users;
    this.currentPage   = r.currentPage;
    this.totalPages    = r.totalPages;
    this.totalElements = r.totalElements;
  }
}
