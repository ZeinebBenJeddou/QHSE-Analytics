import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-verify-account',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatCardModule],
  templateUrl: './verify-account.component.html',
  styleUrls: ['./verify-account.component.css']
})
export class VerifyAccountComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  statusMessage = 'Verifying your account...';
  isError = false;

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const token = params.get('token');

      if (!token) {
        this.handleError('Missing verification token.');
        return;
      }

      this.authService.verifyAccount(token).subscribe({
        next: (response) => {
          this.isError = false;
          this.statusMessage = response.message || 'Account verified successfully.';
          this.snackBar.open(this.statusMessage, 'Fermer', { duration: 3000 });
          setTimeout(() => this.router.navigate(['/auth/login']), 2500);
        },
        error: (error) => {
          this.handleError(error?.error?.message || 'Unable to verify account.');
        }
      });
    });
  }

  private handleError(message: string): void {
    this.isError = true;
    this.statusMessage = message;
    this.snackBar.open(message, 'Close', { duration: 4000 });
  }
}
