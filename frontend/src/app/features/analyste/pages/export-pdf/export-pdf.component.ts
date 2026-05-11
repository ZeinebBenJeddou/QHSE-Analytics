import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ImportService } from '../../../../core/services/import.service';
import { HistoriqueItemResponse } from '../../../../core/models/import-session.model';

@Component({
  selector: 'app-export-pdf',
  standalone: true,
  imports: [
    CommonModule, DatePipe,
    MatIconModule, MatProgressSpinnerModule, MatTooltipModule, MatSnackBarModule,
  ],
  templateUrl: './export-pdf.component.html',
  styleUrls: ['./export-pdf.component.css'],
})
export class ExportPdfComponent implements OnInit {
  private importService = inject(ImportService);
  private snackBar = inject(MatSnackBar);

  imports = signal<HistoriqueItemResponse[]>([]);
  selectedImportId = signal<number | null>(null);
  loadingImports = signal(true);
  generating = signal(false);

  lastGenerated = signal<{ fileName: string; date: string; sizeKb: number } | null>(null);

  selectedImport = computed(() => {
    const id = this.selectedImportId();
    return this.imports().find(i => i.importId === id) ?? null;
  });

  ngOnInit() {
    this.importService.getHistoriqueAnalyste().subscribe({
      next: res => {
        const ready = res.items.filter(i => this.canExportPdf(i.statut));
        this.imports.set(ready);
        if (ready.length) {
          this.selectedImportId.set(ready[0].importId);
        }
        this.loadingImports.set(false);
      },
      error: () => {
        this.loadingImports.set(false);
        this.snackBar.open('Impossible de charger l\'historique.', '', { duration: 3000 });
      }
    });
  }

  generate() {
    const id = this.selectedImportId();
    if (!id) return;

    const imp = this.selectedImport();
    if (!imp || !this.canExportPdf(imp.statut)) {
      this.snackBar.open('Le PDF est disponible uniquement pour les imports prêts pour l\'IA ou traités.', 'Fermer', { duration: 4000 });
      return;
    }

    this.generating.set(true);
    this.lastGenerated.set(null);

    this.importService.exportAnalyste(id).subscribe({
      next: (blob: Blob) => {
        const fileName = `QHSE_Rapport_${imp.periodeN1}_${imp.periodeN}.pdf`;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);

        this.lastGenerated.set({
          fileName,
          date: new Date().toLocaleString('fr-FR'),
          sizeKb: Math.round(blob.size / 1024),
        });
        this.generating.set(false);
      },
      error: (error) => {
        this.generating.set(false);
        const message = error?.status === 404
          ? 'Aucune analyse IA disponible pour cet import. Export PDF impossible pour le moment.'
          : 'Erreur lors de la génération du PDF.';
        this.snackBar.open(message, 'Fermer', { duration: 4000 });
      }
    });
  }

  periodLabel(imp: HistoriqueItemResponse): string {
    return `${imp.periodeN1} → ${imp.periodeN}`;
  }

  statutClass(statut: string): string {
    const s = statut?.toUpperCase() ?? '';
    if (this.canExportPdf(s)) return 'ok';
    if (s === 'EN_TRAITEMENT') return 'pending';
    return 'error';
  }

  canExportPdf(statut: string): boolean {
    const s = statut?.toUpperCase() ?? '';
    return s === 'READY_FOR_AI' || s === 'TRAITE';
  }
}
