import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly tokenKey = 'auth_token';
  private readonly pendingEmailKey = 'pending_email';

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  removeToken(): void {
    localStorage.removeItem(this.tokenKey);
  }

  hasToken(): boolean {
    return !!this.getToken();
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
