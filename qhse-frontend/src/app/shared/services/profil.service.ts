import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../constants';
import {
  ChangePasswordRequest,
  MessageResponse,
  Profil,
  UpdateProfilRequest
} from '../models/profil.models';

@Injectable({ providedIn: 'root' })
export class ProfilService {
  constructor(private http: HttpClient) {}

  getProfil(): Observable<Profil> {
    return this.http.get<Profil>(`${API_BASE_URL}/profil`);
  }

  updateProfil(request: UpdateProfilRequest): Observable<Profil> {
    return this.http.put<Profil>(`${API_BASE_URL}/profil`, request);
  }

  changePassword(request: ChangePasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API_BASE_URL}/profil/change-password`, request);
  }
}
