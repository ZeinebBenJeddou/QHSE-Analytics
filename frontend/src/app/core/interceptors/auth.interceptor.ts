import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenService } from '../services/token.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);
  const token = tokenService.getToken();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          tokenService.removeToken();
          router.navigate(['/auth/login'], { queryParams: { expired: true } });
          break;

        case 403:
          // Token valide mais rôle insuffisant — redirection dashboard
          router.navigate(['/dashboard']);
          break;

        case 429:
          // Rate limiting — le composant gère l'affichage du message
          console.warn('[Auth Interceptor] Trop de tentatives, rate limit atteint.');
          break;

        case 0:
          // Réseau indisponible / CORS bloqué
          console.error('[Auth Interceptor] Serveur inaccessible ou erreur réseau.');
          break;

        case 500:
        case 502:
        case 503:
          console.error(`[Auth Interceptor] Erreur serveur ${error.status}.`);
          break;
      }

      return throwError(() => error);
    })
  );
};
