import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthStorageService } from '../services/auth-storage.service';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(private authStorage: AuthStorageService, private authService: AuthService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (req.url.includes('/auth/refresh')) {
      return next.handle(req);
    }

    const token = this.authStorage.accessToken;

    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !this.isRefreshing && this.authStorage.refreshToken) {
          this.isRefreshing = true;
          this.refreshTokenSubject.next(null);

          return this.authService.refreshToken({ refreshToken: this.authStorage.refreshToken }).pipe(
            switchMap((response) => {
              this.isRefreshing = false;
              this.authStorage.setAuth(response);
              this.refreshTokenSubject.next(response.accessToken);
              return next.handle(
                req.clone({
                  setHeaders: {
                    Authorization: `Bearer ${response.accessToken}`
                  }
                })
              );
            }),
            catchError((err) => {
              this.isRefreshing = false;
              this.authStorage.clear();
              return throwError(() => err);
            })
          );
        } else if (this.isRefreshing) {
          return this.refreshTokenSubject.pipe(
            filter((token) => token != null),
            take(1),
            switchMap((token) => {
              return next.handle(
                req.clone({
                  setHeaders: {
                    Authorization: `Bearer ${token}`
                  }
                })
              );
            })
          );
        }

        return throwError(() => error);
      })
    );
  }
}
