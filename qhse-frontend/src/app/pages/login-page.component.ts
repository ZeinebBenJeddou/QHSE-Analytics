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

      <!-- Background -->
      <div class="bg-grid"></div>
      <div class="bg-orb orb-1"></div>
      <div class="bg-orb orb-2"></div>

      <!-- Left panel (branding) -->
      <div class="left-panel">
        <div class="left-content">
          <a routerLink="/" class="brand">
            <div class="brand-icon">
              <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                <path d="M14 2L24 8V20L14 26L4 20V8L14 2Z" stroke="white" stroke-width="2" fill="none"/>
                <path d="M14 7L20 10.5V17.5L14 21L8 17.5V10.5L14 7Z" fill="white" opacity="0.3"/>
                <circle cx="14" cy="14" r="3" fill="white"/>
              </svg>
            </div>
            <span class="brand-name">QHSE <strong>Analytics</strong></span>
          </a>

          <div class="left-hero">
            <h2 class="left-title">Pilotez votre performance<br>QHSE avec l'IA</h2>
            <p class="left-desc">
              Analysez vos indicateurs, détectez les dérives et générez
              des synthèses intelligentes en quelques secondes.
            </p>
          </div>

          <div class="features-list">
            <div class="feature-item" *ngFor="let f of sideFeatures">
              <div class="feat-check">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
              </div>
              <span>{{ f }}</span>
            </div>
          </div>

          <div class="left-footer">
            <div class="avatar-stack">
              <div class="avatar av-1">Q</div>
              <div class="avatar av-2">A</div>
              <div class="avatar av-3">M</div>
            </div>
            <p>Plateforme utilisée par des équipes QHSE professionnelles</p>
          </div>
        </div>
      </div>

      <!-- Right panel (form) -->
      <div class="right-panel">
        <div class="form-box">

          <!-- LOGIN VIEW -->
          <ng-container *ngIf="view === 'login'">
            <div class="form-header">
              <div class="form-badge">Espace sécurisé</div>
              <h1 class="form-title">Bon retour</h1>
              <p class="form-subtitle">Connectez-vous à votre espace QHSE Analytics</p>
            </div>

            <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="form" novalidate>

              <div class="field-group">
                <label class="field-label">Adresse email</label>
                <div class="input-wrap" [class.input-error]="loginForm.get('email')?.invalid && loginForm.get('email')?.touched">
                  <svg class="input-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
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
                  <svg class="input-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
                  <input
                    class="field-input"
                    [type]="showPassword ? 'text' : 'password'"
                    formControlName="password"
                    placeholder="••••••••"
                    autocomplete="current-password"
                  />
                  <button type="button" class="eye-btn" (click)="showPassword = !showPassword">
                    <svg *ngIf="!showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    <svg *ngIf="showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                  </button>
                </div>
                <span class="field-error" *ngIf="loginForm.get('password')?.invalid && loginForm.get('password')?.touched">
                  Minimum 6 caractères requis
                </span>
              </div>

              <button class="btn-submit" type="submit" [disabled]="!loginForm.valid || isLoading">
                <mat-spinner diameter="18" *ngIf="isLoading"></mat-spinner>
                <span *ngIf="!isLoading">
                  <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
                  Se connecter
                </span>
                <span *ngIf="isLoading">Connexion en cours…</span>
              </button>

            </form>

            <div class="form-footer">
              <p>Pas encore de compte ? <a routerLink="/register" class="link">Créer un compte</a></p>
            </div>

            <div class="security-note">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#718096" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
              <span>Connexion sécurisée par chiffrement JWT + vérification OTP</span>
            </div>
          </ng-container>

          <!-- FORGOT PASSWORD VIEW -->
          <ng-container *ngIf="view === 'forgot'">
            <div class="form-header">
              <button class="back-btn" (click)="view = 'login'; forgotSuccess = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 5l-7 7 7 7"/></svg>
                Retour
              </button>
              <div class="forgot-icon-wrap">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="1.8"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/><circle cx="12" cy="16" r="1" fill="#1E6FD9"/></svg>
              </div>
              <h1 class="form-title">Mot de passe oublié</h1>
              <p class="form-subtitle">
                Saisissez votre adresse email et nous vous enverrons un lien de réinitialisation.
              </p>
            </div>

            <!-- Success state -->
            <div class="success-box" *ngIf="forgotSuccess">
              <div class="success-icon">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#38A169" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              </div>
              <h3>Email envoyé !</h3>
              <p>Vérifiez votre boîte de réception à l'adresse <strong>{{ forgotForm.get('email')?.value }}</strong>. Le lien expire dans 15 minutes.</p>
              <button class="btn-back" (click)="view = 'login'; forgotSuccess = false">Retour à la connexion</button>
            </div>

            <!-- Forgot form -->
            <form [formGroup]="forgotForm" (ngSubmit)="onForgot()" class="form" novalidate *ngIf="!forgotSuccess">

              <div class="field-group">
                <label class="field-label">Adresse email</label>
                <div class="input-wrap" [class.input-error]="forgotForm.get('email')?.invalid && forgotForm.get('email')?.touched">
                  <svg class="input-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
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
                <mat-spinner diameter="18" *ngIf="isForgotLoading"></mat-spinner>
                <span *ngIf="!isForgotLoading">
                  <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
                  Envoyer le lien
                </span>
                <span *ngIf="isForgotLoading">Envoi en cours…</span>
              </button>

            </form>
          </ng-container>

        </div>
      </div>

    </div>
  `,
  styles: [`
    * { box-sizing: border-box; margin: 0; padding: 0; }

    .page-wrapper {
      display: flex;
      min-height: 100vh;
      background: #F4F7FF;
      font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
      position: relative;
      overflow: hidden;
    }

    /* ── BACKGROUND ── */
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
    .bg-orb {
      position: fixed;
      border-radius: 50%;
      filter: blur(80px);
      pointer-events: none;
      z-index: 0;
      animation: floatOrb 12s ease-in-out infinite;
    }
    .orb-1 { width: 400px; height: 400px; background: rgba(30,111,217,0.12); top: -100px; right: 20%; animation-delay: 0s; }
    .orb-2 { width: 300px; height: 300px; background: rgba(100,180,255,0.09); bottom: 5%; left: 30%; animation-delay: -5s; }
    @keyframes floatOrb {
      0%, 100% { transform: translate(0, 0); }
      50% { transform: translate(15px, -25px); }
    }

    /* ── LEFT PANEL ── */
    .left-panel {
      position: relative;
      z-index: 1;
      width: 46%;
      background: linear-gradient(160deg, #0D1B3E 0%, #1E3A5F 50%, #1E6FD9 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 3rem 3.5rem;
      overflow: hidden;
    }
    .left-panel::before {
      content: '';
      position: absolute;
      inset: 0;
      background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none'%3E%3Cg fill='%23ffffff' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
      pointer-events: none;
    }
    .left-content {
      position: relative;
      display: flex;
      flex-direction: column;
      gap: 2.5rem;
      max-width: 400px;
      width: 100%;
      animation: fadeSlideIn 0.7s ease both;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      text-decoration: none;
    }
    .brand-icon {
      width: 42px; height: 42px;
      background: rgba(255,255,255,0.12);
      border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      border: 1px solid rgba(255,255,255,0.2);
    }
    .brand-name {
      font-size: 1.1rem;
      color: white;
      letter-spacing: -0.02em;
    }
    .brand-name strong { color: #7CC3FF; }

    .left-hero { display: flex; flex-direction: column; gap: 0.75rem; }
    .left-title {
      font-size: 1.85rem;
      font-weight: 800;
      color: white;
      line-height: 1.25;
      letter-spacing: -0.03em;
    }
    .left-desc {
      font-size: 0.9rem;
      color: rgba(255,255,255,0.65);
      line-height: 1.65;
    }

    .features-list { display: flex; flex-direction: column; gap: 0.75rem; }
    .feature-item {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      font-size: 0.875rem;
      color: rgba(255,255,255,0.85);
      font-weight: 500;
    }
    .feat-check {
      flex-shrink: 0;
      width: 22px; height: 22px;
      background: rgba(255,255,255,0.12);
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
    }

    .left-footer {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding-top: 1rem;
      border-top: 1px solid rgba(255,255,255,0.1);
    }
    .avatar-stack { display: flex; }
    .avatar {
      width: 30px; height: 30px;
      border-radius: 50%;
      border: 2px solid rgba(255,255,255,0.3);
      display: flex; align-items: center; justify-content: center;
      font-size: 0.7rem; font-weight: 700; color: white;
      margin-right: -8px;
    }
    .av-1 { background: #2B8AFF; }
    .av-2 { background: #38A169; }
    .av-3 { background: #E53E3E; }
    .left-footer p { font-size: 0.75rem; color: rgba(255,255,255,0.5); margin-left: 16px; }

    /* ── RIGHT PANEL ── */
    .right-panel {
      position: relative;
      z-index: 1;
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2.5rem 2rem;
    }

    .form-box {
      width: 100%;
      max-width: 420px;
      background: white;
      border-radius: 20px;
      padding: 2.5rem 2.25rem;
      box-shadow: 0 16px 48px rgba(30,111,217,0.10), 0 4px 16px rgba(0,0,0,0.06);
      border: 1px solid rgba(30,111,217,0.08);
      animation: fadeSlideIn 0.6s 0.1s ease both;
    }

    @keyframes fadeSlideIn {
      from { opacity: 0; transform: translateY(20px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    /* ── FORM HEADER ── */
    .form-header {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      margin-bottom: 2rem;
    }
    .form-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.25rem 0.75rem;
      background: rgba(30,111,217,0.07);
      border: 1px solid rgba(30,111,217,0.15);
      border-radius: 100px;
      font-size: 0.72rem;
      font-weight: 600;
      color: #1E6FD9;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      width: fit-content;
      margin-bottom: 0.5rem;
    }
    .form-title {
      font-size: 1.65rem;
      font-weight: 800;
      color: #0D1B3E;
      letter-spacing: -0.03em;
    }
    .form-subtitle {
      font-size: 0.875rem;
      color: #718096;
      line-height: 1.55;
    }

    /* ── BACK BUTTON ── */
    .back-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      background: none;
      border: none;
      cursor: pointer;
      font-size: 0.83rem;
      font-weight: 600;
      color: #1E6FD9;
      padding: 0.3rem 0;
      margin-bottom: 0.5rem;
      transition: gap 0.2s;
    }
    .back-btn:hover { gap: 0.6rem; }

    .forgot-icon-wrap {
      width: 52px; height: 52px;
      background: rgba(30,111,217,0.07);
      border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
      margin-bottom: 0.25rem;
    }

    /* ── FORM FIELDS ── */
    .form { display: flex; flex-direction: column; gap: 1.1rem; }

    .field-group { display: flex; flex-direction: column; gap: 0.4rem; }

    .label-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .field-label {
      font-size: 0.83rem;
      font-weight: 600;
      color: #2D3748;
    }
    .forgot-link {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 0.8rem;
      font-weight: 600;
      color: #1E6FD9;
      padding: 0;
      transition: color 0.2s;
    }
    .forgot-link:hover { color: #1558B0; text-decoration: underline; }

    .input-wrap {
      position: relative;
      display: flex;
      align-items: center;
      background: #F8FAFF;
      border: 1.5px solid #E2EAF6;
      border-radius: 10px;
      transition: all 0.2s;
    }
    .input-wrap:focus-within {
      border-color: #1E6FD9;
      background: white;
      box-shadow: 0 0 0 3px rgba(30,111,217,0.10);
    }
    .input-wrap.input-error { border-color: #E53E3E; }
    .input-wrap.input-error:focus-within { box-shadow: 0 0 0 3px rgba(229,62,62,0.10); }

    .input-icon {
      flex-shrink: 0;
      margin-left: 0.85rem;
      color: #A0AEC0;
    }
    .field-input {
      flex: 1;
      border: none;
      background: transparent;
      padding: 0.75rem 0.85rem;
      font-size: 0.9rem;
      color: #0D1B3E;
      outline: none;
      font-family: inherit;
    }
    .field-input::placeholder { color: #A0AEC0; }

    .eye-btn {
      background: none;
      border: none;
      cursor: pointer;
      padding: 0.5rem 0.75rem;
      color: #A0AEC0;
      display: flex;
      align-items: center;
      transition: color 0.2s;
    }
    .eye-btn:hover { color: #1E6FD9; }

    .field-error {
      font-size: 0.75rem;
      color: #E53E3E;
      font-weight: 500;
    }

    /* ── SUBMIT BUTTON ── */
    .btn-submit {
      margin-top: 0.5rem;
      width: 100%;
      padding: 0.85rem;
      background: linear-gradient(135deg, #1E6FD9, #2B8AFF);
      color: white;
      border: none;
      border-radius: 10px;
      font-size: 0.95rem;
      font-weight: 700;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.6rem;
      box-shadow: 0 4px 18px rgba(30,111,217,0.35);
      transition: all 0.25s;
    }
    .btn-submit:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(30,111,217,0.45);
    }
    .btn-submit:disabled {
      opacity: 0.6;
      cursor: not-allowed;
      transform: none;
    }

    /* ── FORM FOOTER ── */
    .form-footer {
      text-align: center;
      margin-top: 1.5rem;
      font-size: 0.85rem;
      color: #718096;
    }
    .link {
      color: #1E6FD9;
      font-weight: 600;
      text-decoration: none;
    }
    .link:hover { text-decoration: underline; }

    .security-note {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.4rem;
      margin-top: 1.25rem;
      font-size: 0.73rem;
      color: #A0AEC0;
    }

    /* ── SUCCESS BOX ── */
    .success-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 0.75rem;
      padding: 1.5rem;
      background: #F0FFF4;
      border: 1px solid #C6F6D5;
      border-radius: 12px;
    }
    .success-icon {
      width: 52px; height: 52px;
      background: #C6F6D5;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
    }
    .success-box h3 { font-size: 1.1rem; font-weight: 700; color: #22543D; }
    .success-box p { font-size: 0.85rem; color: #276749; line-height: 1.6; }
    .btn-back {
      margin-top: 0.5rem;
      padding: 0.6rem 1.4rem;
      background: #38A169;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn-back:hover { background: #2F855A; }

    /* ── RESPONSIVE ── */
    @media (max-width: 860px) {
      .left-panel { display: none; }
      .right-panel { padding: 1.5rem 1rem; }
      .form-box { padding: 2rem 1.5rem; }
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

  sideFeatures = [
    'Comparaison automatique N-1 vs N',
    'Analyse IA des variations QHSE',
    'Détection des signaux critiques',
    'Export PDF & rapports personnalisés',
  ];

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
      finalize(() => {
        this.isForgotLoading = false;
      })
    ).subscribe({
      next: () => {
        this.forgotSuccess = true;
        this.snackBar.open('Lien de réinitialisation envoyé. Vérifiez votre email.', 'Fermer', { duration: 5000 });
      },
      error: (err: any) => {
        const msg = err.error?.message || 'Une erreur est survenue. Veuillez réessayer.';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      }
    });
  }
}