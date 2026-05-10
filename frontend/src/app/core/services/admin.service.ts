import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  AdminAnalysteItemResponse,
  AdminGraphiquesDataResponse,
  AdminKpiCritiqueResponse,
  AdminRepartitionResponse,
  AdminStatsResponse,
  AiConfigResponse,
  AiConfigUpdateRequest,
  AnalyseCompleteResponse,
  AuditPageResponse,
  ChangePasswordRequest,
  CategorieKpiResponse,
  CreateAnalysteRequest,
  CreateKpiRequest,
  DataRetentionPolicyResponse,
  IaHealthResponse,
  KpiResponse,
  MessageResponse,
  ProfileResponse,
  RagKnowledgeRequest,
  RagKnowledgeResponse,
  RagSearchTestRequest,
  RagSearchTestResultItem,
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

  getUser(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.adminBase}/users/${id}`);
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

  getInactiveKpis(): Observable<KpiResponse[]> {
    return this.http.get<KpiResponse[]>(`${this.kpiBase}/inactifs`);
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

  // ── RAG Knowledge Base ────────────────────────────────────────────────────

  getRagEntries(): Observable<RagKnowledgeResponse[]> {
    return this.http.get<RagKnowledgeResponse[]>(`${this.adminBase}/rag`);
  }

  createRagEntry(request: RagKnowledgeRequest): Observable<RagKnowledgeResponse> {
    return this.http.post<RagKnowledgeResponse>(`${this.adminBase}/rag`, request);
  }

  updateRagEntry(id: number, request: RagKnowledgeRequest): Observable<RagKnowledgeResponse> {
    return this.http.put<RagKnowledgeResponse>(`${this.adminBase}/rag/${id}`, request);
  }

  deleteRagEntry(id: number): Observable<void> {
    return this.http.delete<void>(`${this.adminBase}/rag/${id}`);
  }

  testRagSearch(request: RagSearchTestRequest): Observable<RagSearchTestResultItem[]> {
    return this.http.post<RagSearchTestResultItem[]>(`${this.adminBase}/rag/search`, request);
  }

  // ── Data Retention ───────────────────────────────────────────────────────

  getDataRetention(): Observable<DataRetentionPolicyResponse> {
    return this.http.get<DataRetentionPolicyResponse>(`${this.adminBase}/data-retention`);
  }

  triggerPurge(): Observable<DataRetentionPolicyResponse> {
    return this.http.post<DataRetentionPolicyResponse>(`${this.adminBase}/data-retention/purge`, {});
  }

  recalculateAnalyse(userId: number, importId: number): Observable<AnalyseCompleteResponse> {
    return this.http.post<AnalyseCompleteResponse>(
      `${this.dashboardBase}/analystes/${userId}/analyses/${importId}/regenerer`, {}
    );
  }

  // ── AI Config ─────────────────────────────────────────────────────────────

  getAiConfigs(): Observable<AiConfigResponse[]> {
    return this.http.get<AiConfigResponse[]>(`${this.adminBase}/ia/config`);
  }

  updateAiConfig(key: string, request: AiConfigUpdateRequest): Observable<AiConfigResponse> {
    return this.http.put<AiConfigResponse>(`${this.adminBase}/ia/config/${key}`, request);
  }

  // ── IA Health ─────────────────────────────────────────────────────────────

  getIaHealth(): Observable<IaHealthResponse> {
    return this.http.get<IaHealthResponse>(`${this.adminBase}/ia/health`);
  }

  clearIaCache(): Observable<void> {
    return this.http.delete<void>(`${this.adminBase}/ia/cache`);
  }

  clearProviderCooldown(name: string): Observable<void> {
    return this.http.delete<void>(`${this.adminBase}/ia/providers/${name}/cooldown`);
  }
}
