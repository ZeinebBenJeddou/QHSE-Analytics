import { Injectable, signal } from '@angular/core';
import { AuthResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthStorageService {
  private readonly accessTokenKey = 'qhse_access_token';
  private readonly refreshTokenKey = 'qhse_refresh_token';
  private readonly userNameKey = 'qhse_user_name';
  private readonly userRoleKey = 'qhse_user_role';

  readonly authState = signal<boolean>(this.hasAccessToken());

  get accessToken(): string | null {
    return localStorage.getItem(this.accessTokenKey);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  get userName(): string | null {
    return localStorage.getItem(this.userNameKey);
  }

  get userRole(): string | null {
    return localStorage.getItem(this.userRoleKey);
  }

  setAuth(auth: AuthResponse): void {
    localStorage.setItem(this.accessTokenKey, auth.accessToken);
    localStorage.setItem(this.refreshTokenKey, auth.refreshToken);
    localStorage.setItem(this.userNameKey, `${auth.prenom} ${auth.nom}`);
    localStorage.setItem(this.userRoleKey, auth.role);
    this.authState.set(true);
  }

  clear(): void {
    localStorage.removeItem(this.accessTokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.userNameKey);
    localStorage.removeItem(this.userRoleKey);
    this.authState.set(false);
  }

  isAuthenticated(): boolean {
    return this.authState();
  }

  private hasAccessToken(): boolean {
    return !!localStorage.getItem(this.accessTokenKey);
  }
}
