import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';
import { AdminService } from '../../../../core/services/admin.service';
import { UserResponse } from '../../models/admin.models';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class AdminUsersComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly snackBar = inject(MatSnackBar);

  users: UserResponse[] = [];
  loading = false;
  actionInProgressId: number | null = null;
  errorMessage = '';

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.errorMessage = '';

    this.adminService.getUsers().subscribe({
      next: (response) => {
        this.users = response.users;
      },
      error: () => {
        this.errorMessage = 'Unable to load user list. Please try again later.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  verifyUser(user: UserResponse): void {
    this.actionInProgressId = user.id;
    this.adminService.verifyUser(user.id).subscribe({
      next: () => {
        this.snackBar.open('User verified successfully.', 'Close', { duration: 3000 });
        this.loadUsers();
      },
      error: () => {
        this.snackBar.open('Verification failed.', 'Close', { duration: 3000 });
      },
      complete: () => {
        this.actionInProgressId = null;
      }
    });
  }

  toggleActivation(user: UserResponse): void {
    this.actionInProgressId = user.id;
    const action = user.active ? this.adminService.deactivateUser(user.id) : this.adminService.activateUser(user.id);

    action.subscribe({
      next: () => {
        this.snackBar.open(user.active ? 'User deactivated.' : 'User activated.', 'Close', { duration: 3000 });
        this.loadUsers();
      },
      error: () => {
        this.snackBar.open('Unable to update activation status.', 'Close', { duration: 3000 });
      },
      complete: () => {
        this.actionInProgressId = null;
      }
    });
  }

  toggleAdmin(user: UserResponse): void {
    this.actionInProgressId = user.id;
    const action = user.role === 'ADMIN' ? this.adminService.demoteToAnalyste(user.id) : this.adminService.promoteToAdmin(user.id);

    action.subscribe({
      next: () => {
        this.snackBar.open(user.role === 'ADMIN' ? 'User demoted to analyst.' : 'User promoted to admin.', 'Close', { duration: 3000 });
        this.loadUsers();
      },
      error: () => {
        this.snackBar.open('Unable to update role.', 'Close', { duration: 3000 });
      },
      complete: () => {
        this.actionInProgressId = null;
      }
    });
  }

  resetUserPassword(user: UserResponse): void {
    this.actionInProgressId = user.id;
    this.adminService.resetUserPassword(user.id).subscribe({
      next: () => {
        this.snackBar.open('Password reset email sent.', 'Close', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Unable to send password reset email.', 'Close', { duration: 3000 });
      },
      complete: () => {
        this.actionInProgressId = null;
      }
    });
  }

  deleteUser(user: UserResponse): void {
    this.actionInProgressId = user.id;
    this.adminService.deleteUser(user.id).subscribe({
      next: () => {
        this.snackBar.open('User deleted.', 'Close', { duration: 3000 });
        this.loadUsers();
      },
      error: () => {
        this.snackBar.open('Unable to delete user.', 'Close', { duration: 3000 });
      },
      complete: () => {
        this.actionInProgressId = null;
      }
    });
  }
}
