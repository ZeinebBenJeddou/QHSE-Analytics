import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { ImportMode } from '../../shared/enums/import-mode.enum';
import { ImportSessionResponse } from '../../shared/models/import-session-response';
import { ApercuResponse } from '../../shared/models/apercu-response';
import { StagingDonneeResponse } from '../../shared/models/staging-donnee-response';
import { ResultatGlobalResponse } from '../../shared/models/resultat-global-response';
import { ColonneDetecteeResponse } from '../../shared/models/colonne-detectee-response';
import { UserMappingTemplateResponse } from '../../shared/models/user-mapping-template-response';
import { SaveMappingRequest } from '../../shared/models/save-mapping-request';
import { CorrectionRequest } from '../../shared/models/correction-request';
import { HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ImportService {
  constructor(private api: ApiService) {}

  // Template management
  downloadTemplate(): Observable<Blob> {
    return this.api.download('/imports/template/download');
  }

  // Column detection for free file mode
  detectColumns(file: File, ligneEntete: number): Observable<ColonneDetecteeResponse[]> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('ligneEntete', ligneEntete.toString());
    
    return this.api.post<ColonneDetecteeResponse[]>('/imports/detect-columns', formData);
  }

  // Mapping management
  getMappings(): Observable<UserMappingTemplateResponse[]> {
    return this.api.get<UserMappingTemplateResponse[]>('/imports/mappings');
  }

  saveMapping(request: SaveMappingRequest): Observable<UserMappingTemplateResponse> {
    return this.api.post<UserMappingTemplateResponse>('/imports/mappings', request);
  }

  deleteMapping(mappingId: number): Observable<any> {
    return this.api.delete(`/imports/mappings/${mappingId}`);
  }

  // Upload file with import mode
  upload(
    mode: ImportMode,
    mappingTemplateId: number | null,
    periodeN1: number,
    periodeN: number,
    file: File
  ): Observable<ImportSessionResponse> {
    const formData = new FormData();
    formData.append('mode', mode);
    if (mappingTemplateId) {
      formData.append('mappingTemplateId', mappingTemplateId.toString());
    }
    formData.append('periodeN1', periodeN1.toString());
    formData.append('periodeN', periodeN.toString());
    formData.append('file', file);

    return this.api.post<ImportSessionResponse>('/imports/upload', formData);
  }

  // Upload and automatically process (confirm & analyze)
  uploadAndProcess(
    mode: ImportMode,
    mappingTemplateId: number | null,
    periodeN1: number,
    periodeN: number,
    file: File
  ): Observable<ResultatGlobalResponse> {
    const formData = new FormData();
    formData.append('mode', mode);
    if (mappingTemplateId) {
      formData.append('mappingTemplateId', mappingTemplateId.toString());
    }
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
