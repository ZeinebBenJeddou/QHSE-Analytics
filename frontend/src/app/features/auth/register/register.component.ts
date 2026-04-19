import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  data = {
    nom: '',
    prenom: '',
    email: '',
    password: '',
    confirmPassword: ''
  };

  fieldErrors: Partial<Record<keyof typeof this.data, string>> = {};
  formError = '';
  successMessage = '';
  hidePassword = true;
  hideConfirmPassword = true;
  isSubmitting = false;

  constructor(
    private auth: AuthService,
    private router: Router
  ) {}

  register(): void {
    this.formError = '';
    this.successMessage = '';
    this.fieldErrors = {};

    if (!this.validateForm()) {
      return;
    }

    this.isSubmitting = true;

    this.auth.register(this.data).subscribe({
      next: (res: any) => {
        this.isSubmitting = false;
        this.successMessage = res?.message || 'Inscription réussie. Vérifiez votre email pour activer votre compte.';
        this.data = {
          nom: '',
          prenom: '',
          email: '',
          password: '',
          confirmPassword: ''
        };

        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1200);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.formError = this.extractBackendError(err);
      }
    });
  }

  private extractBackendError(err: any): string {
    const backendError = err?.error?.error || err?.error?.message;

    if (typeof backendError === 'string' && backendError.trim()) {
      const marker = 'default message [';
      if (backendError.includes(marker)) {
        const parts = backendError.split(marker);
        const lastPart = parts[parts.length - 1] || '';
        const extracted = lastPart.split(']')[0]?.trim();
        if (extracted) {
          return extracted;
        }
      }
      return backendError;
    }

    return 'Erreur lors de l\'inscription.';
  }

  private validateForm(): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/;

    if (!this.data.nom.trim()) {
      this.fieldErrors.nom = 'Le nom est obligatoire.';
    }

    if (!this.data.prenom.trim()) {
      this.fieldErrors.prenom = 'Le prénom est obligatoire.';
    }

    if (!this.data.email.trim()) {
      this.fieldErrors.email = 'L\'email est obligatoire.';
    } else if (!emailRegex.test(this.data.email)) {
      this.fieldErrors.email = 'Format d\'email invalide.';
    }

    if (!this.data.password) {
      this.fieldErrors.password = 'Le mot de passe est obligatoire.';
    } else if (this.data.password.length < 8) {
      this.fieldErrors.password = 'Le mot de passe doit contenir au moins 8 caractères.';
    } else if (!passwordRegex.test(this.data.password)) {
      this.fieldErrors.password = 'Le mot de passe doit contenir majuscule, minuscule, chiffre et caractère spécial.';
    }

    if (!this.data.confirmPassword) {
      this.fieldErrors.confirmPassword = 'La confirmation du mot de passe est obligatoire.';
    } else if (this.data.confirmPassword !== this.data.password) {
      this.fieldErrors.confirmPassword = 'Les mots de passe ne correspondent pas.';
    }

    return Object.keys(this.fieldErrors).length === 0;
  }
}
