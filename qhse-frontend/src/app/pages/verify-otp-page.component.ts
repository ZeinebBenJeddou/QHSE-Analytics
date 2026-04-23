import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../core/services/auth.service';
import { VerifyOtpRequest } from '../shared/models/auth-requests';

@Component({
  selector: 'app-verify-otp-page',
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

      <!-- Left panel -->
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
            <div class="shield-visual">
              <svg width="72" height="72" viewBox="0 0 80 80" fill="none">
                <circle cx="40" cy="40" r="38" fill="rgba(255,255,255,0.07)" stroke="rgba(255,255,255,0.15)" stroke-width="1.5"/>
                <circle cx="40" cy="40" r="26" fill="rgba(255,255,255,0.07)" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>
                <path d="M40 18L56 25V38C56 48 48 56.5 40 60C32 56.5 24 48 24 38V25L40 18Z" fill="rgba(255,255,255,0.15)" stroke="white" stroke-width="1.5"/>
                <polyline points="33,40 38,45 47,35" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
              </svg>
              <div class="pulse-ring ring-1"></div>
              <div class="pulse-ring ring-2"></div>
            </div>

            <h2 class="left-title">Vérification en<br>deux étapes</h2>
            <p class="left-desc">
              Cette étape supplémentaire protège votre compte
              et garantit que vous êtes bien le seul à y accéder.
            </p>
          </div>

          <div class="security-features">
            <div class="sec-item" *ngFor="let s of securityItems">
              <div class="sec-icon" [innerHTML]="s.icon"></div>
              <div class="sec-text">
                <span class="sec-title">{{ s.title }}</span>
                <span class="sec-desc">{{ s.desc }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right panel -->
      <div class="right-panel">
        <div class="form-box">

          <div class="form-header">
            <a routerLink="/login" class="back-btn">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 5l-7 7 7 7"/></svg>
              Retour à la connexion
            </a>

            <div class="otp-icon-wrap">
              <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="1.8">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
            </div>

            <h1 class="form-title">Vérifiez votre email</h1>
            <p class="form-subtitle">
              Nous avons envoyé un code à 6 chiffres à<br>
              <strong class="email-highlight">{{ email }}</strong>
            </p>
          </div>

          <!-- OTP DIGIT INPUTS -->
          <div class="otp-inputs" (paste)="onPaste($event)">
            <input
              *ngFor="let ctrl of digitControls; let i = index"
              class="otp-digit"
              type="text"
              inputmode="numeric"
              maxlength="1"
              [value]="digits[i]"
              (input)="onDigitInput($event, i)"
              (keydown)="onKeyDown($event, i)"
              (focus)="onFocus(i)"
              [class.filled]="digits[i]"
              [class.otp-error]="showOtpError"
              #digitInput
            />
          </div>

          <div class="otp-error-msg" *ngIf="showOtpError">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#E53E3E" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            Code invalide ou expiré. Veuillez réessayer.
          </div>

          <!-- Remember me -->
          <label class="remember-label">
            <div class="custom-checkbox" [class.checked]="rememberMe" (click)="rememberMe = !rememberMe">
              <svg *ngIf="rememberMe" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3.5"><polyline points="20 6 9 17 4 12"/></svg>
            </div>
            <span>Se souvenir de moi sur cet appareil</span>
          </label>

          <!-- Submit -->
          <button
            class="btn-submit"
            (click)="onSubmit()"
            [disabled]="!isCodeComplete || isLoading"
          >
            <mat-spinner diameter="18" *ngIf="isLoading"></mat-spinner>
            <span *ngIf="!isLoading">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
              Vérifier le code
            </span>
            <span *ngIf="isLoading">Vérification en cours…</span>
          </button>

          <!-- Resend -->
          <div class="resend-row">
            <span class="resend-text">Vous n'avez pas reçu le code ?</span>

            <button
              class="resend-btn"
              (click)="resendOtp()"
              [disabled]="resendLoading || countdown > 0"
            >
              <mat-spinner diameter="14" *ngIf="resendLoading"></mat-spinner>
              <span *ngIf="!resendLoading && countdown === 0">Renvoyer le code</span>
              <span *ngIf="!resendLoading && countdown > 0" class="countdown">Renvoyer dans {{ countdown }}s</span>
            </button>
          </div>

          <div class="security-note">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#A0AEC0" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            <span>Code valide pendant 15 minutes · Chiffrement de bout en bout</span>
          </div>

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
      position: fixed; border-radius: 50%;
      filter: blur(80px); pointer-events: none; z-index: 0;
      animation: floatOrb 12s ease-in-out infinite;
    }
    .orb-1 { width: 400px; height: 400px; background: rgba(30,111,217,0.12); top: -100px; right: 20%; }
    .orb-2 { width: 300px; height: 300px; background: rgba(100,180,255,0.09); bottom: 5%; left: 30%; animation-delay: -5s; }
    @keyframes floatOrb {
      0%, 100% { transform: translate(0,0); }
      50% { transform: translate(15px,-25px); }
    }

    /* ── LEFT PANEL ── */
    .left-panel {
      position: relative; z-index: 1;
      width: 46%;
      background: linear-gradient(160deg, #0D1B3E 0%, #1E3A5F 50%, #1E6FD9 100%);
      display: flex; align-items: center; justify-content: center;
      padding: 3rem 3.5rem; overflow: hidden;
    }
    .left-panel::before {
      content: '';
      position: absolute; inset: 0;
      background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none'%3E%3Cg fill='%23ffffff' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
      pointer-events: none;
    }
    .left-content {
      position: relative;
      display: flex; flex-direction: column; gap: 2.75rem;
      max-width: 380px; width: 100%;
      animation: fadeSlideIn 0.7s ease both;
    }

    .brand {
      display: flex; align-items: center; gap: 0.65rem;
      text-decoration: none;
    }
    .brand-icon {
      width: 42px; height: 42px;
      background: rgba(255,255,255,0.12); border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      border: 1px solid rgba(255,255,255,0.2);
    }
    .brand-name { font-size: 1.1rem; color: white; letter-spacing: -0.02em; }
    .brand-name strong { color: #7CC3FF; }

    /* Shield visual */
    .left-hero { display: flex; flex-direction: column; align-items: flex-start; gap: 1.25rem; }
    .shield-visual {
      position: relative;
      width: 80px; height: 80px;
      display: flex; align-items: center; justify-content: center;
    }
    .pulse-ring {
      position: absolute;
      border-radius: 50%;
      border: 1.5px solid rgba(255,255,255,0.25);
      animation: pulseRing 3s ease-out infinite;
    }
    .ring-1 { width: 100px; height: 100px; animation-delay: 0s; }
    .ring-2 { width: 130px; height: 130px; animation-delay: 1s; }
    @keyframes pulseRing {
      0% { transform: scale(0.85); opacity: 0.6; }
      100% { transform: scale(1.15); opacity: 0; }
    }

    .left-title {
      font-size: 1.85rem; font-weight: 800; color: white;
      line-height: 1.25; letter-spacing: -0.03em;
    }
    .left-desc { font-size: 0.875rem; color: rgba(255,255,255,0.6); line-height: 1.65; }

    /* Security features */
    .security-features { display: flex; flex-direction: column; gap: 1rem; }
    .sec-item {
      display: flex; align-items: flex-start; gap: 0.9rem;
      padding: 0.85rem 1rem;
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 10px;
    }
    .sec-icon {
      flex-shrink: 0;
      width: 34px; height: 34px;
      background: rgba(255,255,255,0.1);
      border-radius: 8px;
      display: flex; align-items: center; justify-content: center;
    }
    .sec-text { display: flex; flex-direction: column; gap: 0.15rem; }
    .sec-title { font-size: 0.83rem; font-weight: 700; color: white; }
    .sec-desc { font-size: 0.75rem; color: rgba(255,255,255,0.5); }

    /* ── RIGHT PANEL ── */
    .right-panel {
      position: relative; z-index: 1; flex: 1;
      display: flex; align-items: center; justify-content: center;
      padding: 2.5rem 2rem;
    }

    .form-box {
      width: 100%; max-width: 420px;
      background: white;
      border-radius: 20px;
      padding: 2.5rem 2.25rem;
      box-shadow: 0 16px 48px rgba(30,111,217,0.10), 0 4px 16px rgba(0,0,0,0.06);
      border: 1px solid rgba(30,111,217,0.08);
      display: flex; flex-direction: column; gap: 1.4rem;
      animation: fadeSlideIn 0.6s 0.1s ease both;
    }

    @keyframes fadeSlideIn {
      from { opacity: 0; transform: translateY(20px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    /* ── FORM HEADER ── */
    .form-header { display: flex; flex-direction: column; gap: 0.6rem; }

    .back-btn {
      display: inline-flex; align-items: center; gap: 0.4rem;
      text-decoration: none; font-size: 0.82rem; font-weight: 600;
      color: #1E6FD9; margin-bottom: 0.25rem;
      transition: gap 0.2s;
    }
    .back-btn:hover { gap: 0.6rem; }

    .otp-icon-wrap {
      width: 54px; height: 54px;
      background: rgba(30,111,217,0.07);
      border-radius: 14px;
      display: flex; align-items: center; justify-content: center;
    }

    .form-title {
      font-size: 1.6rem; font-weight: 800;
      color: #0D1B3E; letter-spacing: -0.03em;
    }
    .form-subtitle {
      font-size: 0.875rem; color: #718096; line-height: 1.6;
    }
    .email-highlight { color: #1E6FD9; font-weight: 700; }

    /* ── OTP DIGIT INPUTS ── */
    .otp-inputs {
      display: flex;
      gap: 0.6rem;
      justify-content: center;
      padding: 0.5rem 0;
    }
    .otp-digit {
      width: 52px; height: 60px;
      text-align: center;
      font-size: 1.5rem; font-weight: 800;
      color: #0D1B3E;
      background: #F8FAFF;
      border: 2px solid #E2EAF6;
      border-radius: 12px;
      outline: none;
      font-family: 'Courier New', monospace;
      transition: all 0.2s;
      caret-color: #1E6FD9;
    }
    .otp-digit:focus {
      border-color: #1E6FD9;
      background: white;
      box-shadow: 0 0 0 4px rgba(30,111,217,0.12);
      transform: translateY(-2px);
    }
    .otp-digit.filled {
      border-color: #1E6FD9;
      background: rgba(30,111,217,0.04);
    }
    .otp-digit.otp-error {
      border-color: #E53E3E;
      background: #FFF5F5;
      animation: shake 0.4s ease;
    }
    @keyframes shake {
      0%, 100% { transform: translateX(0); }
      20%, 60% { transform: translateX(-4px); }
      40%, 80% { transform: translateX(4px); }
    }

    .otp-error-msg {
      display: flex; align-items: center; gap: 0.4rem;
      font-size: 0.8rem; color: #E53E3E; font-weight: 500;
      text-align: center; justify-content: center;
    }

    /* ── REMEMBER ME ── */
    .remember-label {
      display: flex; align-items: center; gap: 0.65rem;
      cursor: pointer;
      font-size: 0.85rem; color: #4A5568; font-weight: 500;
      user-select: none;
    }
    .custom-checkbox {
      width: 18px; height: 18px;
      border: 2px solid #CBD5E0;
      border-radius: 5px;
      background: white;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
      transition: all 0.2s;
    }
    .custom-checkbox.checked {
      background: #1E6FD9;
      border-color: #1E6FD9;
    }

    /* ── SUBMIT ── */
    .btn-submit {
      width: 100%; padding: 0.85rem;
      background: linear-gradient(135deg, #1E6FD9, #2B8AFF);
      color: white; border: none; border-radius: 10px;
      font-size: 0.95rem; font-weight: 700;
      cursor: pointer;
      display: flex; align-items: center; justify-content: center; gap: 0.6rem;
      box-shadow: 0 4px 18px rgba(30,111,217,0.35);
      transition: all 0.25s;
    }
    .btn-submit:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(30,111,217,0.45);
    }
    .btn-submit:disabled { opacity: 0.55; cursor: not-allowed; transform: none; }

    /* ── RESEND ── */
    .resend-row {
      display: flex; flex-direction: column; align-items: center; gap: 0.4rem;
      text-align: center;
    }
    .resend-text { font-size: 0.82rem; color: #A0AEC0; }
    .resend-btn {
      background: none; border: none; cursor: pointer;
      font-size: 0.85rem; font-weight: 700; color: #1E6FD9;
      display: inline-flex; align-items: center; gap: 0.4rem;
      padding: 0.3rem 0.5rem; border-radius: 6px;
      transition: background 0.2s;
    }
    .resend-btn:hover:not(:disabled) { background: rgba(30,111,217,0.07); }
    .resend-btn:disabled { color: #A0AEC0; cursor: not-allowed; }
    .countdown { color: #A0AEC0; font-weight: 600; }

    /* ── SECURITY NOTE ── */
    .security-note {
      display: flex; align-items: center; justify-content: center; gap: 0.4rem;
      font-size: 0.72rem; color: #A0AEC0;
    }

    /* ── RESPONSIVE ── */
    @media (max-width: 860px) {
      .left-panel { display: none; }
      .right-panel { padding: 1.5rem 1rem; }
      .form-box { padding: 2rem 1.5rem; }
    }
    @media (max-width: 420px) {
      .otp-digit { width: 44px; height: 54px; font-size: 1.3rem; }
    }
  `]
})
export class VerifyOtpPageComponent implements OnInit, OnDestroy {
  otpForm: FormGroup;
  email = '';
  isLoading = false;
  resendLoading = false;
  showOtpError = false;
  rememberMe = false;

  digits: string[] = ['', '', '', '', '', ''];
  digitControls = new Array(6);

  countdown = 0;
  private countdownInterval: any;

  securityItems = [
    {
      title: 'Code à usage unique',
      desc: 'Expire automatiquement après 15 minutes',
      icon: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.8)" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`
    },
    {
      title: 'Chiffrement de bout en bout',
      desc: 'Vos données sont protégées en transit',
      icon: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.8)" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>`
    },
    {
      title: 'Authentification renforcée',
      desc: 'Protection JWT + OTP contre les intrusions',
      icon: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.8)" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`
    },
  ];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {
    this.otpForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
      rememberMe: [false]
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
      if (!this.email) {
        this.snackBar.open('Email manquant. Retour à l\'inscription.', 'Fermer', { duration: 3000 });
        this.router.navigate(['/register']);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
  }

  get isCodeComplete(): boolean {
    return this.digits.every(d => d !== '');
  }

  get fullCode(): string {
    return this.digits.join('');
  }

  onDigitInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const val = input.value.replace(/\D/g, '').slice(-1);
    this.digits[index] = val;
    this.showOtpError = false;

    if (val && index < 5) {
      this.focusDigit(index + 1);
    }
  }

  onKeyDown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace') {
      if (this.digits[index]) {
        this.digits[index] = '';
      } else if (index > 0) {
        this.digits[index - 1] = '';
        this.focusDigit(index - 1);
      }
    } else if (event.key === 'ArrowLeft' && index > 0) {
      this.focusDigit(index - 1);
    } else if (event.key === 'ArrowRight' && index < 5) {
      this.focusDigit(index + 1);
    } else if (event.key === 'Enter' && this.isCodeComplete) {
      this.onSubmit();
    }
  }

  onFocus(index: number): void {
    // Select content on focus for easy replacement
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasted = event.clipboardData?.getData('text').replace(/\D/g, '').slice(0, 6) || '';
    for (let i = 0; i < 6; i++) {
      this.digits[i] = pasted[i] || '';
    }
    if (pasted.length > 0) {
      this.focusDigit(Math.min(pasted.length, 5));
    }
  }

  private focusDigit(index: number): void {
    const inputs = document.querySelectorAll<HTMLInputElement>('.otp-digit');
    if (inputs[index]) {
      inputs[index].focus();
      inputs[index].value = this.digits[index];
    }
  }

  onSubmit(): void {
    if (!this.isCodeComplete) return;

    this.isLoading = true;
    this.showOtpError = false;

    const verifyOtpRequest: VerifyOtpRequest = {
      email: this.email,
      code: this.fullCode,
      rememberMe: this.rememberMe
    };

    this.authService.verifyOtp(verifyOtpRequest).subscribe({
      next: () => {
        this.isLoading = false;
        this.snackBar.open('Connexion réussie ! Bienvenue.', 'Fermer', { duration: 3000 });
        this.router.navigate(['/dashboard']);
      },
      error: (error: any) => {
        this.isLoading = false;
        this.showOtpError = true;
        this.digits = ['', '', '', '', '', ''];
        setTimeout(() => this.focusDigit(0), 50);
        const msg = error.error?.message || 'Code invalide ou expiré';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      }
    });
  }

  resendOtp(): void {
    if (!this.email || this.countdown > 0) return;

    this.resendLoading = true;
    this.authService.resendOtp(this.email).subscribe({
      next: () => {
        this.resendLoading = false;
        this.snackBar.open('Code renvoyé avec succès', 'Fermer', { duration: 3000 });
        this.startCountdown(60);
      },
      error: (error: any) => {
        this.resendLoading = false;
        const msg = error.error?.message || 'Erreur lors du renvoi du code';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      }
    });
  }

  private startCountdown(seconds: number): void {
    this.countdown = seconds;
    if (this.countdownInterval) clearInterval(this.countdownInterval);
    this.countdownInterval = setInterval(() => {
      this.countdown--;
      if (this.countdown <= 0) {
        clearInterval(this.countdownInterval);
        this.countdown = 0;
      }
    }, 1000);
  }
}