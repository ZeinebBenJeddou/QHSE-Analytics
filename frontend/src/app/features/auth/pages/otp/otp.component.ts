import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../../../core/services/auth.service';
import { TokenService } from '../../../../core/services/token.service';
import { OtpRequest } from '../../models/auth.models';

@Component({
  selector: 'app-otp',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './otp.component.html',
  styleUrls: ['./otp.component.css']
})
export class OtpComponent {
  private readonly fb = inject(FormBuilder);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly tokenService = inject(TokenService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  backendError = '';

  ngOnInit(): void {
    const emailFromQuery = this.activatedRoute.snapshot.queryParamMap.get('email');
    if (emailFromQuery) {
      this.tokenService.savePendingEmail(emailFromQuery);
    }
  }

  otpForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]],
    rememberMe: [false]
  });

  get code() {
    return this.otpForm.get('code');
  }



  getDigit(i: number): string {
    return (this.otpForm.get('code')?.value ?? '')[i] ?? '';
  }

 
  onDigitInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const char = input.value.replace(/\D/g, '').slice(-1);
    input.value = char;

    const current = this.otpForm.get('code')?.value ?? '';
    const arr = current.split('');
    arr[index] = char;
    const newCode = arr.join('').slice(0, 6);
    this.otpForm.get('code')?.setValue(newCode);

    if (char && index < 5) {
      this.focusBox(index + 1);
    }
  }

 
  onDigitKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace') {
      const current = this.otpForm.get('code')?.value ?? '';
      if (!current[index] && index > 0) {
        const arr = current.split('');
        arr[index - 1] = '';
        this.otpForm.get('code')?.setValue(arr.join(''));
        this.focusBox(index - 1);
        event.preventDefault();
      }
    }
  }

  
  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasted = event.clipboardData?.getData('text') ?? '';
    const digits = pasted.replace(/\D/g, '').slice(0, 6);
    this.otpForm.get('code')?.setValue(digits);
    this.focusBox(Math.min(digits.length, 5));
  }

  private focusBox(index: number): void {
    const boxes = document.querySelectorAll<HTMLInputElement>('.otp-box');
    boxes[index]?.focus();
  }



  submit(): void {
    this.backendError = '';

    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      return;
    }

    let email = this.activatedRoute.snapshot.queryParamMap.get('email') ?? this.tokenService.getPendingEmail();
    if (email) {
      this.tokenService.savePendingEmail(email);
    }

    if (!email) {
      this.backendError = 'Email is required for OTP verification. Please login again.';
      this.router.navigate(['/auth/login']);
      return;
    }

    const otpRequest = {
      email,
      ...this.otpForm.getRawValue()
    } as OtpRequest;

    this.authService.verifyOtp(otpRequest).subscribe({
      next: (response) => {
        this.tokenService.clearPendingEmail();
        this.tokenService.setToken(response.accessToken);
        this.snackBar.open('OTP verified. Redirecting to dashboard.', 'Close', {
          duration: 3000
        });
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        const fieldError = error?.error?.fieldErrors?.code;
        const message = error?.error?.message || fieldError || (typeof error?.error === 'string' ? error.error : null);
        this.backendError = message || 'Unable to verify OTP. Please try again.';
        console.error('OTP verify error:', error);
      }
    });
  }
}