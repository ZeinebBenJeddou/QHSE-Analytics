import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import { IaHealthResponse } from '../../models/admin.models';

export const iaHealthResolver: ResolveFn<IaHealthResponse | null> = () => {
  return inject(AdminService).getIaHealth().pipe(catchError(() => of(null)));
};
