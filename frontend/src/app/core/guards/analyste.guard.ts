import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';

export const analysteGuard: CanActivateFn = (): boolean => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (tokenService.hasToken()) {
    if (tokenService.isAnalyste()) {
      return true;
    }
    // If user is admin, redirect to admin area
    if (tokenService.isAdmin()) {
      router.navigate(['/admin']);
      return false;
    }
  }

  // Not authenticated as analyst → go to login
  router.navigate(['/auth/login']);
  return false;
};
