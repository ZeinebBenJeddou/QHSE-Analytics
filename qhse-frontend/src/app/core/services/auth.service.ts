import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ApiService } from './api.service';
import { AuthStorageService } from './auth-storage.service';
import { AuthResponse } from '../../shared/models/auth-response';
import { RegisterRequest, VerifyOtpRequest, MessageResponse, ForgotPasswordRequest } from '../../shared/models/auth-requests';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(
    private api: ApiService,
    private authStorage: AuthStorageService
  ) {}

  login(email: string, password: string): Observable<MessageResponse> {
    return this.api.post<MessageResponse>('/auth/login', { email, password });
  }

  register(registerRequest: RegisterRequest): Observable<MessageResponse> {
    return this.api.post<MessageResponse>('/auth/register', registerRequest);
  }

  refreshToken(refreshToken: string): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/refresh', { refreshToken }).pipe(
      tap(response => this.authStorage.saveAuthData(response))
    );
  }

  logout(): Observable<any> {
    return this.api.post('/auth/logout', {});
  }

  verifyOtp(verifyOtpRequest: VerifyOtpRequest): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/verify-otp', verifyOtpRequest).pipe(
      tap(response => this.authStorage.saveAuthData(response))
    );
  }

  resendOtp(email: string): Observable<any> {
    return this.api.post(`/auth/resend-otp?email=${encodeURIComponent(email)}`, {});
  }

  forgotPassword(email: string): Observable<MessageResponse> {
    const request: ForgotPasswordRequest = { email };
    return this.api.post<MessageResponse>('/auth/forgot-password', request);
  }
}
