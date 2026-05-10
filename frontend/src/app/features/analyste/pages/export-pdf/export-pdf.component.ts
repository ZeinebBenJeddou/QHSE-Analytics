import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ImportService } from '../../../../core/services/import.service';
import { HistoriqueItemResponse } from '../../../../core/models/import-session.model';

interface ReportSection {
  id: string;
  label: string;
  description: string;
  icon: string;
  checked: boolean;
}

@Component({
  selector: 'app-export-pdf',
  standalone: true,
  imports: [
    CommonModule, DatePipe, FormsModule,
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

  sections: ReportSection[] = [
    { id: 'synthese',   label: 'Synthèse exécutive',     description: 'Résumé et score global QHSE',       icon: 'summarize',    checked: true },
    { id: 'comparatif', label: 'Tableau comparatif N/N-1', description: 'Tous les KPIs avec variations',     icon: 'compare_arrows', checked: true },
    { id: 'ia',         label: 'Analyses IA',             description: 'Insights, causes et recommandations', icon: 'smart_toy',  checked: true },
    { id: 'plan',       label: 'Plan d\'action',          description: 'Actions correctives priorisées',     icon: 'task_alt',   checked: true },
    { id: 'categories', label: 'Résumé par catégorie',   description: 'Performance par domaine QHSE',       icon: 'category',   checked: true },
  ];

  selectedImport = computed(() => {
    const id = this.selectedImportId();
    return this.imports().find(i => i.importId === id) ?? null;
  });

  checkedCount = computed(() => this.sections.filter(s => s.checked).length);

  ngOnInit() {
    this.importService.getHistoriqueAnalyste().subscribe({
      next: res => {
        const ready = res.items.filter(i =>
          ['TRAITE', 'CALCULATED', 'READY_FOR_AI', 'IMPORTED'].includes(i.statut)
        );
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
    this.generating.set(true);
    this.lastGenerated.set(null);

    this.importService.exportAnalyste(id).subscribe({
      next: (blob: Blob) => {
        const imp = this.selectedImport();
        const fileName = `QHSE_Rapport_${imp?.periodeN1}_${imp?.periodeN}.pdf`;
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
      error: () => {
        this.generating.set(false);
        this.snackBar.open('Erreur lors de la génération du PDF.', 'Fermer', { duration: 4000 });
      }
    });
  }

  toggleAll(checked: boolean) {
    this.sections.forEach(s => s.checked = checked);
  }

  periodLabel(imp: HistoriqueItemResponse): string {
    return `${imp.periodeN1} → ${imp.periodeN}`;
  }

  statutClass(statut: string): string {
    const s = statut?.toUpperCase() ?? '';
    if (['TRAITE', 'CALCULATED', 'READY_FOR_AI', 'IMPORTED'].includes(s)) return 'ok';
    if (s === 'EN_TRAITEMENT') return 'pending';
    return 'error';
  }
}
