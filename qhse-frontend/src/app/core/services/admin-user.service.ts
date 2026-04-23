import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { UserResponse } from '../../shared/models/user-response';
import { UserListResponse } from '../../shared/models/user-list-response';
import { CreateAnalysteRequest } from '../../shared/models/create-analyste-request';
import { UpdateUserRequest } from '../../shared/models/update-user-request';

@Injectable({
  providedIn: 'root'
})
export class AdminUserService {
  constructor(private api: ApiService) {}

  getUsers(): Observable<UserListResponse> {
    return this.api.get<UserListResponse>('/admin/users');
  }

  getUserById(id: number): Observable<UserResponse> {
    return this.api.get<UserResponse>(`/admin/users/${id}`);
  }

  createAnalyste(request: CreateAnalysteRequest): Observable<UserResponse> {
    return this.api.post<UserResponse>('/admin/users', request);
  }

  updateUser(id: number, request: UpdateUserRequest): Observable<UserResponse> {
    return this.api.put<UserResponse>(`/admin/users/${id}`, request);
  }

  deleteUser(id: number): Observable<any> {
    return this.api.delete(`/admin/users/${id}`);
  }

  verifyUser(id: number): Observable<UserResponse> {
    return this.api.patch<UserResponse>(`/admin/users/${id}/verify`, {});
  }

  activateUser(id: number): Observable<UserResponse> {
    return this.api.patch<UserResponse>(`/admin/users/${id}/activate`, {});
  }

  deactivateUser(id: number): Observable<UserResponse> {
    return this.api.patch<UserResponse>(`/admin/users/${id}/deactivate`, {});
  }

  promoteUser(id: number): Observable<UserResponse> {
    return this.api.patch<UserResponse>(`/admin/users/${id}/promote`, {});
  }

  demoteUser(id: number): Observable<UserResponse> {
    return this.api.patch<UserResponse>(`/admin/users/${id}/demote`, {});
  }

  resetPassword(id: number): Observable<any> {
    return this.api.post(`/admin/users/${id}/reset-password`, {});
  }
}
