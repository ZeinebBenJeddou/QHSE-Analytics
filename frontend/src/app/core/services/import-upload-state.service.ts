import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ImportProcessingResponse } from '../models/import-session.model';

export interface ImportUploadState {
  file: File;
  yearN: number;
  yearNMinus1: number;
  headers: string[];
  detectedHeaders?: string[];
}

@Injectable({ providedIn: 'root' })
export class ImportUploadStateService {
  private readonly uploadSubject = new BehaviorSubject<ImportUploadState | null>(null);
  private readonly responseSubject = new BehaviorSubject<ImportProcessingResponse | null>(null);

  readonly upload$ = this.uploadSubject.asObservable();
  readonly response$ = this.responseSubject.asObservable();

  // Synchronous snapshot access — safe to use from ngOnInit / resolvers
  getUpload(): ImportUploadState | null { return this.uploadSubject.getValue(); }
  getResponse(): ImportProcessingResponse | null { return this.responseSubject.getValue(); }

  saveUpload(state: ImportUploadState): void {
    this.clearResponse();
    this.uploadSubject.next(state);
  }

  clearUpload(): void { this.uploadSubject.next(null); }

  saveResponse(response: ImportProcessingResponse): void {
    this.responseSubject.next(response);
  }

  clearResponse(): void { this.responseSubject.next(null); }
}
