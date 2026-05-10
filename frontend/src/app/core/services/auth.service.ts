import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  ForgotPasswordRequest,
  GenericResponse,
  LoginRequest,
  OtpRequest,
  RegisterRequest,
  ResetPasswordRequest,
  VerifyResponse
} from '../../features/auth/models/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  register(request: RegisterRequest): Observable<GenericResponse> {
    return this.http.post<GenericResponse>(`${this.baseUrl}/register`, request);
  }

  verifyAccount(token: string): Observable<VerifyResponse> {
    const params = new HttpParams().set('token', token);
    return this.http.get<VerifyResponse>(`${this.baseUrl}/verify`, { params });
  }

  login(request: LoginRequest): Observable<GenericResponse> {
    return this.http.post<GenericResponse>(`${this.baseUrl}/login`, request);
  }

  verifyOtp(request: OtpRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/verify-otp`, request);
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<GenericResponse> {
    return this.http.post<GenericResponse>(`${this.baseUrl}/forgot-password`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<GenericResponse> {
    return this.http.post<GenericResponse>(`${this.baseUrl}/reset-password`, request);
  }

  logout(): Observable<GenericResponse> {
    return this.http.post<GenericResponse>(`${this.baseUrl}/logout`, {});
  }
}
