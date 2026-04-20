import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const allowedRoles = (route.data?.['roles'] as string[] | undefined) ?? [];
  const role = authService.getRole();

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } });
  }

  if (!allowedRoles.length || (role && allowedRoles.includes(role))) {
    return true;
  }

  return router.createUrlTree([role === 'ADMIN' ? '/admin' : '/analyst']);
};