import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../shared/services/auth.service';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule],
  template: `
    <section class="verify-shell">
      <mat-card>
        <div class="verify-status" *ngIf="loading">
          <mat-progress-spinner diameter="48" mode="indeterminate"></mat-progress-spinner>
          <p>Vérification en cours...</p>
        </div>

        <div class="verify-status" *ngIf="!loading && success">
          <mat-icon color="primary">check_circle</mat-icon>
          <h1>Compte activé</h1>
          <p>Votre compte a bien été vérifié. Vous pouvez maintenant vous connecter.</p>
          <button mat-flat-button color="primary" routerLink="/login">Connexion</button>
        </div>

        <div class="verify-status" *ngIf="!loading && !success && errorMessage">
          <mat-icon color="warn">error</mat-icon>
          <h1>Vérification impossible</h1>
          <p>{{ errorMessage }}</p>
          <button mat-flat-button color="primary" routerLink="/register">Créer un compte</button>
        </div>
      </mat-card>
    </section>
  `,
  styles: [`
    .verify-shell {
      max-width: 520px;
      margin: 0 auto;
      padding: 3rem 1rem;
    }
    mat-card {
      padding: 2.5rem;
      border-radius: 1.4rem;
      box-shadow: 0 30px 70px rgba(15, 23, 42, 0.12);
      text-align: center;
    }
    .verify-status {
      display: grid;
      gap: 1.25rem;
      align-items: center;
      justify-items: center;
    }
    h1 {
      margin: 0;
      font-size: 2rem;
      color: #0b4a94;
    }
    p {
      margin: 0;
      color: #475569;
      max-width: 32rem;
      line-height: 1.65;
    }
    mat-icon {
      font-size: 4rem;
    }
  `]
})
export class VerifyPage {
  loading = true;
  success = false;
  errorMessage = '';

  constructor(
    private activatedRoute: ActivatedRoute,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    const token = this.activatedRoute.snapshot.queryParamMap.get('token');
    if (!token) {
      this.handleError('Le lien de vérification est invalide ou incomplet.');
      return;
    }

    this.authService.verifyAccount(token).subscribe({
      next: () => {
        this.success = true;
      },
      error: (error) => {
        this.handleError(error?.error?.message || 'Impossible de vérifier votre compte.');
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  private handleError(message: string) {
    this.loading = false;
    this.success = false;
    this.errorMessage = message;
    this.snackBar.open(message, 'Fermer', { duration: 6000 });
  }
}
