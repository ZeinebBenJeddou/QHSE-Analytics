import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, of, tap } from 'rxjs';
import { AdminService } from './admin.service';
import { ProfileResponse } from '../../features/admin/models/admin.models';

@Injectable({ providedIn: 'root' })
export class CurrentProfileStateService {
  private readonly profileSubject = new BehaviorSubject<ProfileResponse | null>(null);

  readonly profile$ = this.profileSubject.asObservable();

  constructor(private readonly adminService: AdminService) {}

  getSnapshot(): ProfileResponse | null {
    return this.profileSubject.getValue();
  }

  setProfile(profile: ProfileResponse | null): void {
    this.profileSubject.next(profile);
  }

  refresh(): Observable<ProfileResponse | null> {
    return this.adminService.getCurrentProfile().pipe(
      tap(profile => this.profileSubject.next(profile)),
      catchError(() => of(null))
    );
  }
}