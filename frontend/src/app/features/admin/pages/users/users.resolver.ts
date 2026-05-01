import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { AdminService } from '../../../../core/services/admin.service';
import { UserResponse } from '../../models/admin.models';
 
export const adminUsersResolver: ResolveFn<UserResponse[]> = () => {
  return inject(AdminService).getUsers().pipe(
    map((response) => response.users),
    catchError(() => of([])),
  );
};