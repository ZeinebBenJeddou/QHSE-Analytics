import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../shared/services/auth.service';
import { ForgotPasswordRequest } from '../shared/models/auth.models';

@Component({
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  template: `
    <section class="auth-shell">
      <mat-card>
        <h1>Mot de passe oublié</h1>
        <p>Entrez votre email professionnel pour recevoir un lien de réinitialisation.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="fill">
            <mat-label>Email</mat-label>
            <input matInput formControlName="email" type="email" autocomplete="email" />
          </mat-form-field>

          <button mat-flat-button color="primary" class="submit-button" type="submit" [disabled]="form.invalid || loading">
            <span *ngIf="!loading">Envoyer le lien</span>
            <mat-progress-spinner *ngIf="loading" diameter="20" mode="indeterminate"></mat-progress-spinner>
          </button>
        </form>

        <div class="auth-actions">
          <a routerLink="/login">Retour à la connexion</a>
        </div>
      </mat-card>
    </section>
  `,
  styles: [`
    .auth-shell {
      max-width: 520px;
      margin: 0 auto;
      padding: 2rem 1rem;
    }

    mat-card {
      padding: 2rem;
      border-radius: 1.4rem;
      box-shadow: 0 30px 70px rgba(15, 23, 42, 0.12);
    }

    h1 {
      margin: 0 0 0.5rem;
      font-size: 2.2rem;
      color: #0b4a94;
    }

    p {
      margin: 0 0 1.75rem;
      color: #475569;
      line-height: 1.65;
    }

    .submit-button {
      width: 100%;
      margin-top: 1rem;
      min-height: 3rem;
    }

    .auth-actions {
      display: flex;
      justify-content: flex-end;
      margin-top: 1.5rem;
    }

    .auth-actions a {
      color: #0b4a94;
      font-size: 0.95rem;
    }

    mat-progress-spinner {
      margin: 0 auto;
      display: inline-block;
    }
  `]
})
export class ForgotPasswordPage {
  loading = false;
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    const payload: ForgotPasswordRequest = this.form.value as ForgotPasswordRequest;

    this.authService.forgotPassword(payload).subscribe({
      next: () => {
        this.snackBar.open('Un email de réinitialisation a été envoyé.', 'Fermer', { duration: 6000 });
        this.router.navigate(['/login']);
      },
      error: (error) => {
        const message = error?.error?.message || 'Impossible d’envoyer le lien de réinitialisation.';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      },
      complete: () => {
        this.loading = false;
      }
    });
  }
}
