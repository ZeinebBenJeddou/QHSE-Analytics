import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import { ProfileResponse } from '../../models/admin.models';

export const adminProfileResolver: ResolveFn<ProfileResponse | null> = () => {
  return inject(AdminService).getCurrentProfile().pipe(
    catchError(() => of(null))
  );
};