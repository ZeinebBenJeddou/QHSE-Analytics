import {
  Component, inject, OnInit, signal
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ImportService } from '../../../../core/services/import.service';
import { HistoriqueItemResponse } from '../../../../core/models/import-session.model';

@Component({
  selector: 'app-historique',
  standalone: true,
  imports: [
    CommonModule, DatePipe, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatChipsModule, MatTooltipModule,
    MatProgressSpinnerModule, MatDialogModule, MatSnackBarModule,
  ],
  templateUrl: './historique.component.html',
  styleUrls: ['./historique.component.css'],
})
export class HistoriqueComponent implements OnInit {
  private importService = inject(ImportService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  items = signal<HistoriqueItemResponse[]>([]);
  loading = signal(true);
  error = signal('');

  columns = ['nomFichier', 'periodes', 'dateImport', 'statut', 'critiques', 'actions'];
  downloadingImportId = signal<number | null>(null);

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.importService.getHistoriqueAnalyste().subscribe({
      next: res => {
        this.items.set(res.items);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger l\'historique.');
        this.loading.set(false);
      }
    });
  }

  viewDashboard(item: HistoriqueItemResponse) {
    this.router.navigate(['/analyste/dashboard', item.importId]);
  }

  viewIA(item: HistoriqueItemResponse) {
    this.router.navigate(['/analyste/ia', item.importId]);
  }

  exportPdf(item: HistoriqueItemResponse) {
    if (!this.canExportPdf(item.statut)) {
      this.snackBar.open('Le PDF est disponible uniquement pour les imports prêts pour l\'IA ou traités.', 'OK', { duration: 4000 });
      return;
    }

    this.downloadingImportId.set(item.importId);
    this.importService.exportAnalyste(item.importId).subscribe({
      next: (blob) => {
        this.downloadBlob(blob, `rapport-import-${item.importId}.pdf`);
        this.snackBar.open('Export PDF lancé.', 'OK', { duration: 3000 });
      },
      error: (error) => {
        const message = error?.status === 404
          ? 'Aucune analyse IA disponible pour cet import. Export PDF impossible pour le moment.'
          : 'Erreur lors de l\'export PDF.';
        this.snackBar.open(message, 'OK', { duration: 4000 });
      },
      complete: () => this.downloadingImportId.set(null),
    });
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }

  goToImport(): void {
    this.router.navigate(['/analyste/import']);
  }

  delete(item: HistoriqueItemResponse) {
    if (!confirm(`Supprimer l'import du fichier "${item.nomFichier}" ?`)) return;
    this.importService.annuler(item.importId).subscribe({
      next: () => {
        this.snackBar.open('Import supprimé', 'OK', { duration: 3000 });
        this.load();
      },
      error: () => this.snackBar.open('Erreur lors de la suppression', 'OK', { duration: 3000 }),
    });
  }

  statutColor(statut: string): string {
    const map: Record<string, string> = {
      TRAITE: 'success', READY_FOR_AI: 'success', CALCULATED: 'accent', IMPORTED: 'primary', ERREUR: 'error', EN_TRAITEMENT: 'warn', ANNULE: 'default'
    };
    return map[statut] ?? 'default';
  }

  statutLabel(statut: string): string {
    const map: Record<string, string> = {
      TRAITE: 'Traité',
      READY_FOR_AI: 'Prêt pour l\'IA',
      CALCULATED: 'Calculé',
      IMPORTED: 'Importé',
      EN_TRAITEMENT: 'En traitement',
      ERREUR: 'Erreur',
      ANNULE: 'Annulé',
    };
    return map[statut] ?? statut;
  }

  canExportPdf(statut: string): boolean {
    return statut === 'READY_FOR_AI' || statut === 'TRAITE';
  }
}
