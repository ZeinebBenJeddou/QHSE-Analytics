import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { ProfilResponse } from '../../shared/models/profil-response';
import { UpdateProfilRequest } from '../../shared/models/update-profil-request';
import { ChangePasswordRequest } from '../../shared/models/change-password-request';

@Injectable({
  providedIn: 'root'
})
export class ProfilService {
  constructor(private api: ApiService) {}

  getProfil(): Observable<ProfilResponse> {
    return this.api.get<ProfilResponse>('/profil');
  }

  updateProfil(request: UpdateProfilRequest): Observable<ProfilResponse> {
    return this.api.put<ProfilResponse>('/profil', request);
  }

  changePassword(request: ChangePasswordRequest): Observable<any> {
    return this.api.post('/profil/change-password', request);
  }
}
