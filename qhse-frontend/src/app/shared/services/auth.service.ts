import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { API_BASE_URL } from '../constants';
import {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  MessageResponse,
  RegisterRequest,
  ResetPasswordRequest,
  RefreshTokenRequest,
  VerifyOtpRequest
} from '../models/auth.models';
import { AuthStorageService } from './auth-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient, private authStorage: AuthStorageService) {}

  register(request: RegisterRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API_BASE_URL}/auth/register`, request);
  }

  login(request: LoginRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API_BASE_URL}/auth/login`, request);
  }

  verifyAccount(token: string): Observable<MessageResponse> {
    const params = new HttpParams().set('token', token);
    return this.http.get<MessageResponse>(`${API_BASE_URL}/auth/verify`, { params });
  }

  resendVerification(email: string): Observable<MessageResponse> {
    const params = new HttpParams().set('email', email);
    return this.http.post<MessageResponse>(`${API_BASE_URL}/auth/resend-verification`, null, { params });
  }

  verifyOtp(request: VerifyOtpRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/verify-otp`, request).pipe(
      tap((auth) => this.authStorage.setAuth(auth))
    );
  }

  resendOtp(email: string): Observable<MessageResponse> {
    const params = new HttpParams().set('email', email);
    return this.http.post<MessageResponse>(`${API_BASE_URL}/auth/resend-otp`, null, { params });
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API_BASE_URL}/auth/forgot-password`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API_BASE_URL}/auth/reset-password`, request);
  }

  refreshToken(request: RefreshTokenRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/refresh`, request).pipe(
      tap((auth) => this.authStorage.setAuth(auth))
    );
  }

  logout(): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API_BASE_URL}/auth/logout`, {});
  }
}
