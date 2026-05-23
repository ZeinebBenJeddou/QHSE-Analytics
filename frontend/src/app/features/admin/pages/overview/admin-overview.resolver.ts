import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { forkJoin, catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import {
  AdminStatsResponse,
  AdminAnalysteItemResponse,
  AdminKpiCritiqueResponse,
  AdminRepartitionResponse,
  AdminGraphiquesDataResponse,
} from '../../../admin/models/admin.models';

export interface AdminOverviewData {
  stats:         AdminStatsResponse | null;
  analystes:     AdminAnalysteItemResponse[];
  kpisCritiques: AdminKpiCritiqueResponse[];
  repartition:   AdminRepartitionResponse | null;
  graphiques:    AdminGraphiquesDataResponse | null;
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