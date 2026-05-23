import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginPage implements OnInit {
  view: 'login' | 'forgot' = 'login';

  loginForm: FormGroup;
  forgotForm: FormGroup;

  isLoading = false;
  isForgotLoading = false;
  showPassword = false;
  forgotSuccess = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {
    this.loginForm = this.fb.group({
      email:    ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.forgotForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['expired']) {
        this.snackBar.open('Votre session a expiré. Veuillez vous reconnecter.', 'Fermer', {
          duration: 6000,
        });
      }
    });
  }

  onSubmit(): void {
    if (!this.loginForm.valid) return;
    this.isLoading = true;
    const { email, password } = this.loginForm.value;

    this.authService.login({ email, password }).subscribe({
      next: () => {
        this.isLoading = false;
        this.snackBar.open('OTP envoyé. Vérifiez votre email.', 'Fermer', { duration: 4000 });
        this.router.navigate(['/auth/otp'], { queryParams: { email } });
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        const msg = err.error?.message || 'Identifiants incorrects. Veuillez réessayer.';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      }
    });
  }

  onForgot(): void {
    if (!this.forgotForm.valid) return;
    this.isForgotLoading = true;
    const { email } = this.forgotForm.value;

    this.authService.forgotPassword({ email }).pipe(
      finalize(() => { this.isForgotLoading = false; })
    ).subscribe({
      next: () => {
        this.forgotSuccess = true;
        this.snackBar.open('Lien envoyé. Vérifiez votre email.', 'Fermer', { duration: 5000 });
      },
      error: (err: HttpErrorResponse) => {
        const msg = err.error?.message || 'Une erreur est survenue. Veuillez réessayer.';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      }
    });
  }
}