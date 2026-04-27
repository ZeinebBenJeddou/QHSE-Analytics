import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly tokenKey = 'auth_token';
  private readonly roleKey = 'auth_role';
  private readonly pendingEmailKey = 'pending_email';

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  removeToken(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.roleKey);
  }

  hasToken(): boolean {
    return !!this.getToken();
  }

  setUserRole(role: string): void {
    localStorage.setItem(this.roleKey, role);
  }

  getUserRole(): string | null {
    return localStorage.getItem(this.roleKey);
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  isAnalyste(): boolean {
    return this.getUserRole() === 'ANALYSTE';
  }

  savePendingEmail(email: string): void {
    localStorage.setItem(this.pendingEmailKey, email);
  }

  getPendingEmail(): string | null {
    return localStorage.getItem(this.pendingEmailKey);
  }

  clearPendingEmail(): void {
    localStorage.removeItem(this.pendingEmailKey);
  }
}
