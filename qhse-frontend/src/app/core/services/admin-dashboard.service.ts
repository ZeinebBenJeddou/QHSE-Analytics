import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import {
  AdminStatsResponse,
  AdminGraphiquesDataResponse,
  AdminRepartitionResponse,
  AdminAnalysteItemResponse,
  AdminKpiCritiqueResponse
} from '../../shared/models/admin-dashboard';

@Injectable({
  providedIn: 'root'
})
export class AdminDashboardService {
  constructor(private api: ApiService) {}

  getStats(): Observable<AdminStatsResponse> {
    return this.api.get<AdminStatsResponse>('/dashboard/admin/stats');
  }

  getAnalystes(): Observable<AdminAnalysteItemResponse[]> {
    return this.api.get<AdminAnalysteItemResponse[]>('/dashboard/admin/analystes');
  }

  getKpisCritiques(): Observable<AdminKpiCritiqueResponse[]> {
    return this.api.get<AdminKpiCritiqueResponse[]>('/dashboard/admin/kpis-critiques');
  }

  getRepartition(): Observable<AdminRepartitionResponse> {
    return this.api.get<AdminRepartitionResponse>('/dashboard/admin/repartition');
  }

  getGraphiques(): Observable<AdminGraphiquesDataResponse> {
    return this.api.get<AdminGraphiquesDataResponse>('/dashboard/admin/graphiques');
  }
}