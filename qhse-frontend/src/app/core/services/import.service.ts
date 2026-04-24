import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { ImportSessionResponse } from '../../shared/models/import-session-response';
import { ApercuResponse } from '../../shared/models/apercu-response';
import { StagingDonneeResponse } from '../../shared/models/staging-donnee-response';
import { ResultatGlobalResponse } from '../../shared/models/resultat-global-response';
import { CorrectionRequest } from '../../shared/models/correction-request';

@Injectable({
  providedIn: 'root'
})
export class ImportService {
  constructor(private api: ApiService) {}

  // Template management
  downloadTemplate(): Observable<Blob> {
    return this.api.download('/imports/template/download');
  }

  // Upload file with template-only mode
  upload(
    periodeN1: number,
    periodeN: number,
    file: File
  ): Observable<ImportSessionResponse> {
    const formData = new FormData();
    formData.append('periodeN1', periodeN1.toString());
    formData.append('periodeN', periodeN.toString());
    formData.append('file', file);

    return this.api.post<ImportSessionResponse>('/imports/upload', formData);
  }

  // Upload and automatically process (confirm & analyze)
  uploadAndProcess(
    periodeN1: number,
    periodeN: number,
    file: File
  ): Observable<ResultatGlobalResponse> {
    const formData = new FormData();
    formData.append('periodeN1', periodeN1.toString());
    formData.append('periodeN', periodeN.toString());
    formData.append('file', file);

    return this.api.post<ResultatGlobalResponse>('/imports/upload-and-process', formData);
  }

  // Staging and preview
  getApercu(sessionId: number): Observable<ApercuResponse> {
    return this.api.get<ApercuResponse>(`/imports/${sessionId}/apercu`);
  }

  // Manual correction
  corriger(sessionId: number, request: CorrectionRequest): Observable<StagingDonneeResponse> {
    return this.api.patch<StagingDonneeResponse>(`/imports/${sessionId}/corriger`, request);
  }

  // Confirm import
  confirmer(sessionId: number): Observable<ResultatGlobalResponse> {
    return this.api.post<ResultatGlobalResponse>(`/imports/${sessionId}/confirmer`, {});
  }

  // Results
  getResultats(sessionId: number): Observable<ResultatGlobalResponse> {
    return this.api.get<ResultatGlobalResponse>(`/imports/${sessionId}/resultats`);
  }

  // History
  getHistorique(): Observable<ImportSessionResponse[]> {
    return this.api.get<ImportSessionResponse[]>('/imports');
  }

  deleteSession(sessionId: number): Observable<any> {
    return this.api.delete(`/imports/${sessionId}`);
  }
}
