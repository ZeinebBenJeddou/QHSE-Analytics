import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly roleKey = 'auth_role';
  private readonly activeKey = 'auth_active';
  private readonly pendingEmailKey = 'pending_email';

  
  setToken(_token: string): void {
    sessionStorage.setItem(this.activeKey, 'true');
  }

  getToken(): string | null {
    return null;
  }

  removeToken(): void {
    sessionStorage.removeItem(this.roleKey);
    sessionStorage.removeItem(this.activeKey);
  }

  hasToken(): boolean {
    return sessionStorage.getItem(this.activeKey) === 'true';
  }

  setUserRole(role: string): void {
    sessionStorage.setItem(this.roleKey, role);
    sessionStorage.setItem(this.activeKey, 'true');
  }

  getUserRole(): string | null {
    return sessionStorage.getItem(this.roleKey);
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
