import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../../../core/services/auth.service';
import { ForgotPasswordRequest } from '../../models/auth.models';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  backendError = '';
  successMessage = '';

  forgotForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  get email() {
    return this.forgotForm.get('email');
  }

  submit(): void {
    this.backendError = '';
    this.successMessage = '';

    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    this.authService.forgotPassword(this.forgotForm.getRawValue() as ForgotPasswordRequest).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'If your email exists, you will receive password reset instructions.';
        this.snackBar.open(this.successMessage, 'Close', { duration: 4000 });
        this.forgotForm.reset();
      },
      error: (error) => {
        this.backendError = error?.error?.message || 'Unable to send reset instructions.';
      }
    });
  }
}