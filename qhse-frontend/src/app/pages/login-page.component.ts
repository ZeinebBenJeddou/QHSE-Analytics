import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../core/services/auth.service';
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
  template: `
    <div class="page-wrapper">

      <div class="bg-grid"></div>

      <div class="card">

        
       <div class="logo">
           <!--<div class="logo-icon">
            <svg width="20" height="20" viewBox="0 0 28 28" fill="none">
              <path d="M14 2L24 8V20L14 26L4 20V8L14 2Z" stroke="white" stroke-width="2" fill="none"/>
              <circle cx="14" cy="14" r="3" fill="white"/>
            </svg>
          </div>
          <div class="logo-sep"></div>-->
          <span class="logo-name">QHSE <strong>Analytics</strong></span>
        </div>

        <!-- LOGIN VIEW -->
        <ng-container *ngIf="view === 'login'">

          <div class="form-header">
            
            <p class="form-subtitle">    Connectez-vous à votre espace </p>
          </div>

          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="form" novalidate>

            <div class="field-group">
              <label class="field-label">Adresse email</label>
              <div class="input-wrap" [class.input-error]="loginForm.get('email')?.invalid && loginForm.get('email')?.touched">
                <svg class="input-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
                <input
                  class="field-input"
                  type="email"
                  formControlName="email"
                  placeholder="exemple@entreprise.com"
                  autocomplete="email"
                />
              </div>
              <span class="field-error" *ngIf="loginForm.get('email')?.invalid && loginForm.get('email')?.touched">
                Veuillez saisir un email valide
              </span>
            </div>

            <div class="field-group">
              <div class="label-row">
                <label class="field-label">Mot de passe</label>
                <button type="button" class="forgot-link" (click)="view = 'forgot'">Mot de passe oublié ?</button>
              </div>
              <div class="input-wrap" [class.input-error]="loginForm.get('password')?.invalid && loginForm.get('password')?.touched">
                <svg class="input-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0110 0v4"/>
                </svg>
                <input
                  class="field-input"
                  [type]="showPassword ? 'text' : 'password'"
                  formControlName="password"
                  placeholder="••••••••"
                  autocomplete="current-password"
                />
                <button type="button" class="eye-btn" (click)="showPassword = !showPassword">
                  <svg *ngIf="!showPassword" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                    <circle cx="12" cy="12" r="3"/>
                  </svg>
                  <svg *ngIf="showPassword" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/>
                    <line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                </button>
              </div>
              <span class="field-error" *ngIf="loginForm.get('password')?.invalid && loginForm.get('password')?.touched">
               Veuillez saisir un mot de passe valide
              </span>
            </div>

            <button class="btn-submit" type="submit" [disabled]="!loginForm.valid || isLoading">
              <mat-spinner diameter="17" *ngIf="isLoading"></mat-spinner>
              <ng-container *ngIf="!isLoading">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5">
                  <path d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4"/>
                  <polyline points="10 17 15 12 10 7"/>
                  <line x1="15" y1="12" x2="3" y2="12"/>
                </svg>
                Se connecter
              </ng-container>
              <span *ngIf="isLoading">Connexion en cours…</span>
            </button>

          </form>

          <div class="form-footer">
            Pas encore de compte ? <a routerLink="/register" class="link">Créer un compte</a>
          </div>

          <div class="security-note">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
            Connexion sécurisée 
          </div>

        </ng-container>

        <!-- FORGOT PASSWORD VIEW -->
        <ng-container *ngIf="view === 'forgot'">

          <div class="form-header">
            <button class="back-btn" (click)="view = 'login'; forgotSuccess = false">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M19 12H5M12 5l-7 7 7 7"/>
              </svg>
              Retour
            </button>
            <h1 class="form-title" style="margin-top: 1rem;">Mot de passe oublié</h1>
            <p class="form-subtitle">Saisissez votre email pour recevoir un lien de réinitialisation.</p>
          </div>

          
          <div class="success-box" *ngIf="forgotSuccess">
            <div class="success-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#38A169" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <h3>Email envoyé !</h3>
            <p>Vérifiez votre boîte de réception à l'adresse <strong>{{ forgotForm.get('email')?.value }}</strong>. Le lien expire dans 15 minutes.</p>
            <button class="btn-back" (click)="view = 'login'; forgotSuccess = false">Retour à la connexion</button>
          </div>

          
          <form [formGroup]="forgotForm" (ngSubmit)="onForgot()" class="form" novalidate *ngIf="!forgotSuccess">

            <div class="field-group">
              <label class="field-label">Adresse email</label>
              <div class="input-wrap" [class.input-error]="forgotForm.get('email')?.invalid && forgotForm.get('email')?.touched">
                <svg class="input-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
                <input
                  class="field-input"
                  type="email"
                  formControlName="email"
                  placeholder="exemple@entreprise.com"
                  autocomplete="email"
                />
              </div>
              <span class="field-error" *ngIf="forgotForm.get('email')?.invalid && forgotForm.get('email')?.touched">
                Veuillez saisir un email valide
              </span>
            </div>

            <button class="btn-submit" type="submit" [disabled]="!forgotForm.valid || isForgotLoading">
              <mat-spinner diameter="17" *ngIf="isForgotLoading"></mat-spinner>
              <ng-container *ngIf="!isForgotLoading">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5">
                  <line x1="22" y1="2" x2="11" y2="13"/>
                  <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                </svg>
                Envoyer le lien
              </ng-container>
              <span *ngIf="isForgotLoading">Envoi en cours…</span>
            </button>

          </form>

        </ng-container>

      </div>
    </div>
  `,
  styles: [`
    * { box-sizing: border-box; margin: 0; padding: 0; }

    .page-wrapper {
      display: flex;
      min-height: 100vh;
      align-items: center;
      justify-content: center;
      background: #F4F7FF;
      font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
      padding: 2rem 1rem;
      position: relative;
    }

    .bg-grid {
      position: fixed;
      inset: 0;
      background-image:
        linear-gradient(rgba(30,111,217,0.04) 1px, transparent 1px),
        linear-gradient(90deg, rgba(30,111,217,0.04) 1px, transparent 1px);
      background-size: 48px 48px;
      pointer-events: none;
      z-index: 0;
    }

    
    .card {
      position: relative;
      z-index: 1;
      width: 100%;
      max-width: 420px;
      background: white;
      border-radius: 20px;
      padding: 2.5rem 2.25rem;
      box-shadow:
        0 0 0 1px rgba(30,111,217,0.07),
        0 8px 32px rgba(30,111,217,0.09),
        0 2px 8px rgba(0,0,0,0.04);
      animation: fadeUp 0.5s ease both;
    }

    @keyframes fadeUp {
      from { opacity: 0; transform: translateY(16px); }
      to   { opacity: 1; transform: translateY(0); }
    }

   
    .logo {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      margin-bottom: 2rem;
    }
    .logo-icon {
      width: 36px; height: 36px;
      background: #1E6FD9;
      border-radius: 9px;
      display: flex; align-items: center; justify-content: center;
    }
    .logo-sep {
      width: 1px; height: 20px;
      background: #E2EAF6;
    }
    .logo-name {
      font-size: 0.95rem;
      color: #0D1B3E;
      font-weight: 400;
      letter-spacing: -0.01em;
    }
    .logo-name strong { color: #1E6FD9; font-weight: 600; }

  
    .form-header { margin-bottom: 1.75rem; }
    .form-title {
      font-size: 1.45rem;
      font-weight: 700;
      color: #0D1B3E;
      letter-spacing: -0.025em;
      margin-bottom: 0.3rem;
    }
    .form-subtitle {
      font-size: 0.855rem;
      color: #718096;
      line-height: 1.55;
    }

   
    .back-btn {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      background: none;
      border: none;
      cursor: pointer;
      font-size: 0.82rem;
      font-weight: 600;
      color: #1E6FD9;
      padding: 0;
      transition: gap 0.2s;
    }
    .back-btn:hover { gap: 7px; }

   
    .form { display: flex; flex-direction: column; gap: 1rem; }
    .field-group { display: flex; flex-direction: column; gap: 5px; }

    .label-row { display: flex; justify-content: space-between; align-items: center; }
    .field-label { font-size: 0.8rem; font-weight: 600; color: #2D3748; }

    .forgot-link {
      background: none; border: none; cursor: pointer;
      font-size: 0.78rem; font-weight: 500; color: #1E6FD9; padding: 0;
    }
    .forgot-link:hover { text-decoration: underline; }

    .input-wrap {
      display: flex;
      align-items: center;
      background: #F8FAFF;
      border: 1.5px solid #E2EAF6;
      border-radius: 10px;
      transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
    }
    .input-wrap:focus-within {
      border-color: #1E6FD9;
      background: white;
      box-shadow: 0 0 0 3px rgba(30,111,217,0.09);
    }
    .input-wrap.input-error { border-color: #FC8181; }
    .input-wrap.input-error:focus-within { box-shadow: 0 0 0 3px rgba(229,62,62,0.09); }

    .input-icon { flex-shrink: 0; margin-left: 0.8rem; color: #A0AEC0; }
    .field-input {
      flex: 1;
      border: none; background: transparent;
      padding: 0.72rem 0.5rem 0.72rem 0.6rem;
      font-size: 0.875rem; color: #0D1B3E;
      outline: none; font-family: inherit;
    }
    .field-input::placeholder { color: #A0AEC0; }

    .eye-btn {
      background: none; border: none; cursor: pointer;
      padding: 0.5rem 0.75rem; color: #A0AEC0;
      display: flex; align-items: center; transition: color 0.2s;
    }
    .eye-btn:hover { color: #1E6FD9; }

    .field-error { font-size: 0.74rem; color: #E53E3E; font-weight: 500; }

    .btn-submit {
      width: 100%;
      padding: 0.8rem;
      background: #1E6FD9;
      color: white;
      border: none;
      border-radius: 10px;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      display: flex; align-items: center; justify-content: center; gap: 7px;
      margin-top: 0.4rem;
      transition: background 0.2s, transform 0.2s, box-shadow 0.2s;
      box-shadow: 0 2px 10px rgba(30,111,217,0.25);
    }
    .btn-submit:hover:not(:disabled) {
      background: #1558B0;
      transform: translateY(-1px);
      box-shadow: 0 4px 16px rgba(30,111,217,0.35);
    }
    .btn-submit:disabled { opacity: 0.55; cursor: not-allowed; transform: none; }

    
    .form-footer {
      text-align: center;
      margin-top: 1.4rem;
      font-size: 0.83rem;
      color: #718096;
    }
    .link { color: #1E6FD9; font-weight: 600; text-decoration: none; }
    .link:hover { text-decoration: underline; }

    .security-note {
      display: flex; align-items: center; justify-content: center; gap: 5px;
      margin-top: 1.1rem;
      font-size: 0.71rem;
      color: #A0AEC0;
    }

  
    .success-box {
      display: flex; flex-direction: column; align-items: center;
      text-align: center; gap: 0.75rem;
      padding: 1.5rem;
      background: #F0FFF4;
      border: 1px solid #C6F6D5;
      border-radius: 12px;
    }
    .success-icon {
      width: 48px; height: 48px;
      background: #C6F6D5; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
    }
    .success-box h3 { font-size: 1rem; font-weight: 700; color: #22543D; }
    .success-box p { font-size: 0.84rem; color: #276749; line-height: 1.6; }
    .btn-back {
      padding: 0.55rem 1.4rem;
      background: #38A169; color: white;
      border: none; border-radius: 8px;
      font-size: 0.855rem; font-weight: 600;
      cursor: pointer; margin-top: 0.25rem;
      transition: background 0.2s;
    }
    .btn-back:hover { background: #2F855A; }

    @media (max-width: 480px) {
      .card { padding: 2rem 1.5rem; }
    }
  `]
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

  ngOnInit(): void {}

  onSubmit(): void {
    if (!this.loginForm.valid) return;
    this.isLoading = true;
    const { email, password } = this.loginForm.value;

    this.authService.login(email, password).subscribe({
      next: () => {
        this.isLoading = false;
        this.snackBar.open('OTP envoyé. Vérifiez votre email.', 'Fermer', { duration: 4000 });
        this.router.navigate(['/verify-otp'], { queryParams: { email } });
      },
      error: (err: any) => {
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

    this.authService.forgotPassword(email).pipe(
      finalize(() => { this.isForgotLoading = false; })
    ).subscribe({
      next: () => {
        this.forgotSuccess = true;
        this.snackBar.open('Lien envoyé. Vérifiez votre email.', 'Fermer', { duration: 5000 });
      },
      error: (err: any) => {
        const msg = err.error?.message || 'Une erreur est survenue. Veuillez réessayer.';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      }
    });
  }
}