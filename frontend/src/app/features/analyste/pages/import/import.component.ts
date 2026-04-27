import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ImportService } from '../../../../core/services/import.service';
import { AutoImportResultResponse } from '../../../../core/models/import-session.model';

@Component({
  selector: 'app-import',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressBarModule,
    MatSelectModule, MatChipsModule, MatSnackBarModule, MatTooltipModule,
  ],
  templateUrl: './import.component.html',
  styleUrls: ['./import.component.css'],
})
export class ImportComponent {
  private importService = inject(ImportService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  selectedFile = signal<File | null>(null);
  isDragOver = signal(false);
  isUploading = signal(false);
  progress = signal(0);
  result = signal<AutoImportResultResponse | null>(null);
  errorMsg = signal('');

  periodeN1 = new Date().getFullYear() - 1;
  periodeN = new Date().getFullYear();
  mode: 'auto' | 'manuel' = 'auto';

  years = Array.from({ length: 10 }, (_, i) => new Date().getFullYear() - i);

  get fileName(): string {
    return this.selectedFile()?.name ?? '';
  }

  get fileSize(): string {
    const b = this.selectedFile()?.size ?? 0;
    return b > 0 ? (b / 1024 / 1024).toFixed(2) + ' MB' : '';
  }

  onDragOver(e: DragEvent) { e.preventDefault(); this.isDragOver.set(true); }
  onDragLeave() { this.isDragOver.set(false); }

  onDrop(e: DragEvent) {
    e.preventDefault();
    this.isDragOver.set(false);
    const file = e.dataTransfer?.files[0];
    if (file) this.setFile(file);
  }

  onFileSelected(e: Event) {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) this.setFile(file);
  }

  setFile(file: File) {
    if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.csv')) {
      this.snackBar.open('Seuls les fichiers .xlsx et .csv sont acceptés', 'OK', { duration: 4000 });
      return;
    }
    this.selectedFile.set(file);
    this.result.set(null);
    this.errorMsg.set('');
  }

  upload() {
    const file = this.selectedFile();
    if (!file || !this.periodeN1 || !this.periodeN) return;

    this.isUploading.set(true);
    this.progress.set(0);
    this.errorMsg.set('');
    this.result.set(null);

    const interval = setInterval(() => {
      this.progress.update(p => Math.min(p + 8, 85));
    }, 400);

    const call$: Observable<any> = this.mode === 'auto'
      ? this.importService.uploadAuto(this.periodeN1, this.periodeN, file)
      : this.importService.upload(this.periodeN1, this.periodeN, file);

    call$.subscribe({
      next: (res: any) => {
        clearInterval(interval);
        this.progress.set(100);
        this.isUploading.set(false);
        if (this.mode === 'auto') {
          this.result.set(res as AutoImportResultResponse);
        } else {
          this.router.navigate(['/analyste/mapping'], { queryParams: { sessionId: (res as any).id } });
        }
      },
      error: (err: any) => {
        clearInterval(interval);
        this.isUploading.set(false);
        this.progress.set(0);
        this.errorMsg.set(err?.error?.message ?? "Erreur lors de l'import. Vérifiez le fichier.");
      },
    });
  }

  goToDashboard() {
    const res = this.result();
    if (res) this.router.navigate(['/analyste/dashboard', res.session.id]);
  }

  downloadTemplate() {
    this.importService.downloadTemplate().subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'template_qhse_v1.xlsx'; a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  reset() {
    this.selectedFile.set(null);
    this.result.set(null);
    this.errorMsg.set('');
    this.progress.set(0);
  }
}
