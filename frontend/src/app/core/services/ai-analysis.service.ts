import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnalyseCompleteResponse } from '../models/analyse-ia.model';
import {
  MappingTemplateRequest,
  MappingTemplateResponse,
} from '../models/analyse-ia.model';

@Injectable({ providedIn: 'root' })
export class AiAnalysisService {
  private http = inject(HttpClient);
  private iaBase = `${environment.apiUrl}/api/ia`;
  private mappingBase = `${environment.apiUrl}/api/mapping`;

  /** Get or trigger complete AI analysis for an import session */
  getAnalyseComplete(importSessionId: number): Observable<AnalyseCompleteResponse> {
    return this.http.get<AnalyseCompleteResponse>(`${this.iaBase}/${importSessionId}/complete`);
  }

  /** Regenerate all AI analyses */
  regenerer(importSessionId: number): Observable<AnalyseCompleteResponse> {
    return this.http.post<AnalyseCompleteResponse>(`${this.iaBase}/${importSessionId}/regenerer`, {});
  }

  /** Save a column-mapping template */
  saveMappingTemplate(req: MappingTemplateRequest): Observable<MappingTemplateResponse> {
    return this.http.post<MappingTemplateResponse>(`${this.mappingBase}/templates`, req);
  }

  /** Load all mapping templates for the current user */
  getMappingTemplates(): Observable<MappingTemplateResponse[]> {
    return this.http.get<MappingTemplateResponse[]>(`${this.mappingBase}/templates`);
  }

  /** Delete a mapping template */
  deleteMappingTemplate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.mappingBase}/templates/${id}`);
  }
}
