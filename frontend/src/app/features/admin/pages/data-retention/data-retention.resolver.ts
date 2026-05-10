import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import { DataRetentionPolicyResponse } from '../../models/admin.models';

export const dataRetentionResolver: ResolveFn<DataRetentionPolicyResponse | null> = () => {
  return inject(AdminService).getDataRetention().pipe(catchError(() => of(null)));
};
