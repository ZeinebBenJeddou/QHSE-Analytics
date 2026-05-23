import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import { HistoriqueAnalysteResponse } from '../../../../core/models/import-session.model';

export const adminHistoriqueResolver: ResolveFn<HistoriqueAnalysteResponse | null> = () => {
  return inject(AdminService).getHistorique().pipe(
    catchError(() => of(null)),
  );
};
 