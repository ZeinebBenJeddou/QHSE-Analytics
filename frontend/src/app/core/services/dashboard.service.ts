import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AlertesResponse,
  ComparatifTableauResponse,
  GraphiquesDataResponse,
  ResumeAnalysteResponse,
} from '../models/dashboard.model';
import { AnalyseCompleteResponse } from '../models/analyse-ia.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/dashboard/analyste`;

  /** Get resume of the most recent import session */
  getResume(): Observable<ResumeAnalysteResponse> {
    return this.http.get<ResumeAnalysteResponse>(`${this.base}/resume`);
  }

  /** Get N vs N-1 comparison table for given import */
  getComparatif(importId: number): Observable<ComparatifTableauResponse> {
    return this.http.get<ComparatifTableauResponse>(`${this.base}/comparatif/${importId}`);
  }

  /** Get chart data (bar, radar, top degraded KPIs) */
  getGraphiques(importId: number): Observable<GraphiquesDataResponse> {
    return this.http.get<GraphiquesDataResponse>(`${this.base}/graphiques/${importId}`);
  }

  /** Get complete AI analysis for given import */
  getAnalysesIa(importId: number): Observable<AnalyseCompleteResponse> {
    return this.http.get<AnalyseCompleteResponse>(`${this.base}/analyses/${importId}`);
  }

  /** Get critical KPI alerts for the latest import */
  getAlertes(): Observable<AlertesResponse> {
    return this.http.get<AlertesResponse>(`${this.base}/alertes`);
  }
}
