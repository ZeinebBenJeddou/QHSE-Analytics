import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AdminService } from '../../../../core/services/admin.service';
import { HistoriqueItemResponse } from '../../../../core/models/import-session.model';
 
@Component({
  selector: 'app-admin-historique',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './historique-admin.component.html',
  styleUrls: ['./historique-admin.component.css'],
})
export class AdminHistoriqueComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route        = inject(ActivatedRoute);
 
  items         = signal<HistoriqueItemResponse[]>([]);
  loading       = signal(false);
  error         = signal('');
  totalImports  = signal(0);
  totalTraites  = signal(0);
  totalErreurs  = signal(0);
 
  displayedColumns = ['analyste', 'nomFichier', 'periodes', 'dateImport', 'statut', 'critiques'];
 
  ngOnInit(): void {
    
    const resolved = this.route.snapshot.data['historique'];
    if (resolved) {
      this.applyResponse(resolved);
    } else {
      this.loadHistory();
    }
  }
 
  loadHistory(): void {
    this.loading.set(true);
    this.error.set('');
    this.adminService.getHistorique().subscribe({
      next:     (response) => { this.applyResponse(response); },
      error:    ()         => { this.error.set('Impossible de charger l\'historique des analyses.'); },
      complete: ()         => { this.loading.set(false); },
    });
  }
 
  private applyResponse(response: any): void {
    this.items.set(response.items);
    this.totalImports.set(response.totalImports);
    this.totalTraites.set(response.totalTraites);
    this.totalErreurs.set(response.totalErreurs);
    this.loading.set(false);
  }
 
  
  statutClass(statut: string): string {
    switch (statut) {
      case 'TRAITE':
      case 'READY_FOR_AI':
      case 'CALCULATED':
        return 'success';
      case 'ERREUR':
        return 'error';
      case 'EN_COURS':
        return 'warn';
      case 'ANNULE':
        return 'muted';
      default:
        return 'neutral';
    }
  }
 
 
  statutLabel(statut: string): string {
    const labels: Record<string, string> = {
      TRAITE:        'Traité',
      READY_FOR_AI:  'Prêt IA',
      CALCULATED:    'Calculé',
      ERREUR:        'Erreur',
      EN_COURS:      'En cours',
      ANNULE:        'Annulé',
    };
    return labels[statut] ?? statut;
  }
}