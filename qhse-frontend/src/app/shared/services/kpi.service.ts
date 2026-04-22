import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../constants';
import {
  CategorieKpi,
  CreateKpiRequest,
  Kpi,
  KpiDeleteResponse,
  UpdateKpiRequest
} from '../models/kpi.models';

@Injectable({ providedIn: 'root' })
export class KpiService {
  constructor(private http: HttpClient) {}

  getKpis(categorie?: string): Observable<Kpi[]> {
    let params = new HttpParams();
    if (categorie) {
      params = params.set('categorie', categorie);
    }
    return this.http.get<Kpi[]>(`${API_BASE_URL}/kpis`, { params });
  }

  getInactiveKpis(): Observable<Kpi[]> {
    return this.http.get<Kpi[]>(`${API_BASE_URL}/kpis/inactifs`);
  }

  getAllCategories(): Observable<CategorieKpi[]> {
    return this.http.get<CategorieKpi[]>(`${API_BASE_URL}/kpis/categories`);
  }

  getKpiById(id: number): Observable<Kpi> {
    return this.http.get<Kpi>(`${API_BASE_URL}/kpis/${id}`);
  }

  createKpi(request: CreateKpiRequest): Observable<Kpi> {
    return this.http.post<Kpi>(`${API_BASE_URL}/kpis`, request);
  }

  updateKpi(id: number, request: UpdateKpiRequest): Observable<Kpi> {
    return this.http.put<Kpi>(`${API_BASE_URL}/kpis/${id}`, request);
  }

  deleteKpi(id: number): Observable<KpiDeleteResponse> {
    return this.http.delete<KpiDeleteResponse>(`${API_BASE_URL}/kpis/${id}`);
  }

  restoreKpi(id: number): Observable<Kpi> {
    return this.http.patch<Kpi>(`${API_BASE_URL}/kpis/${id}/restore`, {});
  }
}
