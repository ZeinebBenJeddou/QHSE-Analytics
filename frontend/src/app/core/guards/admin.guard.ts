import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';

export const adminGuard: CanActivateFn = (): boolean => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (!tokenService.hasToken()) {
    router.navigate(['/auth/login']);
    return false;
  }

  if (!tokenService.isAdmin()) {
    router.navigate(['/analyste/dashboard']);
    return false;
  }

  return true;
};
