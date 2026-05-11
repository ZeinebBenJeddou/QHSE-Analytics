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
    if (tokenService.isAdmin()) {
      return true;
    }
  }

  router.navigate(['/auth/login']);
  return false;
};
