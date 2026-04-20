import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ImportService } from '../../core/services/import.service';
import { AuthService } from '../../core/services/auth.service';
import {
  ApercuResponse,
  ImportMode,
  ImportSessionResponse,
  ResultatGlobalResponse,
  StagingDonneeResponse,
  UserMappingTemplateResponse,
} from '../../core/models/api.models';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-analyst',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './analyst.component.html',
  styleUrls: ['./analyst.component.css']
})
export class AnalystComponent implements OnInit {
  readonly importModes: ImportMode[] = ['TEMPLATE_OFFICIEL', 'FICHIER_LIBRE'];

  userName = '';
  mappings: UserMappingTemplateResponse[] = [];
  history: ImportSessionResponse[] = [];
  currentApercu: ApercuResponse | null = null;
  currentResults: ResultatGlobalResponse | null = null;
  editableRows: EditableStagingRow[] = [];
  currentSessionId: number | null = null;
  message = '';
  selectedMode: ImportMode = 'TEMPLATE_OFFICIEL';
  selectedMappingId: number | null = null;
  periodeN1 = new Date().getFullYear() - 1;
  periodeN = new Date().getFullYear();
  selectedFile: File | null = null;
  isUploading = false;
  isRefreshing = false;

  constructor(
    private readonly importService: ImportService,
    private readonly notificationService: NotificationService,
    private readonly authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.userName = this.authService.getDisplayName();
    this.loadInitialData();
  }

  loadInitialData(): void {
    this.isRefreshing = true;

    this.importService.getMappings().subscribe({
      next: (mappings) => {
        this.mappings = mappings;
      }
    });

    this.importService.getHistorique().subscribe({
      next: (history) => {
        this.history = history;
        if (!this.currentSessionId && history.length) {
          this.openSession(history[0].id);
        }
        this.isRefreshing = false;
      },
      error: () => {
        this.isRefreshing = false;
      }
    });
  }

  chooseFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  downloadTemplate(): void {
    this.importService.downloadTemplate().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'template_qhse_v1.xlsx';
        link.click();
        window.URL.revokeObjectURL(url);
        this.notificationService.success('Modèle téléchargé.');
      }
    });
  }

  uploadImport(): void {
    if (!this.selectedFile) {
      this.notificationService.error('Sélectionnez un fichier Excel avant de lancer l’import.');
      return;
    }

    if (!this.periodeN1 || !this.periodeN) {
      this.notificationService.error('Renseignez les périodes N-1 et N.');
      return;
    }

    this.isUploading = true;
    this.importService.uploadImport({
      file: this.selectedFile,
      mode: this.selectedMode,
      periodeN1: this.periodeN1,
      periodeN: this.periodeN,
      mappingTemplateId: this.selectedMappingId,
    }).subscribe({
      next: (session) => {
        this.isUploading = false;
        this.currentSessionId = session.id;
        this.message = `Import ${session.nomFichier} enregistré.`;
        this.notificationService.success('Import lancé avec succès.');
        this.loadApercu(session.id);
        this.loadHistory();
      },
      error: () => {
        this.isUploading = false;
      }
    });
  }

  loadHistory(): void {
    this.importService.getHistorique().subscribe({
      next: (history) => {
        this.history = history;
      }
    });
  }

  openSession(sessionId: number): void {
    this.currentSessionId = sessionId;
    this.loadApercu(sessionId);
    this.loadResults(sessionId);
  }

  loadApercu(sessionId: number): void {
    this.importService.getApercu(sessionId).subscribe({
      next: (response) => {
        this.currentApercu = response;
        this.editableRows = response.donnees.map((row) => ({
          ...row,
          valeurN1Edit: row.valeurN1,
          valeurNEdit: row.valeurN,
        }));
      }
    });
  }

  loadResults(sessionId: number): void {
    this.importService.getResultats(sessionId).subscribe({
      next: (response) => {
        this.currentResults = response;
      }
    });
  }

  saveCorrection(row: EditableStagingRow): void {
    if (!this.currentSessionId) {
      return;
    }

    this.importService.corriger(this.currentSessionId, {
      stagingDonneeId: row.id,
      valeurN1: this.parseNumber(row.valeurN1Edit),
      valeurN: this.parseNumber(row.valeurNEdit),
    }).subscribe({
      next: (updated) => {
        this.notificationService.success(`Correction appliquée pour ${updated.kpiNom}.`);
        this.loadApercu(this.currentSessionId as number);
      }
    });
  }

  confirmImport(): void {
    if (!this.currentSessionId) {
      this.notificationService.error('Aucun import actif à confirmer.');
      return;
    }

    this.importService.confirmer(this.currentSessionId).subscribe({
      next: (response) => {
        this.currentResults = response;
        this.notificationService.success('Import confirmé et résultats enregistrés.');
        this.loadHistory();
      }
    });
  }

  deleteMapping(mappingId: number): void {
    this.importService.deleteMapping(mappingId).subscribe({
      next: () => {
        this.notificationService.success('Mapping supprimé.');
        this.loadInitialData();
      }
    });
  }

  refreshCurrentSession(): void {
    if (this.currentSessionId) {
      this.loadApercu(this.currentSessionId);
      this.loadResults(this.currentSessionId);
    }
  }

  statusLabel(status?: string): string {
    switch (status) {
      case 'OK': return 'Conforme';
      case 'CORRIGE': return 'Corrigé';
      case 'MANQUANT': return 'Manquant';
      case 'INVALIDE': return 'Invalide';
      case 'SUSPECT': return 'Suspect';
      default: return 'Ignoré';
    }
  }

  sessionBadge(session: ImportSessionResponse): string {
    return `${session.nombreTotal} lignes · ${session.nombreOk} ok · ${session.nombreManquant} manquantes`;
  }

  private parseNumber(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
}

interface EditableStagingRow extends StagingDonneeResponse {
  valeurN1Edit?: number | string | null;
  valeurNEdit?: number | string | null;
}
