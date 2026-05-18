import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ColumnProfileDTO, ImportProcessingResponse } from '../models/import-session.model';

export interface ImportUploadState {
  file: File;
  yearN: number;
  yearNMinus1: number;
  headers: string[];
  detectedHeaders?: string[];
}

export interface DualFileState {
  fileN1: File;
  fileN: File;
  yearN: number;
  yearNMinus1: number;
  columnsN1: ColumnProfileDTO[];
  columnsN: ColumnProfileDTO[];
}

@Injectable({ providedIn: 'root' })
export class ImportUploadStateService {
  private readonly uploadSubject = new BehaviorSubject<ImportUploadState | null>(null);
  private readonly responseSubject = new BehaviorSubject<ImportProcessingResponse | null>(null);
  private readonly dualSubject = new BehaviorSubject<DualFileState | null>(null);

  readonly upload$ = this.uploadSubject.asObservable();
  readonly response$ = this.responseSubject.asObservable();

  getUpload(): ImportUploadState | null { return this.uploadSubject.getValue(); }
  getResponse(): ImportProcessingResponse | null { return this.responseSubject.getValue(); }
  getDualState(): DualFileState | null { return this.dualSubject.getValue(); }

  saveUpload(state: ImportUploadState): void {
    this.clearResponse();
    this.uploadSubject.next(state);
  }

  saveDualState(state: DualFileState): void {
    this.clearResponse();
    this.dualSubject.next(state);
  }

  clearUpload(): void { this.uploadSubject.next(null); }
  clearDualState(): void { this.dualSubject.next(null); }

  saveResponse(response: ImportProcessingResponse): void {
    this.responseSubject.next(response);
  }

  clearResponse(): void { this.responseSubject.next(null); }
}
