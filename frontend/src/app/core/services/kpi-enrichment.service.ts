import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface KpiPreviewInput {
  kpiName: string;
  category?: string;
  unit?: string;
  definition?: string;
  valueN?: number;
  valueN1?: number;
  status?: string;
  commentaire?: string;
}

export interface KpiEnrichedResponse {
  previewId: number;
  importSessionId: number;
  kpiName: string;
  category?: string;
  unit?: string;
  definition?: string;
  valueN?: number;
  valueN1?: number;
  status?: string;
  commentaire?: string;
  variationPercent?: number;
  ecart?: number;

  analysisId?: number;
  riskLevel?: 'Faible' | 'Modéré' | 'Élevé';
  riskJustification?: string;
  issueDetected?: string;
  correctiveAction?: string;
  preventiveAction?: string;
  immediateAction?: string;
  immediatePriority?: 'Haute' | 'Moyenne' | 'Basse';
  requires8d?: boolean;
  eightDDetails?: string;  
  aiNote?: string;

  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class KpiEnrichmentService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/kpi-enrichment`;

  
  savePreview(importSessionId: number, inputs: KpiPreviewInput[]): Observable<any> {
    return this.http.post(`${this.base}/${importSessionId}/preview`, inputs);
  }

  analyseAll(importSessionId: number, force = false): Observable<KpiEnrichedResponse[]> {
    return this.http.post<KpiEnrichedResponse[]>(
      `${this.base}/${importSessionId}/analyse?force=${force}`,
      {}
    );
  }

 
  getEnrichedView(importSessionId: number): Observable<KpiEnrichedResponse[]> {
    return this.http.get<KpiEnrichedResponse[]>(`${this.base}/${importSessionId}`);
  }
}
