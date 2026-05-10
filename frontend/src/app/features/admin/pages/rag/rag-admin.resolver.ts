import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import { RagKnowledgeResponse } from '../../models/admin.models';

export const ragAdminResolver: ResolveFn<RagKnowledgeResponse[] | null> = () => {
  return inject(AdminService).getRagEntries().pipe(catchError(() => of(null)));
};
