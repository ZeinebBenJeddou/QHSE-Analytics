import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
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
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  email = '';
  fieldError = '';
  formError = '';
  successMessage = '';
  isSubmitting = false;

  constructor(private auth: AuthService) {}

  submit(): void {
    this.fieldError = '';
    this.formError = '';
    this.successMessage = '';

    if (!this.validateEmail()) {
      return;
    }

    this.isSubmitting = true;

    this.auth.forgotPassword(this.email.trim()).subscribe({
      next: (res: any) => {
        this.isSubmitting = false;
        this.successMessage = res?.message || 'Si email existe, lien envoyé.';
      },
      error: (err) => {
        this.isSubmitting = false;
        this.formError = err?.error?.error || err?.error?.message || 'Erreur lors de l\'envoi du lien.';
      }
    });
  }

  private validateEmail(): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!this.email.trim()) {
      this.fieldError = 'L\'email est obligatoire.';
      return false;
    }

    if (!emailRegex.test(this.email)) {
      this.fieldError = 'Format d\'email invalide.';
      return false;
    }

    return true;
  }
}
