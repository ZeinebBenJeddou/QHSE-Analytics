import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AnalyseCompleteResponse } from '../models/analyse-ia.model';
import {
  AdminAnalysteItemResponse,
  AdminGraphiquesDataResponse,
  AdminKpiCritiqueResponse,
  AdminRepartitionResponse,
  AdminStatsResponse,
  AuditPageResponse,
  ChangePasswordRequest,
  CategorieKpiResponse,
  CreateAnalysteRequest,
  CreateKpiRequest,
  KpiResponse,
  MessageResponse,
  ProfileResponse,
  UpdateKpiRequest,
  UpdateProfilRequest,
  UpdateUserRequest,
  UserListResponse,
  UserResponse
} from '../../features/admin/models/admin.models';
import { HistoriqueAnalysteResponse } from '../models/import-session.model';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly adminBase = `${environment.apiUrl}/api/admin`;
  private readonly kpiBase = `${environment.apiUrl}/api/kpis`;
  private readonly dashboardBase = `${environment.apiUrl}/api/dashboard/admin`;
  private readonly profilBase = `${environment.apiUrl}/api/profil`;
  private readonly exportBase = `${environment.apiUrl}/api/export`;

  constructor(private readonly http: HttpClient) {}

  getUsers(page = 0, size = 15, search?: string): Observable<UserListResponse> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search?.trim()) params = params.set('search', search.trim());
    return this.http.get<UserListResponse>(`${this.adminBase}/users`, { params });
  }

  getAuditLog(page = 0, size = 30): Observable<AuditPageResponse> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<AuditPageResponse>(`${this.adminBase}/audit`, { params });
  }

  createUser(request: CreateAnalysteRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.adminBase}/users`, request);
  }

  updateUser(id: number, request: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.adminBase}/users/${id}`, request);
  }

  deleteUser(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.adminBase}/users/${id}`);
  }

  verifyUser(id: number): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.adminBase}/users/${id}/verify`, {});
  }

  activateUser(id: number): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.adminBase}/users/${id}/activate`, {});
  }

  deactivateUser(id: number): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.adminBase}/users/${id}/deactivate`, {});
  }

  promoteToAdmin(id: number): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.adminBase}/users/${id}/promote`, {});
  }

  demoteToAnalyste(id: number): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.adminBase}/users/${id}/demote`, {});
  }

  resetUserPassword(id: number): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.adminBase}/users/${id}/reset-password`, {});
  }

  getStats(): Observable<AdminStatsResponse> {
    return this.http.get<AdminStatsResponse>(`${this.dashboardBase}/stats`);
  }

  getAnalystes(): Observable<AdminAnalysteItemResponse[]> {
    return this.http.get<AdminAnalysteItemResponse[]>(`${this.dashboardBase}/analystes`);
  }

  getKpisCritiques(): Observable<AdminKpiCritiqueResponse[]> {
    return this.http.get<AdminKpiCritiqueResponse[]>(`${this.dashboardBase}/kpis-critiques`);
  }

  getRepartition(): Observable<AdminRepartitionResponse> {
    return this.http.get<AdminRepartitionResponse>(`${this.dashboardBase}/repartition`);
  }

  getGraphiques(): Observable<AdminGraphiquesDataResponse> {
    return this.http.get<AdminGraphiquesDataResponse>(`${this.dashboardBase}/graphiques`);
  }

  getKpis(categorie?: string): Observable<KpiResponse[]> {
    let params = new HttpParams();
    if (categorie) params = params.set('categorie', categorie);
    return this.http.get<{ content: KpiResponse[] }>(this.kpiBase, { params }).pipe(
      map(page => page.content)
    );
  }

  getKpiCategories(): Observable<CategorieKpiResponse[]> {
    return this.http.get<CategorieKpiResponse[]>(`${this.kpiBase}/categories`);
  }

  getCurrentProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`${this.profilBase}`);
  }

  updateProfile(request: UpdateProfilRequest): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(`${this.profilBase}`, request);
  }

  changePassword(request: ChangePasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.profilBase}/change-password`, request);
  }

  createKpi(request: CreateKpiRequest): Observable<KpiResponse> {
    return this.http.post<KpiResponse>(this.kpiBase, request);
  }

  updateKpi(id: number, request: UpdateKpiRequest): Observable<KpiResponse> {
    return this.http.put<KpiResponse>(`${this.kpiBase}/${id}`, request);
  }

  deleteKpi(id: number): Observable<any> {
    return this.http.delete<any>(`${this.kpiBase}/${id}`);
  }

  restoreKpi(id: number): Observable<KpiResponse> {
    return this.http.patch<KpiResponse>(`${this.kpiBase}/${id}/restore`, {});
  }

  exportAdminPdf(): Observable<Blob> {
    return this.http.get(`${this.exportBase}/admin/global`, { responseType: 'blob' });
  }

  getAnalyses(userId: number, importId: number): Observable<AnalyseCompleteResponse> {
    return this.http.get<AnalyseCompleteResponse>(`${this.dashboardBase}/analystes/${userId}/analyses/${importId}`);
  }

  getHistorique(): Observable<HistoriqueAnalysteResponse> {
    return this.http.get<HistoriqueAnalysteResponse>(`${this.dashboardBase}/historique`);
  }


  recalculateAnalyse(userId: number, importId: number): Observable<AnalyseCompleteResponse> {
    return this.http.post<AnalyseCompleteResponse>(
      `${this.dashboardBase}/analystes/${userId}/analyses/${importId}/regenerer`, {}
    );
  }


}
