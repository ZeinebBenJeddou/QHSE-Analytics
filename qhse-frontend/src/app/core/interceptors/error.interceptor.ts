import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private snackBar: MatSnackBar) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        let message = 'Une erreur s\'est produite';

        if (error.error?.message) {
          message = error.error.message;
        } else if (error.error?.error) {
          message = error.error.error;
        } else if (error.status === 0) {
          message = 'Erreur de connexion au serveur';
        } else if (error.status === 400) {
          message = error.error?.message || 'Requête invalide';
        } else if (error.status === 403) {
          message = 'Accès refusé';
        } else if (error.status === 404) {
          message = 'Ressource non trouvée';
        } else if (error.status === 500) {
          message = 'Erreur serveur';
        }

        this.snackBar.open(message, 'Fermer', {
          duration: 5000,
          horizontalPosition: 'end',
          verticalPosition: 'bottom',
          panelClass: ['error-snackbar']
        });

        return throwError(() => error);
      })
    );
  }
}
