import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  AdminAnalysteItemResponse,
  AdminGraphiquesDataResponse,
  AdminKpiCritiqueResponse,
  AdminStatsResponse,
  CreateAnalysteRequest,
  MessageResponse,
  UpdateUserRequest,
  UserListResponse,
  UserResponse,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly api = 'http://localhost:8080/api/admin';

  constructor(private readonly http: HttpClient) {}

  getUsers() {
    return this.http.get<UserListResponse>(`${this.api}/users`);
  }

  getUserById(id: number) {
    return this.http.get<UserResponse>(`${this.api}/users/${id}`);
  }

  createAnalyste(request: CreateAnalysteRequest) {
    return this.http.post<UserResponse>(`${this.api}/users`, request);
  }

  updateUser(id: number, request: UpdateUserRequest) {
    return this.http.put<UserResponse>(`${this.api}/users/${id}`, request);
  }

  deleteUser(id: number) {
    return this.http.delete<MessageResponse>(`${this.api}/users/${id}`);
  }

  verifyUser(id: number) {
    return this.http.patch<UserResponse>(`${this.api}/users/${id}/verify`, {});
  }

  activateUser(id: number) {
    return this.http.patch<UserResponse>(`${this.api}/users/${id}/activate`, {});
  }

  deactivateUser(id: number) {
    return this.http.patch<UserResponse>(`${this.api}/users/${id}/deactivate`, {});
  }

  promoteToAdmin(id: number) {
    return this.http.patch<UserResponse>(`${this.api}/users/${id}/promote`, {});
  }

  demoteToAnalyste(id: number) {
    return this.http.patch<UserResponse>(`${this.api}/users/${id}/demote`, {});
  }

  resetPassword(id: number) {
    return this.http.post<MessageResponse>(`${this.api}/users/${id}/reset-password`, {});
  }

  getStats() {
    return this.http.get<AdminStatsResponse>(`http://localhost:8080/api/dashboard/admin/stats`);
  }

  getAnalystes() {
    return this.http.get<AdminAnalysteItemResponse[]>(`http://localhost:8080/api/dashboard/admin/analystes`);
  }

  getKpisCritiques() {
    return this.http.get<AdminKpiCritiqueResponse[]>(`http://localhost:8080/api/dashboard/admin/kpis-critiques`);
  }

  getGraphiques() {
    return this.http.get<AdminGraphiquesDataResponse>(`http://localhost:8080/api/dashboard/admin/graphiques`);
  }
}