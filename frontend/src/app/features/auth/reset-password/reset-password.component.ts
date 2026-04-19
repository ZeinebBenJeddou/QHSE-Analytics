import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent {
  data = {
    token: '',
    password: '',
    confirmPassword: ''
  };

  fieldErrors: Partial<Record<'password' | 'confirmPassword' | 'token', string>> = {};
  formError = '';
  successMessage = '';
  hidePassword = true;
  hideConfirmPassword = true;
  isSubmitting = false;

  constructor(
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.data.token = this.route.snapshot.queryParamMap.get('token') || '';
  }

  submit(): void {
    this.fieldErrors = {};
    this.formError = '';
    this.successMessage = '';

    if (!this.validateForm()) {
      return;
    }

    this.isSubmitting = true;

    this.auth.resetPassword(this.data).subscribe({
      next: (res: any) => {
        this.isSubmitting = false;
        this.successMessage = res?.message || 'Mot de passe réinitialisé.';
        this.data.password = '';
        this.data.confirmPassword = '';

        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1300);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.formError = err?.error?.error || err?.error?.message || 'Erreur lors de la réinitialisation.';
      }
    });
  }

  private validateForm(): boolean {
    if (!this.data.token) {
      this.fieldErrors.token = 'Token manquant dans le lien.';
    }

    if (!this.data.password) {
      this.fieldErrors.password = 'Le mot de passe est obligatoire.';
    }

    if (!this.data.confirmPassword) {
      this.fieldErrors.confirmPassword = 'La confirmation du mot de passe est obligatoire.';
    } else if (this.data.confirmPassword !== this.data.password) {
      this.fieldErrors.confirmPassword = 'Les mots de passe ne correspondent pas.';
    }

    return Object.keys(this.fieldErrors).length === 0;
  }
}
