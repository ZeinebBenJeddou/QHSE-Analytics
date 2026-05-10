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
    // Admin can view analyste pages for oversight
    if (tokenService.isAdmin()) {
      return true;
    }
  }

  // Not authenticated as analyst → go to login
  router.navigate(['/auth/login']);
  return false;
};
