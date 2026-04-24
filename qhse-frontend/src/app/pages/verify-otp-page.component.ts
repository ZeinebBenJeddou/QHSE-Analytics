import { Component, OnInit, OnDestroy, QueryList, ViewChildren, ElementRef } from '@angular/core';
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
      <div class="bg-grid"></div>

      <div class="card">

        <div class="logo">
          <span class="logo-name">QHSE <strong>Analytics</strong></span>
        </div>

        <a routerLink="/login" class="back-btn">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M19 12H5M12 5l-7 7 7 7"/>
          </svg>
          Retour à la connexion
        </a>

        <div class="otp-icon-wrap">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#1E6FD9" stroke-width="1.8">
            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
            <polyline points="22,6 12,13 2,6"/>
          </svg>
        </div>

        <h1 class="form-title">Vérifiez votre email</h1>
        <p class="form-subtitle">
          Nous avons envoyé un code à 6 chiffres à<br>
          <strong class="email-highlight">{{ email }}</strong>
        </p>

        
        <div class="otp-inputs" (paste)="onPaste($event)">
          <input
            *ngFor="let ctrl of digitControls; let i = index"
            #digitInput
            class="otp-digit"
            type="text"
            inputmode="numeric"
            maxlength="1"
            [value]="digits[i]"
            [class.filled]="digits[i]"
            [class.error]="showOtpError"
            (input)="onDigitInput($event, i)"
            (keydown)="onKeyDown($event, i)"
          />
        </div>

        <div class="otp-error-msg" *ngIf="showOtpError">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#E53E3E" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          Code invalide ou expiré. Veuillez réessayer.
        </div>

        
        <label class="remember-label">
          <div class="custom-checkbox" [class.checked]="rememberMe" (click)="rememberMe = !rememberMe">
            <svg *ngIf="rememberMe" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3.5">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
          <span>Se souvenir de moi sur cet appareil</span>
        </label>

        
        <button
          class="btn-submit"
          (click)="onSubmit()"
          [disabled]="!isCodeComplete || isLoading"
        >
          <mat-spinner diameter="17" *ngIf="isLoading"></mat-spinner>
          <ng-container *ngIf="!isLoading">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
            Vérifier le code
          </ng-container>
          <span *ngIf="isLoading">Vérification en cours…</span>
        </button>

       
        <div class="resend-row">
          <span class="resend-text">Vous n'avez pas reçu le code ?</span>
          <button class="resend-btn" (click)="resendOtp()" [disabled]="resendLoading || countdown > 0">
            <mat-spinner diameter="13" *ngIf="resendLoading"></mat-spinner>
            <span *ngIf="!resendLoading && countdown === 0">Renvoyer le code</span>
            <span *ngIf="!resendLoading && countdown > 0">Renvoyer dans {{ countdown }}s</span>
          </button>
        </div>

        <div class="security-note">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
          Code valide pendant 10 minutes · Chiffrement de bout en bout
        </div>

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
      position: fixed; inset: 0;
      background-image:
        linear-gradient(rgba(30,111,217,0.04) 1px, transparent 1px),
        linear-gradient(90deg, rgba(30,111,217,0.04) 1px, transparent 1px);
      background-size: 48px 48px;
      pointer-events: none; z-index: 0;
    }

    .card {
      position: relative; z-index: 1;
      width: 100%; max-width: 420px;
      background: white;
      border-radius: 20px;
      padding: 2.5rem 2.25rem;
      box-shadow:
        0 0 0 1px rgba(30,111,217,0.07),
        0 8px 32px rgba(30,111,217,0.09),
        0 2px 8px rgba(0,0,0,0.04);
      display: flex; flex-direction: column;
      animation: fadeUp 0.5s ease both;
    }

    @keyframes fadeUp {
      from { opacity: 0; transform: translateY(16px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    
    .logo {
      display: flex; align-items: center; justify-content: center;
      margin-bottom: 2rem;
    }
    .logo-name { font-size: 0.95rem; color: #0D1B3E; font-weight: 400; letter-spacing: -0.01em; }
    .logo-name strong { color: #1E6FD9; font-weight: 600; }

    .back-btn {
      display: inline-flex; align-items: center; gap: 5px;
      text-decoration: none; font-size: 0.82rem; font-weight: 600;
      color: #1E6FD9; margin-bottom: 1.4rem;
      transition: gap 0.2s;
    }
    .back-btn:hover { gap: 7px; }

    
    .otp-icon-wrap {
      width: 48px; height: 48px;
      background: rgba(30,111,217,0.07);
      border-radius: 13px;
      display: flex; align-items: center; justify-content: center;
      margin-bottom: 0.9rem;
    }

    .form-title {
      font-size: 1.45rem; font-weight: 700;
      color: #0D1B3E; letter-spacing: -0.025em; margin-bottom: 0.3rem;
    }
    .form-subtitle { font-size: 0.855rem; color: #718096; line-height: 1.55; margin-bottom: 1.75rem; }
    .email-highlight { color: #1E6FD9; font-weight: 700; }

    
    .otp-inputs {
      display: flex; gap: 0.55rem; justify-content: center;
      margin-bottom: 0.4rem;
    }
    .otp-digit {
      width: 52px; height: 58px;
      text-align: center;
      font-size: 1.4rem; font-weight: 800;
      color: #0D1B3E;
      background: #F8FAFF;
      border: 1.5px solid #E2EAF6;
      border-radius: 10px;
      outline: none;
      font-family: 'Courier New', monospace;
      transition: all 0.2s;
      caret-color: #1E6FD9;
    }
    .otp-digit:focus {
      border-color: #1E6FD9; background: white;
      box-shadow: 0 0 0 3px rgba(30,111,217,0.09);
      transform: translateY(-2px);
    }
    .otp-digit.filled { border-color: #1E6FD9; background: rgba(30,111,217,0.04); }
    .otp-digit.error {
      border-color: #FC8181; background: #FFF5F5;
      animation: shake 0.35s ease;
    }
    @keyframes shake {
      0%,100% { transform: translateX(0); }
      25%,75%  { transform: translateX(-4px); }
      50%      { transform: translateX(4px); }
    }

    .otp-error-msg {
      display: flex; align-items: center; gap: 5px;
      font-size: 0.77rem; color: #E53E3E; font-weight: 500;
      justify-content: center; margin-bottom: 0.5rem;
    }

 
    .remember-label {
      display: flex; align-items: center; gap: 0.6rem;
      cursor: pointer; font-size: 0.84rem; color: #4A5568;
      font-weight: 500; user-select: none;
      margin-top: 0.75rem; margin-bottom: 0;
    }
    .custom-checkbox {
      width: 17px; height: 17px;
      border: 1.5px solid #CBD5E0; border-radius: 5px;
      background: white; display: flex; align-items: center; justify-content: center;
      flex-shrink: 0; transition: all 0.2s;
    }
    .custom-checkbox.checked { background: #1E6FD9; border-color: #1E6FD9; }

    
    .btn-submit {
      width: 100%; padding: 0.8rem;
      background: #1E6FD9; color: white;
      border: none; border-radius: 10px;
      font-size: 0.9rem; font-weight: 600;
      cursor: pointer; margin-top: 1rem;
      display: flex; align-items: center; justify-content: center; gap: 7px;
      transition: background 0.2s, transform 0.2s, box-shadow 0.2s;
      box-shadow: 0 2px 10px rgba(30,111,217,0.25);
    }
    .btn-submit:hover:not(:disabled) {
      background: #1558B0; transform: translateY(-1px);
      box-shadow: 0 4px 16px rgba(30,111,217,0.35);
    }
    .btn-submit:disabled { opacity: 0.55; cursor: not-allowed; transform: none; }

   
    .resend-row {
      display: flex; flex-direction: column; align-items: center; gap: 0.3rem;
      margin-top: 1.1rem; text-align: center;
    }
    .resend-text { font-size: 0.82rem; color: #A0AEC0; }
    .resend-btn {
      background: none; border: none; cursor: pointer;
      font-size: 0.84rem; font-weight: 700; color: #1E6FD9;
      display: inline-flex; align-items: center; gap: 5px;
      padding: 0.25rem 0.5rem; border-radius: 6px;
      transition: background 0.2s;
    }
    .resend-btn:hover:not(:disabled) { background: rgba(30,111,217,0.07); }
    .resend-btn:disabled { color: #A0AEC0; cursor: not-allowed; }

   
    .security-note {
      display: flex; align-items: center; justify-content: center; gap: 5px;
      margin-top: 1.2rem; font-size: 0.71rem; color: #A0AEC0;
    }

    @media (max-width: 480px) {
      .card { padding: 2rem 1.5rem; }
      .otp-digit { width: 44px; height: 52px; font-size: 1.2rem; }
    }
  `]
})
export class VerifyOtpPageComponent implements OnInit, OnDestroy {
  @ViewChildren('digitInput') digitInputs!: QueryList<ElementRef<HTMLInputElement>>;

  email = '';
  isLoading = false;
  resendLoading = false;
  showOtpError = false;
  rememberMe = false;
  digits: string[] = ['', '', '', '', '', ''];
  digitControls = new Array(6);
  countdown = 0;
  private countdownInterval: any;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
      if (!this.email) {
        this.snackBar.open('Email manquant. Retour à la connexion.', 'Fermer', { duration: 3000 });
        this.router.navigate(['/login']);
      }
    });
    setTimeout(() => this.focusDigit(0), 100);
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
    input.value = val;
    this.showOtpError = false;
    if (val && index < 5) this.focusDigit(index + 1);
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

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasted = event.clipboardData?.getData('text').replace(/\D/g, '').slice(0, 6) || '';
    for (let i = 0; i < 6; i++) {
      this.digits[i] = pasted[i] || '';
    }
    this.focusDigit(Math.min(pasted.length, 5));
   
    setTimeout(() => {
      this.digitInputs.forEach((el, i) => { el.nativeElement.value = this.digits[i]; });
    });
  }

  private focusDigit(index: number): void {
    setTimeout(() => {
      const el = this.digitInputs?.get(index);
      if (el) el.nativeElement.focus();
    });
  }

  onSubmit(): void {
    if (!this.isCodeComplete || this.isLoading) return;
    this.isLoading = true;
    this.showOtpError = false;

    const req: VerifyOtpRequest = {
      email: this.email,
      code: this.fullCode,
      rememberMe: this.rememberMe
    };

    this.authService.verifyOtp(req).subscribe({
      next: () => {
        this.isLoading = false;
        this.snackBar.open('Connexion réussie ! Bienvenue.', 'Fermer', { duration: 3000 });
        this.router.navigate(['/dashboard']);
      },
      error: (err: any) => {
        this.isLoading = false;
        this.showOtpError = true;
        this.digits = ['', '', '', '', '', ''];
        this.digitInputs.forEach(el => { el.nativeElement.value = ''; });
        setTimeout(() => this.focusDigit(0), 50);
        const msg = err.error?.message || 'Code invalide ou expiré';
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
      error: (err: any) => {
        this.resendLoading = false;
        const msg = err.error?.message || 'Erreur lors du renvoi du code';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      }
    });
  }

  private startCountdown(seconds: number): void {
    this.countdown = seconds;
    if (this.countdownInterval) clearInterval(this.countdownInterval);
    this.countdownInterval = setInterval(() => {
      this.countdown--;
      if (this.countdown <= 0) { clearInterval(this.countdownInterval); this.countdown = 0; }
    }, 1000);
  }
}