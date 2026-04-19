import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private API = 'http://localhost:8080/api/auth';
  private readonly PENDING_EMAIL_KEY = 'pendingEmail';

  constructor(private http: HttpClient) {}

  register(data: any) {
    return this.http.post(`${this.API}/register`, data);
  }

  login(data: any) {
    return this.http.post(`${this.API}/login`, data);
  }

  verifyAccount(token: string) {
    return this.http.get(`${this.API}/verify`, { params: { token } });
  }

  resendVerification(email: string) {
    return this.http.post(`${this.API}/resend-verification`, null, { params: { email } });
  }

  verifyOtp(data: any) {
    return this.http.post(`${this.API}/verify-otp`, data);
  }

  resendOtp(email: string) {
    return this.http.post(`${this.API}/resend-otp`, null, { params: { email } });
  }

  forgotPassword(email: string) {
    return this.http.post(`${this.API}/forgot-password`, { email });
  }

  resetPassword(data: any) {
    return this.http.post(`${this.API}/reset-password`, data);
  }

  saveToken(token: string) {
    localStorage.setItem('token', token);
  }

  setPendingEmail(email: string) {
    sessionStorage.setItem(this.PENDING_EMAIL_KEY, email);
  }

  getPendingEmail() {
    return sessionStorage.getItem(this.PENDING_EMAIL_KEY);
  }

  clearPendingEmail() {
    sessionStorage.removeItem(this.PENDING_EMAIL_KEY);
  }

  getToken() {
    return localStorage.getItem('token');
  }

  logout() {
    localStorage.clear();
    this.clearPendingEmail();
  }
}