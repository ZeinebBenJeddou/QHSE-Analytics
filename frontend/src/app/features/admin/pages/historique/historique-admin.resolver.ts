import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
 
export const adminHistoriqueResolver: ResolveFn<any> = () => {
  return inject(AdminService).getHistorique().pipe(
    catchError(() => of(null)),
  );
};
 