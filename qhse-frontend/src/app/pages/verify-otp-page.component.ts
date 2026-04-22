import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../shared/services/auth.service';
import { VerifyOtpRequest } from '../shared/models/auth.models';

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
        <h1>Vérification OTP</h1>
        <p>Entrez le code envoyé à votre adresse email pour terminer la connexion.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="fill">
            <mat-label>Adresse email</mat-label>
            <input matInput formControlName="email" type="email" autocomplete="email" />
          </mat-form-field>

          <mat-form-field appearance="fill">
            <mat-label>Code OTP</mat-label>
            <input matInput formControlName="code" type="text" autocomplete="one-time-code" />
          </mat-form-field>

          <button mat-flat-button color="primary" class="submit-button" type="submit" [disabled]="form.invalid || loading">
            <span *ngIf="!loading">Valider le code</span>
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
export class VerifyOtpPage {
  loading = false;
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      code: ['', [Validators.required, Validators.minLength(6), Validators.pattern('^[0-9]+$')]]
    });

    const email = this.activatedRoute.snapshot.queryParamMap.get('email');
    if (email) {
      this.form.patchValue({ email });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    const payload: VerifyOtpRequest = this.form.value as VerifyOtpRequest;

    this.authService.verifyOtp(payload).subscribe({
      next: (response) => {
        this.snackBar.open('Connexion réussie. Bienvenue !', 'Fermer', { duration: 4000 });
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        const message = error?.error?.message || 'Code invalide ou expiré. Réessayez.';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      },
      complete: () => {
        this.loading = false;
      }
    });
  }
}
