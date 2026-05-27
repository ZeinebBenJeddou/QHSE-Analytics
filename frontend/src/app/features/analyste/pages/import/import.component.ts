import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, Subject, takeUntil } from 'rxjs';
import { Chart, registerables } from 'chart.js';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { ImportService } from '../../../../core/services/import.service';
import { ImportUploadStateService } from '../../../../core/services/import-upload-state.service';
import { ImportProcessingResponse, KpiCalculatedDTO } from '../../../../core/models/import-session.model';
import { AnalysteProfilService } from '../../../../core/services/analyste-profil.service';
import { AnalysteProfil } from '../../../../core/models/analyste-profil.model';

Chart.register(...registerables);

@Component({
  selector: 'app-import',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    MatExpansionModule,
  ],
  templateUrl: './import.component.html',
  styleUrls: ['./import.component.css'],
})
export class ImportComponent implements OnInit, AfterViewInit, OnDestroy {
  private importService = inject(ImportService);
  private profilService = inject(AnalysteProfilService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private uploadState = inject(ImportUploadStateService);

  readonly tableColumns = ['kpi', 'categorieCode', 'valeurN1', 'valeurN', 'variationPercentage', 'classification', 'seuilFaible', 'seuilCritique'];
  readonly tailleSiteOptions = ['< 50', '50-200', '200-500', '500+'];

  selectedFile = signal<File | null>(null);
  yearN = signal<number | null>(null);
  yearNMinus1 = signal<number | null>(null);
  yearError = signal('');
  private rawYearN = '';
  private rawYearNMinus1 = '';
  isSubmitting = signal(false);
  result = signal<ImportProcessingResponse | null>(null);
  errorMsg = signal('');
  isColumnMappingError = signal(false);

  contexte: AnalysteProfil = {};

  @ViewChild('barCanvas', { read: ElementRef }) barCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('lineCanvas', { read: ElementRef }) lineCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('pieCanvas', { read: ElementRef }) pieCanvas?: ElementRef<HTMLCanvasElement>;

  private barChart?: Chart<'bar'>;
  private lineChart?: Chart<'line'>;
  private pieChart?: Chart<'pie'>;
  private readonly destroy$ = new Subject<void>();

  get fileName(): string {
    return this.selectedFile()?.name ?? '';
  }

  get fileSize(): string {
    const b = this.selectedFile()?.size ?? 0;
    return b > 0 ? (b / 1024 / 1024).toFixed(2) + ' MB' : '';
  }

  get hasResult(): boolean {
    return !!this.result();
  }

  get importSessionId(): number | null {
    return this.result()?.importSessionId ?? null;
  }

  get isUploadDisabled(): boolean {
    return !this.selectedFile() || this.isSubmitting() || !this.isYearValid;
  }

  private get isYearValid(): boolean {
    return this.yearN() !== null && this.yearNMinus1() !== null && this.yearNMinus1() === this.yearN()! - 1;
  }

  get classificationSummary() {
    const counts = { FAIBLE: 0, MODERE: 0, CRITIQUE: 0 };
    this.result()?.calculatedData?.forEach((item) => {
      if (item.classification in counts) {
        counts[item.classification as keyof typeof counts]++;
      }
    });
    return counts;
  }

  classificationLabel(value: KpiCalculatedDTO['classification'] | null | undefined): string {
    switch (value) {
      case 'FAIBLE': return 'OK';
      case 'MODERE': return 'WARNING';
      case 'CRITIQUE': return 'CRITICAL';
      default: return 'UNKNOWN';
    }
  }

  classificationClass(value: KpiCalculatedDTO['classification'] | null | undefined): string {
    switch (value) {
      case 'FAIBLE': return 'faible';
      case 'MODERE': return 'modere';
      case 'CRITIQUE': return 'critique';
      default: return 'unknown';
    }
  }

  categoryLabel(value: string | null | undefined): string {
    return value?.trim() || 'UNKNOWN';
  }

  ngOnInit() {
    this.profilService.getProfil().subscribe({
      next: (profil) => { this.contexte = { ...profil }; },
      error: () => {},
    });
  }

  ngAfterViewInit() {
    this.uploadState.response$.pipe(takeUntil(this.destroy$)).subscribe(saved => {
      if (saved) {
        this.result.set(saved);
        this.buildCharts();
      }
    });
  }

  onFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (!file) return;
    const fileName = file.name.toLowerCase();
    if (!fileName.endsWith('.xlsx') && !fileName.endsWith('.xls')) {
      this.snackBar.open('Seuls les fichiers Excel .xlsx et .xls sont acceptés.', 'OK', { duration: 4000 });
      return;
    }
    this.selectedFile.set(file);
    this.result.set(null);
    this.errorMsg.set('');
    this.uploadState.clearUpload();
    this.uploadState.clearResponse();
    this.destroyCharts();
  }

  onYearNChange(value: string) {
    this.rawYearN = value;
    this.yearN.set(this.parseYear(value));
    this.updateYearError();
  }

  onYearNMinus1Change(value: string) {
    this.rawYearNMinus1 = value;
    this.yearNMinus1.set(this.parseYear(value));
    this.updateYearError();
  }

  private parseYear(value: string): number | null {
    const trimmed = value?.trim() ?? '';
    if (!trimmed) return null;
    const parsed = Number(trimmed);
    if (!Number.isInteger(parsed)) return null;
    if (parsed < 2000 || parsed > 2026) return null;
    return parsed;
  }

