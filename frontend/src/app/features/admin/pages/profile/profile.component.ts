import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AdminService } from '../../../../core/services/admin.service';
import { ChangePasswordRequest, ProfileResponse, UpdateProfilRequest } from '../../models/admin.models';
import { TokenService } from '../../../../core/services/token.service';
import { AuthService } from '../../../../core/services/auth.service';

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
    MatProgressSpinnerModule,
    MatIconModule,
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class AdminProfileComponent implements OnInit {
  private readonly adminService  = inject(AdminService);
  private readonly fb            = inject(FormBuilder);
  private readonly snackBar      = inject(MatSnackBar);
  private readonly router        = inject(Router);
  private readonly route         = inject(ActivatedRoute);
  private readonly tokenService  = inject(TokenService);
  private readonly authService   = inject(AuthService);

  profile: ProfileResponse | null = null;
  loading        = false;
  savingProfile  = false;
  savingPassword = false;
  errorMessage   = '';

  profileForm = this.fb.group({
    nom:    ['', Validators.required],
    prenom: ['', Validators.required],
  });

  passwordForm = this.fb.group({
    ancienPassword:  ['', Validators.required],
    nouveauPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  });

  ngOnInit(): void {
  
    const resolved = this.route.snapshot.data['profile'] as ProfileResponse | null;

    if (resolved) {
      this.profile = resolved;
      this.profileForm.setValue({ nom: resolved.nom, prenom: resolved.prenom });
    } else {
      this.errorMessage = 'Impossible de charger le profil.';
    }
  }

  get homeRoute(): string {
    return this.router.url.startsWith('/admin') ? '/admin' : '/analyste';
  }

  get avatarInitials(): string {
    if (!this.profile) return '??';
    return this.profile.prenom.charAt(0) + this.profile.nom.charAt(0);
  }

  saveProfile(): void {
    if (this.profileForm.invalid) return;
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
      complete: () => { this.savingProfile = false; },
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) return;
    this.savingPassword = true;
    const payload = this.passwordForm.value as ChangePasswordRequest;
    this.adminService.changePassword(payload).subscribe({
      next: (response) => {
        this.snackBar.open(response.message, 'Fermer', { duration: 5000 });
        this.authService.logout().subscribe({
          complete: () => { this.tokenService.removeToken(); this.router.navigate(['/auth/login']); },
          error: ()  => { this.tokenService.removeToken(); this.router.navigate(['/auth/login']); }
        });
      },
      error: (err: any) => {
        const message = err.error?.message || 'Impossible de changer le mot de passe.';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      },
      complete: () => { this.savingPassword = false; },
    });
  }
}