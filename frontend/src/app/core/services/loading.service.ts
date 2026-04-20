import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly pendingRequests = new BehaviorSubject(0);
  readonly loading$ = this.pendingRequests.asObservable().pipe(map((count) => count > 0));

  begin(): void {
    this.pendingRequests.next(this.pendingRequests.value + 1);
  }

  end(): void {
    this.pendingRequests.next(Math.max(this.pendingRequests.value - 1, 0));
  }
}