  private updateYearError() {
    const yearN = this.yearN();
    const yearNMinus1 = this.yearNMinus1();
    if (this.rawYearN.trim() && yearN === null) {
      this.yearError.set('L\'année N doit être comprise entre 2000 et 2026.');
      return;
    }
    if (this.rawYearNMinus1.trim() && yearNMinus1 === null) {
      this.yearError.set('L\'année N-1 doit être comprise entre 2000 et 2026.');
      return;
    }
    if (yearN === null || yearNMinus1 === null) { this.yearError.set(''); return; }
    if (yearNMinus1 !== yearN - 1) {
      this.yearError.set('L\'année N-1 doit être exactement l\'année N moins 1.');
      return;
    }
    this.yearError.set('');
  }

  async continueToMapping() {
    const file = this.selectedFile();
    if (!file) {
      this.snackBar.open('Veuillez sélectionner un fichier Excel.', 'OK', { duration: 3000 });
      return;
    }
    if (!this.isYearValid) {
      this.snackBar.open('Veuillez saisir des années valides pour N et N-1.', 'OK', { duration: 3000 });
      return;
    }

    this.isSubmitting.set(true);
    this.errorMsg.set('');
    this.isColumnMappingError.set(false);

    try {
      const response = await firstValueFrom(
        this.importService.previewImport(file, this.yearN()!, this.yearNMinus1()!, {})
      );

      const detectedHeaders = response.detectedHeaders?.length ? response.detectedHeaders : [];

      this.uploadState.saveUpload({
        file,
        yearN: this.yearN()!,
        yearNMinus1: this.yearNMinus1()!,
        headers: detectedHeaders,
        detectedHeaders,
        contexteSecteur:        this.contexte.secteurActivite,
        contexteTaille:         this.contexte.tailleSite,
        contexteCertifications: this.contexte.certifications,
        contexteObjectifs:      this.contexte.objectifsQhse,
        contexteReglementation: this.contexte.reglementation,
        contexteSpecifique:     this.contexte.contexteSpecifique,
      });
      this.router.navigate(['/analyste/import/mapping']);
    } catch (error: unknown) {
      const msg: string = (error instanceof HttpErrorResponse ? error.error?.message : null) ?? 'Erreur lors de la détection des en-têtes.';
      if (msg.includes('Colonnes non reconnues')) {
        this.isColumnMappingError.set(true);
      } else {
        this.errorMsg.set(msg);
        this.snackBar.open(msg, 'OK', { duration: 5000 });
      }
    } finally {
      this.isSubmitting.set(false);
    }
  }

  clear() {
    this.selectedFile.set(null);
    this.result.set(null);
    this.errorMsg.set('');
    this.isColumnMappingError.set(false);
    this.uploadState.clearUpload();
    this.uploadState.clearResponse();
    this.destroyCharts();
  }

  async goToDashboard() {
    const id = this.importSessionId;
    if (id) {
      this.router.navigate(['/analyste/dashboard', id]);
      return;
    }
    this.snackBar.open('Aucun import sélectionné pour le tableau de bord.', 'OK', { duration: 3000 });
    this.router.navigate(['/analyste/dashboard']);
  }

  goToIa() {
    const id = this.importSessionId;
    if (!id) {
      this.snackBar.open('Aucun import sélectionné pour l\'analyse IA.', 'OK', { duration: 3000 });
      return;
    }
    this.router.navigate(['/analyste/ia', id]);
  }

  private buildCharts() {
    this.destroyCharts();
    const response = this.result();
    if (!response) return;

    if (this.barCanvas && response.charts?.barCharts?.length) {
      const series = response.charts.barCharts[0];
      this.barChart = new Chart(this.barCanvas.nativeElement.getContext('2d')!, {
        type: 'bar',
        data: {
          labels: series.categories,
          datasets: [{ label: series.label, data: series.values, backgroundColor: 'rgba(37,99,235,0.7)', borderColor: 'rgba(37,99,235,1)', borderWidth: 1 }],
        },
        options: { responsive: true, maintainAspectRatio: false },
      });
    }

    if (this.lineCanvas && response.charts?.lineCharts?.length) {
      const series = response.charts.lineCharts[0];
      this.lineChart = new Chart(this.lineCanvas.nativeElement.getContext('2d')!, {
        type: 'line',
        data: {
          labels: series.categories,
          datasets: [{ label: series.label, data: series.values, borderColor: 'rgba(16,185,129,0.9)', backgroundColor: 'rgba(16,185,129,0.2)', tension: 0.4, fill: true }],
        },
        options: { responsive: true, maintainAspectRatio: false },
      });
    }

    if (this.pieCanvas && this.result()?.calculatedData?.length) {
      const counts = this.classificationSummary;
      this.pieChart = new Chart(this.pieCanvas.nativeElement.getContext('2d')!, {
        type: 'pie',
        data: {
          labels: ['FAIBLE', 'MODERE', 'CRITIQUE'],
          datasets: [{ data: [counts.FAIBLE, counts.MODERE, counts.CRITIQUE], backgroundColor: ['#22c55e', '#f97316', '#ef4444'] }],
        },
        options: { responsive: true, maintainAspectRatio: false },
      });
    }
  }

  private destroyCharts() {
    this.barChart?.destroy();
    this.lineChart?.destroy();
    this.pieChart?.destroy();
    this.barChart = undefined;
    this.lineChart = undefined;
    this.pieChart = undefined;
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.destroyCharts();
  }
}
