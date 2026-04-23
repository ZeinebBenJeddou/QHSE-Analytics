import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../core/services/auth.service';
import { RegisterRequest } from '../shared/models/auth-requests';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [
    CommonModule,
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
    <div class="page-wrapper">

      <!-- Animated background -->
      <div class="bg-grid"></div>
      <div class="bg-orb orb-1"></div>
      <div class="bg-orb orb-2"></div>
      <div class="bg-orb orb-3"></div>

      <!-- Left panel — branding -->
      <div class="left-panel">
        <div class="left-inner">

          <div class="brand">
            <div class="brand-icon">
              <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                <path d="M14 2L24 8V20L14 26L4 20V8L14 2Z" stroke="#1E6FD9" stroke-width="2" fill="none"/>
                <path d="M14 7L20 10.5V17.5L14 21L8 17.5V10.5L14 7Z" fill="#1E6FD9" opacity="0.3"/>
                <circle cx="14" cy="14" r="3" fill="#1E6FD9"/>
              </svg>
            </div>
            <span class="brand-name">QHSE <strong>Analytics</strong></span>
          </div>

          <div class="left-copy">
            <div class="hero-badge">
              <span class="badge-dot"></span>
              Plateforme IA nouvelle génération
            </div>
            <h2 class="left-title">
              Rejoignez la plateforme<br>
              <span class="title-accent">QHSE intelligente</span>
            </h2>
            <p class="left-desc">
              Analysez vos indicateurs, détectez les dérives critiques et générez des synthèses grâce à l'IA.
            </p>
          </div>

          <div class="trust-list">
            <div class="trust-item">
              <div class="trust-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              </div>
              <span>Sécurité JWT & 2FA</span>
            </div>
            <div class="trust-item">
              <div class="trust-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              </div>
              <span>Analyse en temps réel</span>
            </div>
            <div class="trust-item">
              <div class="trust-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              </div>
              <span>Export PDF / Excel</span>
            </div>
          </div>

        </div>
      </div>

      <!-- Right panel — form -->
      <div class="right-panel">
        <div class="form-card">

          <div class="form-header">
            <h1 class="form-title">Créer un compte</h1>
            <p class="form-subtitle">Remplissez le formulaire pour commencer</p>
          </div>

          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="register-form" novalidate>

            <!-- Nom & Prénom row -->
            <div class="field-row">
              <div class="field-group">
                <label class="field-label">Nom</label>
                <div class="input-wrap" [class.input-error]="registerForm.get('nom')?.invalid && registerForm.get('nom')?.touched">
                  <mat-icon class="input-icon">person</mat-icon>
                  <input matInput formControlName="nom" placeholder="Votre nom" class="qhse-input">
                </div>
                <span class="error-msg" *ngIf="registerForm.get('nom')?.hasError('required') && registerForm.get('nom')?.touched">
                  Le nom est requis
                </span>
              </div>

              <div class="field-group">
                <label class="field-label">Prénom</label>
                <div class="input-wrap" [class.input-error]="registerForm.get('prenom')?.invalid && registerForm.get('prenom')?.touched">
                  <mat-icon class="input-icon">person_outline</mat-icon>
                  <input matInput formControlName="prenom" placeholder="Votre prénom" class="qhse-input">
                </div>
                <span class="error-msg" *ngIf="registerForm.get('prenom')?.hasError('required') && registerForm.get('prenom')?.touched">
                  Le prénom est requis
                </span>
              </div>
            </div>

            <!-- Email -->
            <div class="field-group">
              <label class="field-label">Adresse e-mail</label>
              <div class="input-wrap" [class.input-error]="registerForm.get('email')?.invalid && registerForm.get('email')?.touched">
                <mat-icon class="input-icon">email</mat-icon>
                <input matInput formControlName="email" type="email" placeholder="votre.email@exemple.com" class="qhse-input">
              </div>
              <span class="error-msg" *ngIf="registerForm.get('email')?.hasError('required') && registerForm.get('email')?.touched">L'email est requis</span>
              <span class="error-msg" *ngIf="registerForm.get('email')?.hasError('email') && registerForm.get('email')?.touched">Format d'email invalide</span>
            </div>

            <!-- Password -->
            <div class="field-group">
              <label class="field-label">Mot de passe</label>
              <div class="input-wrap" [class.input-error]="registerForm.get('password')?.invalid && registerForm.get('password')?.touched">
                <mat-icon class="input-icon">lock</mat-icon>
                <input matInput formControlName="password"
                       [type]="showPassword ? 'text' : 'password'"
                       placeholder="8 caractères minimum"
                       class="qhse-input">
                <button type="button" class="toggle-pw" (click)="showPassword = !showPassword">
                  <mat-icon>{{ showPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
              </div>
              <span class="error-msg" *ngIf="registerForm.get('password')?.hasError('required') && registerForm.get('password')?.touched">Le mot de passe est requis</span>
              <span class="error-msg" *ngIf="registerForm.get('password')?.hasError('minlength') && registerForm.get('password')?.touched">8 caractères minimum</span>
            </div>

            <!-- Confirm Password -->
            <div class="field-group">
              <label class="field-label">Confirmer le mot de passe</label>
              <div class="input-wrap" [class.input-error]="(registerForm.hasError('passwordMismatch') || registerForm.get('confirmPassword')?.hasError('required')) && registerForm.get('confirmPassword')?.touched">
                <mat-icon class="input-icon">lock_outline</mat-icon>
                <input matInput formControlName="confirmPassword"
                       [type]="showConfirm ? 'text' : 'password'"
                       placeholder="Répétez le mot de passe"
                       class="qhse-input">
                <button type="button" class="toggle-pw" (click)="showConfirm = !showConfirm">
                  <mat-icon>{{ showConfirm ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
              </div>
              <span class="error-msg" *ngIf="registerForm.get('confirmPassword')?.hasError('required') && registerForm.get('confirmPassword')?.touched">La confirmation est requise</span>
              <span class="error-msg" *ngIf="registerForm.hasError('passwordMismatch') && registerForm.get('confirmPassword')?.touched">Les mots de passe ne correspondent pas</span>
            </div>

            <!-- Submit -->
            <button type="submit" class="btn-submit" [disabled]="registerForm.invalid || isLoading">
              <mat-progress-spinner diameter="18" mode="indeterminate" *ngIf="isLoading" class="white-spinner"></mat-progress-spinner>
              <ng-container *ngIf="!isLoading">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
                Créer mon compte
              </ng-container>
            </button>

          </form>

          <div class="form-footer">
            <p>Déjà un compte ?
              <button class="link-btn" (click)="goToLogin()">Se connecter</button>
            </p>
          </div>

        </div>
      </div>

    </div>
  `,
  styles: [`
    /* ── RESET ── */
    * { box-sizing: border-box; margin: 0; padding: 0; }

    /* ── LAYOUT ── */
    .page-wrapper {
      min-height: 100vh;
      display: grid;
      grid-template-columns: 1fr 1fr;
      background: #F4F7FF;
      font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
      color: #0D1B3E;
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
    .orb-1 { width:500px; height:500px; background:rgba(30,111,217,0.10); top:-150px; right:-100px; animation-delay:0s; }
    .orb-2 { width:350px; height:350px; background:rgba(100,180,255,0.08); bottom:10%; left:-80px; animation-delay:-4s; }
    .orb-3 { width:280px; height:280px; background:rgba(30,111,217,0.06); top:40%; left:35%; animation-delay:-8s; }
    @keyframes floatOrb {
      0%,100% { transform:translate(0,0) scale(1); }
      50%      { transform:translate(20px,-30px) scale(1.05); }
    }

    /* ── LEFT PANEL ── */
    .left-panel {
      position: relative;
      z-index: 1;
      background: linear-gradient(160deg, #0D1B3E 0%, #1E3A5F 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 3rem 3.5rem;
      overflow: hidden;
    }
    /* subtle grid overlay on left panel */
    .left-panel::before {
      content: '';
      position: absolute;
      inset: 0;
      background-image:
        linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
      background-size: 40px 40px;
      pointer-events: none;
    }

    .left-inner {
      position: relative;
      z-index: 1;
      display: flex;
      flex-direction: column;
      gap: 2.5rem;
      max-width: 380px;
      animation: fadeSlideIn 0.6s ease both;
    }

    /* Brand */
    .brand {
      display: flex;
      align-items: center;
      gap: 0.65rem;
    }
    .brand-icon {
      width: 44px; height: 44px;
      background: rgba(30,111,217,0.2);
      border: 1px solid rgba(30,111,217,0.35);
      border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
    }
    .brand-name {
      font-size: 1.1rem;
      letter-spacing: -0.02em;
      color: rgba(255,255,255,0.9);
    }
    .brand-name strong { color: #5BA4F5; }

    /* Hero badge */
    .hero-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.35rem 0.9rem;
      background: rgba(30,111,217,0.2);
      border: 1px solid rgba(30,111,217,0.35);
      border-radius: 100px;
      font-size: 0.75rem;
      font-weight: 600;
      color: #7CC3FF;
      letter-spacing: 0.03em;
      width: fit-content;
      margin-bottom: 0.5rem;
    }
    .badge-dot {
      width: 7px; height: 7px;
      background: #5BA4F5;
      border-radius: 50%;
      animation: pulse 2s ease-in-out infinite;
    }
    @keyframes pulse {
      0%,100% { opacity:1; transform:scale(1); }
      50%      { opacity:0.5; transform:scale(0.8); }
    }

    .left-title {
      font-size: clamp(1.6rem, 2.2vw, 2rem);
      font-weight: 800;
      line-height: 1.25;
      letter-spacing: -0.03em;
      color: white;
    }
    .title-accent {
      background: linear-gradient(135deg, #5BA4F5 0%, #7CC3FF 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    .left-desc {
      font-size: 0.9rem;
      line-height: 1.7;
      color: rgba(255,255,255,0.55);
      margin-top: 0.75rem;
    }

    /* Trust list */
    .trust-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .trust-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      font-size: 0.85rem;
      color: rgba(255,255,255,0.7);
      font-weight: 500;
    }
    .trust-icon {
      width: 26px; height: 26px;
      background: rgba(30,111,217,0.2);
      border-radius: 6px;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }

    /* ── RIGHT PANEL ── */
    .right-panel {
      position: relative;
      z-index: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2.5rem 2rem;
      overflow-y: auto;
    }

    .form-card {
      background: white;
      border-radius: 20px;
      border: 1px solid rgba(30,111,217,0.12);
      box-shadow: 0 8px 40px rgba(30,111,217,0.10), 0 2px 8px rgba(0,0,0,0.05);
      padding: 2.5rem 2.25rem 2rem;
      width: 100%;
      max-width: 460px;
      animation: fadeSlideIn 0.65s 0.1s ease both;
    }

    .form-header {
      margin-bottom: 2rem;
    }
    .form-title {
      font-size: 1.5rem;
      font-weight: 800;
      letter-spacing: -0.03em;
      color: #0D1B3E;
      line-height: 1.2;
    }
    .form-subtitle {
      font-size: 0.85rem;
      color: #718096;
      margin-top: 0.35rem;
    }

    /* ── FORM ── */
    .register-form {
      display: flex;
      flex-direction: column;
      gap: 1.1rem;
    }

    .field-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.9rem;
    }

    .field-group {
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
    }

    .field-label {
      font-size: 0.8rem;
      font-weight: 600;
      color: #0D1B3E;
      letter-spacing: 0.01em;
    }

    .input-wrap {
      display: flex;
      align-items: center;
      gap: 0;
      border: 1.5px solid rgba(30,111,217,0.20);
      border-radius: 10px;
      background: #F8FAFF;
      transition: border-color 0.2s, box-shadow 0.2s;
      overflow: hidden;
    }
    .input-wrap:focus-within {
      border-color: #1E6FD9;
      box-shadow: 0 0 0 3px rgba(30,111,217,0.10);
      background: white;
    }
    .input-wrap.input-error {
      border-color: #EF4444;
    }
    .input-wrap.input-error:focus-within {
      box-shadow: 0 0 0 3px rgba(239,68,68,0.10);
    }

    .input-icon {
      font-size: 1rem;
      width: 1rem; height: 1rem;
      color: #9CA3AF;
      margin-left: 0.85rem;
      flex-shrink: 0;
    }

    .qhse-input {
      flex: 1;
      border: none;
      background: transparent;
      padding: 0.75rem 0.85rem;
      font-size: 0.875rem;
      color: #0D1B3E;
      outline: none;
      font-family: inherit;
    }
    .qhse-input::placeholder { color: #9CA3AF; }

    .toggle-pw {
      background: none;
      border: none;
      cursor: pointer;
      padding: 0 0.75rem;
      color: #9CA3AF;
      display: flex; align-items: center;
      transition: color 0.2s;
      flex-shrink: 0;
    }
    .toggle-pw:hover { color: #1E6FD9; }
    .toggle-pw mat-icon { font-size: 1.1rem; width:1.1rem; height:1.1rem; }

    .error-msg {
      font-size: 0.72rem;
      color: #EF4444;
      font-weight: 500;
      padding-left: 0.2rem;
    }

    /* ── SUBMIT ── */
    .btn-submit {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      width: 100%;
      padding: 0.85rem;
      background: linear-gradient(135deg, #1E6FD9, #2B8AFF);
      color: white;
      font-size: 0.925rem;
      font-weight: 700;
      border: none;
      border-radius: 10px;
      cursor: pointer;
      box-shadow: 0 4px 20px rgba(30,111,217,0.35);
      transition: all 0.25s;
      margin-top: 0.5rem;
      font-family: inherit;
    }
    .btn-submit:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 28px rgba(30,111,217,0.45);
    }
    .btn-submit:disabled {
      opacity: 0.55;
      cursor: not-allowed;
      transform: none;
    }
    .white-spinner ::ng-deep circle { stroke: white !important; }

    /* ── FOOTER ── */
    .form-footer {
      margin-top: 1.5rem;
      text-align: center;
      font-size: 0.85rem;
      color: #718096;
    }
    .link-btn {
      background: none;
      border: none;
      color: #1E6FD9;
      font-weight: 600;
      font-size: inherit;
      cursor: pointer;
      font-family: inherit;
      padding: 0;
      margin-left: 0.25rem;
      transition: color 0.2s;
    }
    .link-btn:hover { color: #1558B0; text-decoration: underline; }

    /* ── ANIMATION ── */
    @keyframes fadeSlideIn {
      from { opacity: 0; transform: translateY(16px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    /* ── RESPONSIVE ── */
    @media (max-width: 860px) {
      .page-wrapper { grid-template-columns: 1fr; }
      .left-panel { display: none; }
      .right-panel { padding: 2rem 1rem; background: #F4F7FF; min-height: 100vh; }
    }
    @media (max-width: 480px) {
      .field-row { grid-template-columns: 1fr; }
      .form-card { padding: 2rem 1.25rem 1.5rem; border-radius: 14px; }
    }
  `]
})
export class RegisterPageComponent {
  registerForm: FormGroup;
  isLoading = false;
  showPassword = false;
  showConfirm = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.registerForm = this.fb.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  passwordMatchValidator(group: FormGroup): any {
    const password = group.get('password');
    const confirmPassword = group.get('confirmPassword');
    return password && confirmPassword && password.value === confirmPassword.value
      ? null : { passwordMismatch: true };
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      this.isLoading = true;
      const registerRequest: RegisterRequest = this.registerForm.value;

      this.authService.register(registerRequest).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.snackBar.open(response.message, 'Fermer', { duration: 5000 });
          this.router.navigate(['/verify-otp'], {
            queryParams: { email: registerRequest.email }
          });
        },
        error: (error) => {
          this.isLoading = false;
          const errorMessage = error.error?.message || 'Erreur lors de l\'inscription';
          this.snackBar.open(errorMessage, 'Fermer', { duration: 5000 });
        }
      });
    }
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}