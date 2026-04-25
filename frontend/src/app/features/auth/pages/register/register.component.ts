import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../../../core/services/auth.service';
import { RegisterRequest } from '../../models/auth.models';

const passwordMatchValidator: ValidatorFn = (group): ValidationErrors | null => {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  return password && confirmPassword && password !== confirmPassword ? { passwordMismatch: true } : null;
};

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  backendError = '';
  successMessage = '';

  
  showPassword = false;
  showConfirm = false;

  registerForm = this.fb.group(
    {
      nom: ['', [Validators.required]],
      prenom: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: passwordMatchValidator }
  );

  get nom() {
    return this.registerForm.get('nom');
  }

  get prenom() {
    return this.registerForm.get('prenom');
  }

  get email() {
    return this.registerForm.get('email');
  }

  get password() {
    return this.registerForm.get('password');
  }

  get confirmPassword() {
    return this.registerForm.get('confirmPassword');
  }

  get passwordsMismatch() {
    return this.registerForm.hasError('passwordMismatch');
  }

  submit(): void {
    this.backendError = '';
    this.successMessage = '';

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.authService.register(this.registerForm.getRawValue() as RegisterRequest).subscribe({
      next: (response) => {
        this.successMessage = 'Check your email to verify account.';
        this.snackBar.open(response.message || this.successMessage, 'Close', { duration: 3500 });
        this.registerForm.reset();
      },
      error: (error) => {
        this.backendError = error?.error?.message || 'Unable to register. Please try again.';
      }
    });
  }
}