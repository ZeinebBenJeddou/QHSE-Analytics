import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';
import { AuthService } from '../services/auth.service';

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);
  const router = inject(Router);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const message = error.error?.message || error.error?.error || error.message || 'Une erreur inattendue est survenue.';

      if (error.status === 401) {
        authService.clearSession();
        router.navigate(['/auth/login']);
      }

      if (error.status !== 0) {
        notifications.error(message);
      }

      return throwError(() => error);
    })
  );
};