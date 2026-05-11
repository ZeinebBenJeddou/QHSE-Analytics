import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnalyseCompleteResponse, AiAnalysisStructuredResponse } from '../models/analyse-ia.model';
import {
  MappingTemplateRequest,
  MappingTemplateResponse,
} from '../models/analyse-ia.model';

@Injectable({ providedIn: 'root' })
export class AiAnalysisService {
  private http = inject(HttpClient);
  private iaBase = `${environment.apiUrl}/api/ia`;
  private mappingBase = `${environment.apiUrl}/api/mapping`;

  getAnalyseComplete(importSessionId: number): Observable<AnalyseCompleteResponse> {
    return this.http.get<AnalyseCompleteResponse>(`${this.iaBase}/${importSessionId}`);
  }

  runAi(importSessionId: number): Observable<AnalyseCompleteResponse> {
    return this.http.post<AnalyseCompleteResponse>(`${this.iaBase}/${importSessionId}`, {});
  }

  regenerer(importSessionId: number): Observable<AnalyseCompleteResponse> {
    return this.http.post<AnalyseCompleteResponse>(`${this.iaBase}/${importSessionId}/regenerer`, {});
  }

  getStructuredAnalysis(importSessionId: number): Observable<AiAnalysisStructuredResponse> {
    return this.http.get<AiAnalysisStructuredResponse>(`${this.iaBase}/${importSessionId}/structured`);
  }

  saveMappingTemplate(req: MappingTemplateRequest): Observable<MappingTemplateResponse> {
    return this.http.post<MappingTemplateResponse>(`${this.mappingBase}/templates`, req);
  }

  getMappingTemplates(): Observable<MappingTemplateResponse[]> {
    return this.http.get<MappingTemplateResponse[]>(`${this.mappingBase}/templates`);
  }

  deleteMappingTemplate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.mappingBase}/templates/${id}`);
  }
}
