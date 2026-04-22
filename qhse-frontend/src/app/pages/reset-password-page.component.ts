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
import { ResetPasswordRequest } from '../shared/models/auth.models';

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
        <h1>Réinitialiser le mot de passe</h1>
        <p>Entrez un nouveau mot de passe pour sécuriser votre compte.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="fill">
            <mat-label>Token de réinitialisation</mat-label>
            <input matInput formControlName="token" autocomplete="off" />
          </mat-form-field>

          <mat-form-field appearance="fill">
            <mat-label>Nouveau mot de passe</mat-label>
            <input matInput formControlName="password" type="password" autocomplete="new-password" />
          </mat-form-field>

          <mat-form-field appearance="fill">
            <mat-label>Confirmer le mot de passe</mat-label>
            <input matInput formControlName="confirmPassword" type="password" autocomplete="new-password" />
          </mat-form-field>

          <button mat-flat-button color="primary" class="submit-button" type="submit" [disabled]="form.invalid || loading">
            <span *ngIf="!loading">Mettre à jour le mot de passe</span>
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
export class ResetPasswordPage {
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
      token: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    });

    const token = this.activatedRoute.snapshot.queryParamMap.get('token');
    if (token) {
      this.form.patchValue({ token });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.form.value.password !== this.form.value.confirmPassword) {
      this.snackBar.open('Les mots de passe ne correspondent pas.', 'Fermer', { duration: 5000 });
      return;
    }

    this.loading = true;
    const payload: ResetPasswordRequest = this.form.value as ResetPasswordRequest;

    this.authService.resetPassword(payload).subscribe({
      next: () => {
        this.snackBar.open('Votre mot de passe a bien été réinitialisé.', 'Fermer', { duration: 6000 });
        this.router.navigate(['/login']);
      },
      error: (error) => {
        const message = error?.error?.message || 'Impossible de réinitialiser le mot de passe.';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      },
      complete: () => {
        this.loading = false;
      }
    });
  }
}
