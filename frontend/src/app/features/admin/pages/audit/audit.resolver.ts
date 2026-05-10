import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import { AuditPageResponse } from '../../models/admin.models';

export const adminAuditResolver: ResolveFn<AuditPageResponse | null> = () => {
  return inject(AdminService).getAuditLog(0, 30).pipe(
    catchError(() => of(null)),
  );
};
