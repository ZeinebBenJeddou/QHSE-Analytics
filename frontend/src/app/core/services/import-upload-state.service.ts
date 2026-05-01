import { Injectable } from '@angular/core';
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
  private state: ImportUploadState | null = null;
  private lastResponse: ImportProcessingResponse | null = null;

  saveUpload(state: ImportUploadState): void {
    this.clearResponse();
    this.state = state;
  }

  getUpload(): ImportUploadState | null {
    return this.state;
  }

  clearUpload(): void {
    this.state = null;
  }

  saveResponse(response: ImportProcessingResponse): void {
    this.lastResponse = response;
  }

  getResponse(): ImportProcessingResponse | null {
    return this.lastResponse;
  }

  clearResponse(): void {
    this.lastResponse = null;
  }
}
