import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { AdminService } from '../../../../core/services/admin.service';
import { ChangePasswordRequest, ProfileResponse, UpdateProfilRequest } from '../../models/admin.models';
import { TokenService } from '../../../../core/services/token.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CurrentProfileStateService } from '../../../../core/services/current-profile-state.service';

const passwordMatchValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const nouveau = group.get('nouveauPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return nouveau && confirm && nouveau !== confirm ? { passwordMismatch: true } : null;
};

const passwordStrengthValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value || '';
  const hasUpper = /[A-Z]/.test(value);
  const hasLower = /[a-z]/.test(value);
  const hasDigit = /\d/.test(value);
  const hasSpecial = /[^A-Za-z\d]/.test(value);
  const hasLength = value.length >= 8;

  return hasUpper && hasLower && hasDigit && hasSpecial && hasLength
    ? null
    : { passwordStrength: true };
};

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
  private readonly currentProfileState = inject(CurrentProfileStateService);

  profile: ProfileResponse | null = null;
  loading        = false;
  savingProfile  = false;
  savingPassword = false;
  errorMessage   = '';
  passwordErrorMessage = '';
  showAncienPassword = false;
  showNouveauPassword = false;
  showConfirmPassword = false;

  profileForm = this.fb.group({
    nom:    ['', Validators.required],
    prenom: ['', Validators.required],
  });

  passwordForm = this.fb.group({
    ancienPassword:  ['', Validators.required],
    nouveauPassword: ['', [Validators.required, passwordStrengthValidator]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordMatchValidator });

  get ancienPassword() {
    return this.passwordForm.get('ancienPassword');
  }

  get nouveauPassword() {
    return this.passwordForm.get('nouveauPassword');
  }

  get confirmPassword() {
    return this.passwordForm.get('confirmPassword');
  }

  get passwordMismatch(): boolean {
    return this.passwordForm.hasError('passwordMismatch') &&
           !!this.passwordForm.get('confirmPassword')?.dirty;
  }

  ngOnInit(): void {
  
    const resolved = this.route.snapshot.data['profile'] as ProfileResponse | null;

    if (resolved) {
      this.profile = resolved;
      this.profileForm.setValue({ nom: resolved.nom, prenom: resolved.prenom });
      this.currentProfileState.setProfile(resolved);
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
    this.adminService.updateProfile(payload)
      .pipe(finalize(() => { this.savingProfile = false; }))
      .subscribe({
      next: (profile) => {
        this.profile       = profile;
        this.profileForm.setValue({ nom: profile.nom, prenom: profile.prenom });
        this.currentProfileState.setProfile(profile);
        this.snackBar.open('Profil mis à jour avec succès.', 'Fermer', { duration: 4000 });
      },
      error: () => {
        this.snackBar.open('Impossible de mettre à jour le profil.', 'Fermer', { duration: 4000 });
      },
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.savingPassword = true;
    this.passwordErrorMessage = '';
    const payload = this.passwordForm.value as ChangePasswordRequest;
    this.adminService.changePassword(payload)
      .pipe(finalize(() => { this.savingPassword = false; }))
      .subscribe({
      next: (response) => {
        this.snackBar.open(response.message, 'Fermer', { duration: 5000 });
        this.authService.logout().subscribe({
          complete: () => { this.tokenService.removeToken(); this.router.navigate(['/auth/login']); },
          error:    () => { this.tokenService.removeToken(); this.router.navigate(['/auth/login']); },
        });
      },
      error: (err: HttpErrorResponse) => {
        this.passwordErrorMessage = err.error?.message || 'Impossible de changer le mot de passe.';
        this.snackBar.open(this.passwordErrorMessage, 'Fermer', { duration: 5000 });
      },
    });
  }

  get passwordsMismatch(): boolean {
    return this.passwordForm.hasError('passwordMismatch') && !!this.confirmPassword?.touched;
  }
}