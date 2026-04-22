import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthStorageService } from '../services/auth-storage.service';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private authStorage: AuthStorageService,
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | Observable<boolean> {
    const checkAndRedirect = (): boolean => {
      const allowedRoles = route.data['roles'] as string[] | undefined;
      if (!allowedRoles || allowedRoles.length === 0) {
        return true;
      }
      const userRole = this.authStorage.userRole;
      const hasAccess = !!userRole && allowedRoles.includes(userRole);
      if (!hasAccess) {
        this.router.navigate(['/dashboard']);
      }
      return hasAccess;
    };

    if (this.authStorage.isAuthenticated()) {
      return checkAndRedirect();
    }

    const refreshToken = this.authStorage.refreshToken;
    if (!refreshToken) {
      this.router.navigate(['/login']);
      return false;
    }

    return this.authService.refreshToken({ refreshToken }).pipe(
      map(() => checkAndRedirect()),
      catchError(() => {
        this.authStorage.clear();
        this.router.navigate(['/login']);
        return of(false);
      })
    );
  }
}
