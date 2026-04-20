import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { CategorieKpiResponse, KpiResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class KpiService {
  private readonly api = 'http://localhost:8080/api/kpis';

  constructor(private readonly http: HttpClient) {}

  getKpis(categorie?: string) {
    const params = categorie ? new HttpParams().set('categorie', categorie) : undefined;
    return this.http.get<KpiResponse[]>(this.api, { params });
  }

  getCategories() {
    return this.http.get<CategorieKpiResponse[]>(`${this.api}/categories`);
  }

  getInactiveKpis() {
    return this.http.get<KpiResponse[]>(`${this.api}/inactifs`);
  }
}