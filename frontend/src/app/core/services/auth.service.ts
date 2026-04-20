import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  AuthResponse,
  LoginRequest,
  MessageResponse,
  RegisterRequest,
  ResetPasswordRequest,
  UserRole,
  VerifyOtpRequest,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private API = 'http://localhost:8080/api/auth';
  private readonly PENDING_EMAIL_KEY = 'pendingEmail';
  private readonly AUTH_USER_KEY = 'authUser';
  private readonly TOKEN_KEY = 'token';

  constructor(private http: HttpClient) {}

  register(data: RegisterRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API}/register`, data);
  }

  login(data: LoginRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API}/login`, data);
  }

  verifyAccount(token: string): Observable<MessageResponse> {
    return this.http.get<MessageResponse>(`${this.API}/verify`, { params: { token } });
  }

  resendVerification(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API}/resend-verification`, null, { params: { email } });
  }

  verifyOtp(data: VerifyOtpRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/verify-otp`, data);
  }

  resendOtp(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API}/resend-otp`, null, { params: { email } });
  }

  forgotPassword(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API}/forgot-password`, { email });
  }

  resetPassword(data: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API}/reset-password`, data);
  }

  logout(email?: string): Observable<MessageResponse> {
    if (!email) {
      this.clearSession();
      return of({ message: 'Déconnexion réussie.' });
    }

    const params = new HttpParams().set('email', email);
    return this.http.post<MessageResponse>(`${this.API}/logout`, null, { params }).pipe(
      catchError(() => of({ message: 'Déconnexion réussie.' })),
      map((response) => {
        this.clearSession();
        return response;
      })
    );
  }

  saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  saveSession(authResponse: AuthResponse): void {
    if (authResponse.accessToken) {
      this.saveToken(authResponse.accessToken);
    }

    localStorage.setItem(this.AUTH_USER_KEY, JSON.stringify(authResponse));
  }

  setPendingEmail(email: string): void {
    sessionStorage.setItem(this.PENDING_EMAIL_KEY, email);
  }

  getPendingEmail(): string | null {
    return sessionStorage.getItem(this.PENDING_EMAIL_KEY);
  }

  clearPendingEmail(): void {
    sessionStorage.removeItem(this.PENDING_EMAIL_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getStoredUser(): AuthResponse | null {
    const raw = localStorage.getItem(this.AUTH_USER_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as AuthResponse;
    } catch {
      return null;
    }
  }

  getRole(): UserRole | null {
    const stored = this.getStoredUser();
    if (stored?.role) {
      return stored.role as UserRole;
    }

    const token = this.getToken();
    if (!token) {
      return null;
    }

    const payload = this.decodeToken(token);
    return (payload?.['role'] as UserRole | undefined) ?? null;
  }

  getDisplayName(): string {
    const stored = this.getStoredUser();
    if (stored?.prenom || stored?.nom) {
      return [stored.prenom, stored.nom].filter(Boolean).join(' ').trim();
    }

    return this.getEmail() || 'Utilisateur';
  }

  getEmail(): string | null {
    return this.getStoredUser()?.email ?? (this.decodeToken(this.getToken())?.['sub'] as string | undefined) ?? null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.AUTH_USER_KEY);
    this.clearPendingEmail();
  }

  private decodeToken(token: string | null): Record<string, string> | null {
    if (!token) {
      return null;
    }

    try {
      const base64Url = token.split('.')[1];
      if (!base64Url) {
        return null;
      }

      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((character) => `%${(`00${character.charCodeAt(0).toString(16)}`).slice(-2)}`)
          .join('')
      );

      return JSON.parse(jsonPayload) as Record<string, string>;
    } catch {
      return null;
    }
  }
}