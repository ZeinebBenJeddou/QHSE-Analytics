import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ImportService } from '../../../../core/services/import.service';
import { ImportUploadStateService } from '../../../../core/services/import-upload-state.service';
import { ImportProcessingResponse } from '../../../../core/models/import-session.model';

@Component({
  selector: 'app-import-mapping',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    MatTableModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './import-mapping.component.html',
  styleUrls: ['./import-mapping.component.css'],
})
export class ImportMappingComponent implements OnInit {
  private router = inject(Router);
  private importService = inject(ImportService);
  private uploadState = inject(ImportUploadStateService);
  private snackBar = inject(MatSnackBar);

  uploadStateData = signal<{ file: File; yearN: number; yearNMinus1: number; headers: string[]; detectedHeaders?: string[] } | null>(null);
  kpiColumn = signal<number | null>(null);
  categoryColumn = signal<number | null>(null);
  unitColumn = signal<number | null>(null);
  valueNColumn = signal<number | null>(null);
  valueNMinus1Column = signal<number | null>(null);
  errorMsg = signal('');
  processing = signal(false);
  previewResponse = signal<ImportProcessingResponse | null>(null);
  result = signal<ImportProcessingResponse | null>(null);
  readonly previewColumns = ['kpiName', 'valeurN1', 'valeurN', 'status', 'confidence', 'method', 'message'];

  ngOnInit() {
    const state = this.uploadState.getUpload();
    if (!state) {
      this.router.navigate(['/analyste/import']);
      return;
    }
    this.uploadStateData.set(state);
  }

  get steps() {
    return ['Importer', 'Mapper les colonnes', 'Traiter'];
  }

  get indexes(): number[] {
    const headers = this.uploadStateData()?.headers ?? [];
    return headers.map((_, index) => index);
  }

  get selectedIndexes(): number[] {
    return [
      this.kpiColumn(),
      this.categoryColumn(),
      this.unitColumn(),
      this.valueNColumn(),
      this.valueNMinus1Column(),
    ].filter((value): value is number => value !== null);
  }

  validateSelections(): boolean {
    if (
      this.kpiColumn() === null ||
      this.categoryColumn() === null ||
      this.unitColumn() === null ||
      this.valueNColumn() === null ||
      this.valueNMinus1Column() === null
    ) {
      this.errorMsg.set('Tous les champs obligatoires doivent être mappés.');
      return false;
    }

    const duplicates = this.selectedIndexes.filter((value, index, self) => self.indexOf(value) !== index);
    if (duplicates.length > 0) {
      this.errorMsg.set('Chaque colonne doit être sélectionnée une seule fois.');
      return false;
    }

    this.errorMsg.set('');
    return true;
  }

  async submitMapping() {
    if (!this.validateSelections()) {
      return;
    }

    const state = this.uploadStateData();
    if (!state) {
      this.errorMsg.set('État d’import manquant. Revenez à l’étape précédente.');
      return;
    }

    const mapping = {
      kpiNameIndex: this.kpiColumn()!,
      categoryIndex: this.categoryColumn()!,
      unitIndex: this.unitColumn()!,
      valueNIndex: this.valueNColumn()!,
      valueN1Index: this.valueNMinus1Column()!,
    };

    this.processing.set(true);
    this.errorMsg.set('');
    this.previewResponse.set(null);

    try {
      const response = await firstValueFrom(
        this.importService.previewImport(state.file, state.yearN, state.yearNMinus1, mapping)
      );
      this.previewResponse.set(response);
      this.uploadState.saveResponse(response);
      this.snackBar.open('Prévisualisation ready. Vérifiez puis confirmez.', 'OK', { duration: 3000 });
    } catch (err: any) {
      this.errorMsg.set(err?.error?.message ?? 'Erreur lors de la prévisualisation.');
      this.snackBar.open(this.errorMsg(), 'OK', { duration: 5000 });
    } finally {
      this.processing.set(false);
    }
  }

  async confirmImport() {
    const state = this.uploadStateData();
    const preview = this.previewResponse();
    if (!state || !preview) {
      this.errorMsg.set('Aucune prévisualisation disponible.');
      return;
    }

    const mapping = {
      kpiNameIndex: this.kpiColumn()!,
      categoryIndex: this.categoryColumn()!,
      unitIndex: this.unitColumn()!,
      valueNIndex: this.valueNColumn()!,
      valueN1Index: this.valueNMinus1Column()!,
    };

    this.processing.set(true);
    this.errorMsg.set('');

    try {
      const response = await firstValueFrom(
        this.importService.processManualImport(state.file, state.yearN, state.yearNMinus1, mapping)
      );
      this.result.set(response);
      this.uploadState.saveResponse(response);
      this.snackBar.open('Importation confirmée.', 'OK', { duration: 3000 });
    } catch (err: any) {
      this.errorMsg.set(err?.error?.message ?? 'Erreur lors de la confirmation.');
      this.snackBar.open(this.errorMsg(), 'OK', { duration: 5000 });
    } finally {
      this.processing.set(false);
    }
  }

  get isProcessingStep(): boolean {
    return this.processing();
  }

  getHeaderLabel(index: number): string {
    return this.uploadStateData()?.headers?.[index] ?? `Colonne ${index}`;
  }

  extractMethodBadgeClass(method: string | null | undefined): string {
    const normalized = method?.trim().toUpperCase() ?? '';
    if (normalized.includes('OCR')) {
      return 'badge-ocr';
    }
    if (normalized.includes('AI')) {
      return 'badge-ai';
    }
    if (normalized.includes('MANUAL')) {
      return 'badge-manual';
    }
    return 'badge-default';
  }

  qualityLabel(score: number | null | undefined): string {
    if (score === null || score === undefined || Number.isNaN(score)) {
      return 'N/A';
    }
    return `${Math.round(score)}%`;
  }

  invalidRateLabel(rawData: ImportProcessingResponse['rawData']): string {
    const items = rawData ?? [];
    if (!items.length) {
      return 'Aucune ligne détectée.';
    }
    const invalid = items.filter((row) => !row.valid).length;
    const rate = Math.round((invalid / items.length) * 100);
    return `${invalid} ligne(s) invalide(s) sur ${items.length} (${rate}%)`;
  }

  goBack() {
    this.router.navigate(['/analyste/import']);
  }

  goToDashboard() {
    const id = this.result()?.importSessionId ?? null;
    if (id) {
      this.router.navigate(['/analyste/dashboard', id]);
      return;
    }
    this.router.navigate(['/analyste/dashboard']);
  }
}
