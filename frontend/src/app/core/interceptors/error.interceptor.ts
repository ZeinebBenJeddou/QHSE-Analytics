import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notify = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        return throwError(() => error);
      }

      const serverMessage: string | undefined =
        error.error?.message ?? error.error?.error;

      switch (error.status) {
        case 0:
          notify.error('Serveur inaccessible. Vérifiez votre connexion réseau.');
          break;

        case 400:
          notify.warning(serverMessage ?? 'Requête invalide. Vérifiez les données saisies.');
          break;

        case 404:
          notify.warning(serverMessage ?? 'Ressource introuvable.');
          break;

        case 409:
          notify.warning(serverMessage ?? 'Conflit : la ressource existe déjà.');
          break;

        case 413:
          notify.warning('Fichier trop volumineux. Taille maximale : 15 Mo.');
          break;

        case 422:
          notify.warning(serverMessage ?? 'Données non traitables. Vérifiez le format.');
          break;

        case 429:
          notify.warning('Trop de tentatives. Veuillez patienter avant de réessayer.');
          break;

        case 500:
          notify.error('Erreur interne du serveur. Veuillez réessayer plus tard.');
          break;

        case 502:
        case 503:
          notify.error('Service temporairement indisponible. Veuillez réessayer.');
          break;

        default:
          if (error.status >= 500) {
            notify.error(`Erreur serveur (${error.status}). Veuillez réessayer.`);
          }
          break;
      }

      return throwError(() => error);
    })
  );
};
