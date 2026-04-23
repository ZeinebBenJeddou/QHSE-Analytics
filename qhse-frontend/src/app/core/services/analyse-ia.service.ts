import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnalyseCompleteResponse, AnalyseCategorieResponse, AnalyseGlobaleResponse } from '../../shared/models/analyse-ia-response';

@Injectable({
  providedIn: 'root'
})
export class AnalyseIaService {

  constructor(private http: HttpClient) { }

  getAnalyseComplete(importId: number): Observable<AnalyseCompleteResponse> {
    return this.http.get<AnalyseCompleteResponse>(`/api/ia/${importId}`);
  }

  getAnalysesCategories(importId: number): Observable<AnalyseCategorieResponse[]> {
    return this.http.get<AnalyseCategorieResponse[]>(`/api/ia/${importId}/categories`);
  }

  getAnalyseGlobale(importId: number): Observable<AnalyseGlobaleResponse> {
    return this.http.get<AnalyseGlobaleResponse>(`/api/ia/${importId}/globale`);
  }

  regenerer(importId: number): Observable<AnalyseCompleteResponse> {
    return this.http.post<AnalyseCompleteResponse>(`/api/ia/${importId}/regenerer`, {});
  }
}