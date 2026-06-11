import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { TokenService } from '../services/token.service';
import { environment } from '../../../environments/environment';

let isRefreshing = false;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);
  const http = inject(HttpClient);

  const authReq = req.clone({ withCredentials: true });
  const refreshUrl = `${environment.apiBaseUrl}/refresh`;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthEndpoint = req.url.includes('/refresh') || req.url.includes('/login') || req.url.includes('/verify-otp');

      if (error.status === 401 && !isAuthEndpoint) {
        if (!isRefreshing) {
          isRefreshing = true;
          return http.post(refreshUrl, {}, { withCredentials: true }).pipe(
            switchMap(() => {
              isRefreshing = false;
              return next(authReq);
            }),
            catchError(() => {
              isRefreshing = false;
              tokenService.removeToken();
              router.navigate(['/auth/login'], { queryParams: { expired: true } });
              return throwError(() => error);
            })
          );
        }
        tokenService.removeToken();
        router.navigate(['/auth/login'], { queryParams: { expired: true } });
      }

      return throwError(() => error);
    })
  );
};
