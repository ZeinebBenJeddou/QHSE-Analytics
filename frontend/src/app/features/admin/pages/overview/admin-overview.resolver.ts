import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { forkJoin, catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';

export interface AdminOverviewData {
  stats:        any;
  analystes:    any[];
  kpisCritiques: any[];
  repartition:  any;
  graphiques:   any;
}

export const adminOverviewResolver: ResolveFn<AdminOverviewData> = () => {
  const svc = inject(AdminService);
  return forkJoin({
    stats:         svc.getStats().pipe(catchError(() => of(null))),
    analystes:     svc.getAnalystes().pipe(catchError(() => of([]))),
    kpisCritiques: svc.getKpisCritiques().pipe(catchError(() => of([]))),
    repartition:   svc.getRepartition().pipe(catchError(() => of(null))),
    graphiques:    svc.getGraphiques().pipe(catchError(() => of(null))),
  });
};