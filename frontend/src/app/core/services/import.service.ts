import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ImportSessionResponse,
  AutoImportResultResponse,
  ResultatGlobalResponse,
  HistoriqueAnalysteResponse,
} from '../models/import-session.model';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/imports`;

  /** Download the XLSX template */
  downloadTemplate(): Observable<Blob> {
    return this.http.get(`${this.base}/template/download`, { responseType: 'blob' });
  }

  /** Manual upload – returns the created ImportSession */
  upload(periodeN1: number, periodeN: number, file: File): Observable<ImportSessionResponse> {
    const form = new FormData();
    form.append('file', file);
    const params = new HttpParams().set('periodeN1', periodeN1).set('periodeN', periodeN);
    return this.http.post<ImportSessionResponse>(`${this.base}/upload`, form, { params });
  }

  /** Auto upload – fully automatic KPI matching */
  uploadAuto(periodeN1: number, periodeN: number, file: File): Observable<AutoImportResultResponse> {
    const form = new FormData();
    form.append('file', file);
    const params = new HttpParams().set('periodeN1', periodeN1).set('periodeN', periodeN);
    return this.http.post<AutoImportResultResponse>(`${this.base}/auto`, form, { params });
  }

  /** Get all import sessions for the current user */
  getHistorique(): Observable<ImportSessionResponse[]> {
    return this.http.get<ImportSessionResponse[]>(this.base);
  }

  /** Get detailed results for a specific import session */
  getResultats(sessionId: number): Observable<ResultatGlobalResponse> {
    return this.http.get<ResultatGlobalResponse>(`${this.base}/${sessionId}/resultats`);
  }

  /** Cancel an import session */
  annuler(sessionId: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.base}/${sessionId}`);
  }

  /** Get import history summary (analyste dashboard) */
  getHistoriqueAnalyste(): Observable<HistoriqueAnalysteResponse> {
    return this.http.get<HistoriqueAnalysteResponse>(`${environment.apiUrl}/api/dashboard/analyste/historique`);
  }
}
