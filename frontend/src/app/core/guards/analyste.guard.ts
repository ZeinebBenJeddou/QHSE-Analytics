import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';

export const analysteGuard: CanActivateFn = (): boolean => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (tokenService.hasToken() && (tokenService.isAnalyste() || tokenService.isAdmin())) {
    return true;
  }

  if (tokenService.hasToken() && tokenService.isAdmin()) {
     router.navigate(['/admin']);
     return false;
  }

  router.navigate(['/auth/login']);
  return false;
};
