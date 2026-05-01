import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { forkJoin, catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import { KpiResponse, CategorieKpiResponse } from '../../models/admin.models';
 
export interface KpisResolvedData {
  kpis: KpiResponse[];
  categories: CategorieKpiResponse[];
}
 
export const adminKpisResolver: ResolveFn<KpisResolvedData> = () => {
  const adminService = inject(AdminService);
  return forkJoin({
    kpis:       adminService.getKpis().pipe(catchError(() => of([]))),
    categories: adminService.getKpiCategories().pipe(catchError(() => of([]))),
  });
};
 