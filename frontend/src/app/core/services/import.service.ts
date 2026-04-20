import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  ApercuResponse,
  CorrectionRequest,
  ImportMode,
  ImportSessionResponse,
  MessageResponse,
  ResultatGlobalResponse,
  SaveMappingRequest,
  StagingDonneeResponse,
  UserMappingTemplateResponse,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly api = 'http://localhost:8080/api/imports';

  constructor(private readonly http: HttpClient) {}

  downloadTemplate() {
    return this.http.get(`${this.api}/template/download`, { responseType: 'blob' });
  }

  detectColumns(file: File, ligneEntete: number) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('ligneEntete', String(ligneEntete));
    return this.http.post(`${this.api}/detect-columns`, formData);
  }

  getMappings() {
    return this.http.get<UserMappingTemplateResponse[]>(`${this.api}/mappings`);
  }

  saveMapping(request: SaveMappingRequest) {
    return this.http.post<UserMappingTemplateResponse>(`${this.api}/mappings`, request);
  }

  deleteMapping(mappingId: number) {
    return this.http.delete<MessageResponse>(`${this.api}/mappings/${mappingId}`);
  }

  uploadImport(payload: {
    file: File;
    mode: ImportMode;
    periodeN1: number;
    periodeN: number;
    mappingTemplateId?: number | null;
  }) {
    const formData = new FormData();
    formData.append('mode', payload.mode);
    formData.append('periodeN1', String(payload.periodeN1));
    formData.append('periodeN', String(payload.periodeN));
    formData.append('file', payload.file);

    const params = new HttpParams();
    const queryParams = payload.mappingTemplateId ? params.set('mappingTemplateId', String(payload.mappingTemplateId)) : params;

    return this.http.post<ImportSessionResponse>(`${this.api}/upload`, formData, { params: queryParams });
  }

  getApercu(sessionId: number) {
    return this.http.get<ApercuResponse>(`${this.api}/${sessionId}/apercu`);
  }

  corriger(sessionId: number, request: CorrectionRequest) {
    return this.http.patch<StagingDonneeResponse>(`${this.api}/${sessionId}/corriger`, request);
  }

  confirmer(sessionId: number) {
    return this.http.post<ResultatGlobalResponse>(`${this.api}/${sessionId}/confirmer`, null);
  }

  getHistorique() {
    return this.http.get<ImportSessionResponse[]>(this.api);
  }

  getResultats(sessionId: number) {
    return this.http.get<ResultatGlobalResponse>(`${this.api}/${sessionId}/resultats`);
  }

  deleteImport(sessionId: number) {
    return this.http.delete<MessageResponse>(`${this.api}/${sessionId}`);
  }
}