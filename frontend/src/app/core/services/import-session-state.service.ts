import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AnalyseCompleteResponse } from '../models/analyse-ia.model';
import {
  ComparatifTableauResponse,
  GraphiquesDataResponse,
  ResumeAnalysteResponse,
} from '../models/dashboard.model';

export interface ImportDashboardData {
  comparatif: ComparatifTableauResponse | null;
  graphiques: GraphiquesDataResponse | null;
  analysesIa: AnalyseCompleteResponse | null;
  resume: ResumeAnalysteResponse | null;
}

const EMPTY: ImportDashboardData = {
  comparatif: null,
  graphiques: null,
  analysesIa: null,
  resume: null,
};

@Injectable({ providedIn: 'root' })
export class ImportSessionStateService {
  private readonly importIdSubject = new BehaviorSubject<number | null>(null);
  private readonly dataSubject = new BehaviorSubject<ImportDashboardData>({ ...EMPTY });

  readonly activeImportId$ = this.importIdSubject.asObservable();
  readonly dashboardData$ = this.dataSubject.asObservable();

  getActiveImportId(): number | null { return this.importIdSubject.getValue(); }
  getDashboardData(): ImportDashboardData { return this.dataSubject.getValue(); }

  /**
   * Set the active import. Clears cached data automatically when the ID changes,
   * so stale data from a previous import is never served.
   */
  setActiveImport(id: number | null): void {
    if (this.importIdSubject.getValue() !== id) {
      this.importIdSubject.next(id);
      this.dataSubject.next({ ...EMPTY });
    }
  }

  /** Merge a partial update into the current data snapshot. */
  patch(data: Partial<ImportDashboardData>): void {
    this.dataSubject.next({ ...this.dataSubject.getValue(), ...data });
  }

  clear(): void {
    this.importIdSubject.next(null);
    this.dataSubject.next({ ...EMPTY });
  }
}
