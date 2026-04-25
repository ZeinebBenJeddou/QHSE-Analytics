import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../../../core/services/auth.service';
import { ResetPasswordRequest } from '../../models/auth.models';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  backendError = '';
  successMessage = '';
  token: string | null = null;


  showPassword = false;
  showConfirm = false;

  resetForm = this.fb.group(
    {
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: [this.passwordsMatchValidator] }
  );

  ngOnInit(): void {
    this.token = this.activatedRoute.snapshot.queryParamMap.get('token');
    if (!this.token) {
      this.backendError = 'Token de réinitialisation manquant ou invalide.';
    }
  }

  get password() {
    return this.resetForm.get('password');
  }

  get confirmPassword() {
    return this.resetForm.get('confirmPassword');
  }

  submit(): void {
    this.backendError = '';
    this.successMessage = '';

    if (!this.token) {
      this.backendError = 'Le lien de réinitialisation n\'est pas valide.';
      return;
    }

    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const request = {
      token: this.token,
      ...this.resetForm.getRawValue()
    } as ResetPasswordRequest;

    this.authService.resetPassword(request).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'Mot de passe réinitialisé avec succès.';
        this.snackBar.open(this.successMessage, 'Fermer', { duration: 4000 });
        this.resetForm.reset();
        this.router.navigate(['/auth/login']);
      },
      error: (error) => {
        this.backendError = error?.error?.message || 'Erreur lors de la réinitialisation du mot de passe.';
      }
    });
  }

  private passwordsMatchValidator(group: any) {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password && confirmPassword && password !== confirmPassword
      ? { passwordsMismatch: true }
      : null;
  }
}