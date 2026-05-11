import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ColumnProfileDTO, HistoriqueAnalysteResponse, ImportProcessingResponse } from '../models/import-session.model';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private http = inject(HttpClient);
  private manualBase = `${environment.apiUrl}/api/import/manual`;
  private importBase = `${environment.apiUrl}/api/imports`;

  uploadFile(file: File, yearN: number, yearNMinus1: number): Observable<ImportProcessingResponse> {
    const form = new FormData();
    form.append('file', file);
    form.append('yearN', String(yearN));
    form.append('yearN1', String(yearNMinus1));
    return this.http.post<ImportProcessingResponse>(this.manualBase, form);
  }

  previewImport(file: File, yearN: number, yearNMinus1: number, mapping: Record<string, number> = {}): Observable<ImportProcessingResponse> {
    const form = new FormData();
    form.append('file', file);
    form.append('yearN', String(yearN));
    form.append('yearN1', String(yearNMinus1));
    form.append('mapping', JSON.stringify(mapping));
    return this.http.post<ImportProcessingResponse>(`${this.manualBase}/preview`, form);
  }

  processManualImport(file: File, yearN: number, yearNMinus1: number, mapping: Record<string, number>, allowPartialImport = false, clientId?: string): Observable<ImportProcessingResponse> {
    const form = new FormData();
    form.append('file', file);
    form.append('yearN', String(yearN));
    form.append('yearN1', String(yearNMinus1));
    form.append('mapping', JSON.stringify(mapping));
    if (allowPartialImport) {
      form.append('allowPartialImport', 'true');
    }
    const url = clientId ? `${this.manualBase}?clientId=${encodeURIComponent(clientId)}` : this.manualBase;
    return this.http.post<ImportProcessingResponse>(url, form);
  }

  progressStreamUrl(clientId: string): string {
    return `${this.manualBase}/progress?clientId=${encodeURIComponent(clientId)}`;
  }

  profileColumns(file: File): Observable<ColumnProfileDTO[]> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ColumnProfileDTO[]>(`${this.manualBase}/profile`, form);
  }

  confirmStrict(file: File, yearN: number, yearNMinus1: number, mapping: Record<string, number>, clientId?: string): Observable<ImportProcessingResponse> {
    return this.processManualImport(file, yearN, yearNMinus1, mapping, false, clientId);
  }

  confirmPartial(file: File, yearN: number, yearNMinus1: number, mapping: Record<string, number>, clientId?: string): Observable<ImportProcessingResponse> {
    return this.processManualImport(file, yearN, yearNMinus1, mapping, true, clientId);
  }

  annuler(sessionId: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.importBase}/${sessionId}`);
  }

  getHistoriqueAnalyste(): Observable<HistoriqueAnalysteResponse> {
    return this.http.get<HistoriqueAnalysteResponse>(`${environment.apiUrl}/api/dashboard/analyste/historique`);
  }

  exportAnalyste(importId: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/api/export/analyste/${importId}`, { responseType: 'blob' });
  }
}
