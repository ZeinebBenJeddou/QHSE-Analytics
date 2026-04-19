import { Component, OnInit, OnDestroy, ViewChildren, QueryList, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-otp',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './otp.component.html',
  styleUrls: ['./otp.component.css'],
})
export class OtpComponent implements OnInit, OnDestroy {

  @ViewChildren('otpInput') otpInputs!: QueryList<ElementRef<HTMLInputElement>>;

  
  otpBoxes  = Array(6).fill(0);
  otpValues = Array(6).fill('');

  
  maskedEmail = '';

  
  timerDisplay = '10:00';
  canResend    = false;
  private timerRef: any;
  private seconds  = 600;

  data = { code: '', rememberMe: false };

  constructor(
    private auth: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const email = this.auth.getPendingEmail();
    if (email) {
      const [name, domain] = email.split('@');
      this.maskedEmail = name.slice(0, 2) + '***@' + domain;
    }
    this.startTimer();
  }

  ngOnDestroy(): void {
    clearInterval(this.timerRef);
  }

 
  private startTimer(): void {
    clearInterval(this.timerRef);
    this.seconds  = 600;
    this.timerDisplay = '10:00';
    this.canResend = false;
    this.timerRef = setInterval(() => {
      this.seconds--;
      const m = Math.floor(this.seconds / 60);
      const s = this.seconds % 60;
      this.timerDisplay = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
      if (this.seconds <= 0) {
        clearInterval(this.timerRef);
        this.timerDisplay = 'Expiré';
        this.canResend = true;
      }

      this.cdr.detectChanges();
    }, 1000);
  }

  resend(): void {
    if (!this.canResend) {
      alert(`Vous pourrez renvoyer le code quand le timer sera terminé (${this.timerDisplay}).`);
      return;
    }

    const email = this.auth.getPendingEmail();
    if (!email) {
      alert('Session expirée, veuillez vous reconnecter.');
      this.router.navigate(['/login']);
      return;
    }

    this.auth.resendOtp(email).subscribe({
      next: () => {
        this.otpValues = Array(6).fill('');
        this.syncCode();
        const firstInput = this.otpInputs?.toArray()?.[0]?.nativeElement;
        if (firstInput) {
          firstInput.value = '';
          firstInput.focus();
        }
        alert('Code OTP renvoyé. Vérifiez votre email.');
        this.startTimer();
      },
      error: (err: any) => {
        alert(err?.error?.error || err?.error?.message || 'Impossible de renvoyer le code OTP.');
      }
    });
  }

  
  onInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const val   = input.value.replace(/\D/g, '');
    input.value        = val;
    this.otpValues[index] = val;

    if (val && index < 5) {
      this.otpInputs.toArray()[index + 1].nativeElement.focus();
    }
    this.syncCode();
  }

  onKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace' && !this.otpValues[index] && index > 0) {
      this.otpValues[index - 1] = '';
      this.otpInputs.toArray()[index - 1].nativeElement.focus();
      this.syncCode();
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const text = event.clipboardData?.getData('text') ?? '';
    const digits = text.replace(/\D/g, '').slice(0, 6).split('');
    digits.forEach((d, i) => { this.otpValues[i] = d; });
    const inputs = this.otpInputs.toArray();
    inputs.forEach((el, i) => { el.nativeElement.value = this.otpValues[i] || ''; });
    const last = Math.min(digits.length, 5);
    inputs[last]?.nativeElement.focus();
    this.syncCode();
  }

  private syncCode(): void {
    this.data.code = this.otpValues.join('');
  }

  
  verify(): void {
    const email = this.auth.getPendingEmail();

    if (!email) {
      alert('Session expirée, veuillez vous reconnecter.');
      this.router.navigate(['/login']);
      return;
    }

    if (this.data.code.length < 6) {
      alert('Veuillez saisir les 6 chiffres du code.');
      return;
    }

    this.auth.verifyOtp({ email, code: this.data.code }).subscribe({
      next: (res: any) => {
        this.auth.saveToken(res.accessToken);
        this.auth.clearPendingEmail();
        if (res.role === 'ADMIN') {
          this.router.navigate(['/admin']);
        } else {
          this.router.navigate(['/analyst']);
        }
      },
      error: (err: any) => {
        alert(err.error?.message || 'Code OTP invalide.');
      },
    });
  }
}