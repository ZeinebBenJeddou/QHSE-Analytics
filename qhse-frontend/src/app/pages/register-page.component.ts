import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../core/services/auth.service';
import { RegisterRequest } from '../shared/models/auth-requests';

function passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
  const val = control.value || '';
  const pattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/;
  if (val.length >= 8 && pattern.test(val)) return null;
  return { passwordWeak: true };
}

function passwordMatchValidator(g: FormGroup): ValidationErrors | null {
  const pw = g.get('password')?.value;
  const cpw = g.get('confirmPassword')?.value;
  return pw === cpw ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-register-page',
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

        <p class="form-subtitle">Créez votre compte </p>

        <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="form" novalidate>

          
          <div class="field-row">
            <div class="field-group">
              <label class="field-label">Nom</label>
              <div class="input-wrap" [class.input-error]="f['nom'].invalid && f['nom'].touched">
                <svg class="input-icon" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
                </svg>
                <input class="field-input" type="text" formControlName="nom"
                       placeholder="Nom" autocomplete="family-name">
              </div>
              <span class="field-error" *ngIf="f['nom'].invalid && f['nom'].touched">Champ requis</span>
            </div>

            <div class="field-group">
              <label class="field-label">Prénom</label>
              <div class="input-wrap" [class.input-error]="f['prenom'].invalid && f['prenom'].touched">
                <svg class="input-icon" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
                </svg>
                <input class="field-input" type="text" formControlName="prenom"
                       placeholder="Prénom" autocomplete="given-name">
              </div>
              <span class="field-error" *ngIf="f['prenom'].invalid && f['prenom'].touched">Champ requis</span>
            </div>
          </div>

          
          <div class="field-group">
            <label class="field-label">Adresse email professionnelle</label>
            <div class="input-wrap" [class.input-error]="f['email'].invalid && f['email'].touched">
              <svg class="input-icon" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
              <input class="field-input" type="email" formControlName="email"
                     placeholder="Adresse email" autocomplete="email">
            </div>
            <span class="field-error" *ngIf="f['email'].hasError('required') && f['email'].touched">L'email est requis</span>
            <span class="field-error" *ngIf="f['email'].hasError('email') && f['email'].touched">Format invalide</span>
          </div>

          
          <div class="field-group">
            <label class="field-label">Mot de passe</label>
            <div class="input-wrap" [class.input-error]="f['password'].invalid && f['password'].touched">
              <svg class="input-icon" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2"/>
                <path d="M7 11V7a5 5 0 0110 0v4"/>
              </svg>
              <input class="field-input" [type]="showPw ? 'text' : 'password'"
                     formControlName="password"
                     placeholder="Minimum 8 caractères" autocomplete="new-password">
              <button type="button" class="eye-btn" (click)="showPw = !showPw">
                <svg *ngIf="!showPw" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                </svg>
                <svg *ngIf="showPw" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              </button>
            </div>

            
            <div class="pw-strength">
              <div class="pw-bar" [class]="getBarClass(0)"></div>
              <div class="pw-bar" [class]="getBarClass(1)"></div>
              <div class="pw-bar" [class]="getBarClass(2)"></div>
              <div class="pw-bar" [class]="getBarClass(3)"></div>
            </div>
            <span class="pw-label" [style.color]="pwLabelColor">{{ pwLabelText }}</span>

            <span class="field-error" *ngIf="f['password'].hasError('required') && f['password'].touched">Requis</span>
            <span class="field-error" *ngIf="f['password'].hasError('passwordWeak') && f['password'].touched && !f['password'].hasError('required')">
              Doit contenir majuscule, minuscule, chiffre et caractère spécial (@$!%*?&)
            </span>
          </div>

        
          <div class="field-group">
            <label class="field-label">Confirmer le mot de passe</label>
            <div class="input-wrap" [class.input-error]="
              (registerForm.hasError('passwordMismatch') || f['confirmPassword'].hasError('required'))
              && f['confirmPassword'].touched">
              <svg class="input-icon" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2"/>
                <path d="M7 11V7a5 5 0 0110 0v4"/>
              </svg>
              <input class="field-input" [type]="showConfirm ? 'text' : 'password'"
                     formControlName="confirmPassword"
                     placeholder="Répétez le mot de passe" autocomplete="new-password">
              <button type="button" class="eye-btn" (click)="showConfirm = !showConfirm">
                <svg *ngIf="!showConfirm" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                </svg>
                <svg *ngIf="showConfirm" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              </button>
            </div>
            <span class="field-error"
              *ngIf="f['confirmPassword'].hasError('required') && f['confirmPassword'].touched">
              Requis
            </span>
            <span class="field-error"
              *ngIf="registerForm.hasError('passwordMismatch') && f['confirmPassword'].touched && !f['confirmPassword'].hasError('required')">
              Les mots de passe ne correspondent pas
            </span>
          </div>

         
          <button class="btn-submit" type="submit" [disabled]="registerForm.invalid || isLoading">
            <mat-spinner diameter="17" *ngIf="isLoading"></mat-spinner>
            <ng-container *ngIf="!isLoading">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5">
                <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                <circle cx="8.5" cy="7" r="4"/>
                <line x1="20" y1="8" x2="20" y2="14"/>
                <line x1="23" y1="11" x2="17" y2="11"/>
              </svg>
              Créer mon compte
            </ng-container>
            <span *ngIf="isLoading">Création en cours…</span>
          </button>

        </form>

        <div class="form-footer">
          Déjà un compte ? <a routerLink="/login" class="link">Se connecter</a>
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
      max-width: 460px;
      background: white;
      border-radius: 20px;
      padding: 2.25rem 2.25rem 2rem;
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
      margin-bottom: 1.6rem;
    }
    .logo-name { font-size: 0.95rem; color: #0D1B3E; font-weight: 400; letter-spacing: -0.01em; }
    .logo-name strong { color: #1E6FD9; font-weight: 600; }

    .form-subtitle { font-size: 0.855rem; color: #718096; text-align: center; margin-bottom: 1.5rem; }

    .form { display: flex; flex-direction: column; gap: 0.85rem; }

    .field-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
    }

    .field-group { display: flex; flex-direction: column; gap: 4px; }
    .field-label { font-size: 0.78rem; font-weight: 600; color: #2D3748; }

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
      padding: 0.68rem 0.5rem 0.68rem 0.6rem;
      font-size: 0.875rem; color: #0D1B3E;
      outline: none; font-family: inherit;
    }
    .field-input::placeholder { color: #A0AEC0; }

    .eye-btn {
      background: none; border: none; cursor: pointer;
      padding: 0.45rem 0.75rem; color: #A0AEC0;
      display: flex; align-items: center; transition: color 0.2s;
    }
    .eye-btn:hover { color: #1E6FD9; }

    .field-error { font-size: 0.74rem; color: #E53E3E; font-weight: 500; }

   
    .pw-strength {
      display: flex;
      gap: 4px;
      margin-top: 4px;
    }
    .pw-bar {
      flex: 1;
      height: 3px;
      border-radius: 2px;
      background: #E2EAF6;
      transition: background 0.3s;
    }
    .pw-bar.weak   { background: #FC8181; }
    .pw-bar.medium { background: #F6AD55; }
    .pw-bar.strong { background: #68D391; }
    .pw-label { font-size: 0.7rem; color: #A0AEC0; }

    .btn-submit {
      width: 100%;
      padding: 0.78rem;
      background: #1E6FD9;
      color: white;
      border: none;
      border-radius: 10px;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      display: flex; align-items: center; justify-content: center; gap: 7px;
      margin-top: 0.3rem;
      transition: background 0.2s, transform 0.2s, box-shadow 0.2s;
      box-shadow: 0 2px 10px rgba(30,111,217,0.25);
      font-family: inherit;
    }
    .btn-submit:hover:not(:disabled) {
      background: #1558B0;
      transform: translateY(-1px);
      box-shadow: 0 4px 16px rgba(30,111,217,0.35);
    }
    .btn-submit:disabled { opacity: 0.55; cursor: not-allowed; transform: none; }

    .form-footer {
      text-align: center;
      margin-top: 1.2rem;
      font-size: 0.83rem;
      color: #718096;
    }
    .link { color: #1E6FD9; font-weight: 600; text-decoration: none; }
    .link:hover { text-decoration: underline; }

    .security-note {
      display: flex; align-items: center; justify-content: center; gap: 5px;
      margin-top: 0.9rem;
      font-size: 0.71rem;
      color: #A0AEC0;
    }

    @media (max-width: 480px) {
      .card { padding: 2rem 1.5rem; }
      .field-row { grid-template-columns: 1fr; }
    }
  `]
})
export class RegisterPageComponent {
  registerForm: FormGroup;
  isLoading = false;
  showPw = false;
  showConfirm = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.registerForm = this.fb.group({
      nom:             ['', Validators.required],
      prenom:          ['', Validators.required],
      email:           ['', [Validators.required, Validators.email]],
      password:        ['', [Validators.required, passwordStrengthValidator]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });
  }

  get f() { return this.registerForm.controls; }

  get pwScore(): number {
    const val = this.f['password'].value || '';
    let score = 0;
    if (val.length >= 8) score++;
    if (/[A-Z]/.test(val) && /[a-z]/.test(val)) score++;
    if (/\d/.test(val)) score++;
    if (/[@$!%*?&]/.test(val)) score++;
    return score;
  }

  getBarClass(index: number): string {
    const score = this.pwScore;
    if (index >= score) return 'pw-bar';
    if (score <= 1) return 'pw-bar weak';
    if (score <= 2) return 'pw-bar medium';
    return 'pw-bar strong';
  }

  get pwLabelText(): string {
    const val = this.f['password'].value || '';
    if (!val) return 'Majuscule, minuscule, chiffre et caractère spécial requis';
    const score = this.pwScore;
    const missing: string[] = [];
    if (!/[A-Z]/.test(val)) missing.push('majuscule');
    if (!/[a-z]/.test(val)) missing.push('minuscule');
    if (!/\d/.test(val)) missing.push('chiffre');
    if (!/[@$!%*?&]/.test(val)) missing.push('caractère spécial');
    if (val.length < 8) missing.push('8 caractères minimum');
    if (missing.length) return 'Manquant : ' + missing.join(', ');
    return score <= 2 ? 'Moyen' : 'Fort — mot de passe valide';
  }

  get pwLabelColor(): string {
    const score = this.pwScore;
    if (!this.f['password'].value) return '#A0AEC0';
    if (score <= 1) return '#E53E3E';
    if (score <= 2) return '#DD6B20';
    if (score <= 3) return '#DD6B20';
    return '#38A169';
  }

  onSubmit(): void {
    if (this.registerForm.invalid) return;
    this.isLoading = true;
    const req: RegisterRequest = this.registerForm.value;
    this.authService.register(req).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.snackBar.open(res.message, 'Fermer', { duration: 5000 });
        this.router.navigate(['/verify-otp'], { queryParams: { email: req.email } });
      },
      error: (err) => {
        this.isLoading = false;
        const msg = err.error?.message || "Erreur lors de l'inscription";
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      }
    });
  }
}