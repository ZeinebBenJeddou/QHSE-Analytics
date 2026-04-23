import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { KpiResponse } from '../../shared/models/kpi-response';
import { CreateKpiRequest } from '../../shared/models/create-kpi-request';
import { UpdateKpiRequest } from '../../shared/models/update-kpi-request';
import { HttpParams } from '@angular/common/http';

export interface CategorieKpiResponse {
  code: string;
  libelle: string;
}

@Injectable({
  providedIn: 'root'
})
export class KpiService {
  constructor(private api: ApiService) {}

  getKpis(categorie?: string): Observable<KpiResponse[]> {
    let params = new HttpParams();
    if (categorie) {
      params = params.set('categorie', categorie);
    }
    return this.api.get<KpiResponse[]>('/kpis', params);
  }

  getCategories(): Observable<CategorieKpiResponse[]> {
    return this.api.get<CategorieKpiResponse[]>('/kpis/categories');
  }

  getKpiById(id: number): Observable<KpiResponse> {
    return this.api.get<KpiResponse>(`/kpis/${id}`);
  }

  createKpi(request: CreateKpiRequest): Observable<KpiResponse> {
    return this.api.post<KpiResponse>('/kpis', request);
  }

  updateKpi(id: number, request: UpdateKpiRequest): Observable<KpiResponse> {
    return this.api.put<KpiResponse>(`/kpis/${id}`, request);
  }

  deleteKpi(id: number): Observable<any> {
    return this.api.delete(`/kpis/${id}`);
  }

  restoreKpi(id: number): Observable<KpiResponse> {
    return this.api.patch<KpiResponse>(`/kpis/${id}/restore`, {});
  }

  getInactiveKpis(): Observable<KpiResponse[]> {
    return this.api.get<KpiResponse[]>('/kpis/inactifs');
  }
}
