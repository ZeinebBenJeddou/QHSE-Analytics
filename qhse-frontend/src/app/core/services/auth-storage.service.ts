import { Injectable } from '@angular/core';
import { AuthResponse } from '../../shared/models/auth-response';

const ACCESS_TOKEN_KEY = 'QHSE_ACCESS_TOKEN';
const REFRESH_TOKEN_KEY = 'QHSE_REFRESH_TOKEN';
const USER_KEY = 'QHSE_USER_INFO';

@Injectable({
  providedIn: 'root'
})
export class AuthStorageService {
  saveAuthData(response: AuthResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify({
      email: response.email,
      nom: response.nom,
      prenom: response.prenom,
      role: response.role
    }));
  }

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  get user(): { email: string; nom: string; prenom: string; role: string } | null {
    const data = localStorage.getItem(USER_KEY);
    return data ? JSON.parse(data) : null;
  }

  authState(): boolean {
    return !!this.accessToken;
  }

  get userName(): string {
    const user = this.user;
    return user ? `${user.prenom} ${user.nom}` : '';
  }

  isAdmin(): boolean {
    return this.user?.role === 'ADMIN';
  }

  isAnalyste(): boolean {
    return this.user?.role === 'ANALYSTE';
  }

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
}
