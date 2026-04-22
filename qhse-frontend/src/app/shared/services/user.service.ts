import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../constants';
import {
  CreateUserRequest,
  MessageResponse,
  UpdateUserRequest,
  User,
  UserListResponse
} from '../models/user.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<UserListResponse> {
    return this.http.get<UserListResponse>(`${API_BASE_URL}/admin/users`);
  }

  getUserById(id: number): Observable<User> {
    return this.http.get<User>(`${API_BASE_URL}/admin/users/${id}`);
  }

  createUser(request: CreateUserRequest): Observable<User> {
    return this.http.post<User>(`${API_BASE_URL}/admin/users`, request);
  }

  updateUser(id: number, request: UpdateUserRequest): Observable<User> {
    return this.http.put<User>(`${API_BASE_URL}/admin/users/${id}`, request);
  }

  deleteUser(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${API_BASE_URL}/admin/users/${id}`);
  }

  verifyUser(id: number): Observable<User> {
    return this.http.patch<User>(`${API_BASE_URL}/admin/users/${id}/verify`, {});
  }

  activateUser(id: number): Observable<User> {
    return this.http.patch<User>(`${API_BASE_URL}/admin/users/${id}/activate`, {});
  }

  deactivateUser(id: number): Observable<User> {
    return this.http.patch<User>(`${API_BASE_URL}/admin/users/${id}/deactivate`, {});
  }

  promoteToAdmin(id: number): Observable<User> {
    return this.http.patch<User>(`${API_BASE_URL}/admin/users/${id}/promote`, {});
  }

  demoteToAnalyste(id: number): Observable<User> {
    return this.http.patch<User>(`${API_BASE_URL}/admin/users/${id}/demote`, {});
  }

  resetPassword(id: number): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API_BASE_URL}/admin/users/${id}/reset-password`, {});
  }
}
