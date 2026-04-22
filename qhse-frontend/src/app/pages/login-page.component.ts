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
import { LoginRequest } from '../shared/models/auth.models';

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
        <h1>Connexion</h1>
        <p>Connectez-vous avec votre compte administrateur ou analyste.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="fill">
            <mat-label>Email</mat-label>
            <input matInput formControlName="email" type="email" autocomplete="email" />
            <mat-error *ngIf="form.controls['email'].invalid && form.controls['email'].touched">
              Merci de saisir un email valide.
            </mat-error>
          </mat-form-field>

          <mat-form-field appearance="fill">
            <mat-label>Mot de passe</mat-label>
            <input matInput formControlName="password" type="password" autocomplete="current-password" />
            <mat-error *ngIf="form.controls['password'].invalid && form.controls['password'].touched">
              Le mot de passe est requis.
            </mat-error>
          </mat-form-field>

          <button mat-flat-button color="primary" class="submit-button" type="submit" [disabled]="form.invalid || loading">
            <span *ngIf="!loading">Envoyer le code OTP</span>
            <mat-progress-spinner *ngIf="loading" diameter="20" mode="indeterminate"></mat-progress-spinner>
          </button>
        </form>

        <div class="auth-actions">
          <a routerLink="/forgot-password">Mot de passe oublié ?</a>
          <a routerLink="/register">Je souhaite m'inscrire</a>
        </div>
      </mat-card>
    </section>
  `,
  styles: [`
    .auth-shell {
      max-width: 480px;
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
      justify-content: space-between;
      gap: 1rem;
      margin-top: 1.5rem;
      flex-wrap: wrap;
      color: #0b4a94;
    }

    .auth-actions a {
      font-size: 0.95rem;
      color: #0b4a94;
    }

    mat-progress-spinner {
      margin: 0 auto;
      display: inline-block;
    }
  `]
})
export class LoginPage {
  loading = false;
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    const payload: LoginRequest = this.form.value as LoginRequest;

    this.authService.login(payload).subscribe({
      next: () => {
        this.snackBar.open('Code OTP envoyé sur votre email.', 'Fermer', { duration: 4000 });
        this.router.navigate(['/verify-otp'], { queryParams: { email: payload.email } });
      },
      error: (error) => {
        const message = error?.error?.message || 'Impossible de se connecter. Vérifiez vos informations.';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      },
      complete: () => {
        this.loading = false;
      }
    });
  }
}
