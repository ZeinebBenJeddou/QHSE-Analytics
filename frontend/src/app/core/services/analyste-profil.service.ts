import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnalysteProfil } from '../models/analyste-profil.model';

@Injectable({ providedIn: 'root' })
export class AnalysteProfilService {
  private http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/api/profil/qhse-context`;

  getProfil(): Observable<AnalysteProfil> {
    return this.http.get<AnalysteProfil>(this.url);
  }

  saveProfil(profil: AnalysteProfil): Observable<AnalysteProfil> {
    return this.http.put<AnalysteProfil>(this.url, profil);
  }
}
