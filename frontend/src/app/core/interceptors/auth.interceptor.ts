import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenService } from '../services/token.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  // Send the HttpOnly access_token cookie automatically on every request.
  const authReq = req.clone({ withCredentials: true });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        tokenService.removeToken();
        router.navigate(['/auth/login'], { queryParams: { expired: true } });
      } else if (error.status === 403) {
        router.navigate(['/dashboard']);
      }

      return throwError(() => error);
    })
  );
};
