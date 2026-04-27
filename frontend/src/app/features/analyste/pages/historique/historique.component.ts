import {
  Component, inject, OnInit, signal, computed
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ImportService } from '../../../../core/services/import.service';
import { HistoriqueItemResponse } from '../../../../core/models/import-session.model';

@Component({
  selector: 'app-historique',
  standalone: true,
  imports: [
    CommonModule, DatePipe,
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
      TRAITE: 'success', ERREUR: 'error', EN_TRAITEMENT: 'warn', ANNULE: 'default'
    };
    return map[statut] ?? 'default';
  }
}
