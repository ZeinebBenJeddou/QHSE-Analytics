import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import { AiConfigResponse } from '../../models/admin.models';

export const iaConfigResolver: ResolveFn<AiConfigResponse[]> = () => {
  return inject(AdminService).getAiConfigs().pipe(catchError(() => of([])));
};
