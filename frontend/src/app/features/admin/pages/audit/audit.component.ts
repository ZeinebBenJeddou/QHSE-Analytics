import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminService } from '../../../../core/services/admin.service';
import { AuditLogResponse, AuditPageResponse } from '../../models/admin.models';

const ACTION_LABELS: Record<string, string> = {
  CREER_ANALYSTE:       'Créer analyste',
  MODIFIER_UTILISATEUR: 'Modifier utilisateur',
  SUPPRIMER_UTILISATEUR:'Supprimer utilisateur',
  VERIFIER_COMPTE:      'Vérifier compte',
  ACTIVER_COMPTE:       'Activer compte',
  DESACTIVER_COMPTE:    'Désactiver compte',
  PROMOUVOIR_ADMIN:     'Promouvoir admin',
  RETROGRADER_ANALYSTE: 'Rétrograder analyste',
  REINITIALISER_MDP:    'Réinitialiser MDP',
};

const ACTION_CLASS: Record<string, string> = {
  CREER_ANALYSTE:       'chip-create',
  SUPPRIMER_UTILISATEUR:'chip-delete',
  DESACTIVER_COMPTE:    'chip-warn',
  PROMOUVOIR_ADMIN:     'chip-promote',
  RETROGRADER_ANALYSTE: 'chip-demote',
};

@Component({
  selector: 'app-admin-audit',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatTableModule, MatTooltipModule,
  ],
  providers: [DatePipe],
  templateUrl: './audit.component.html',
  styleUrls: ['./audit.component.css'],
})
export class AdminAuditComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route        = inject(ActivatedRoute);
  private readonly datePipe     = inject(DatePipe);

  logs: AuditLogResponse[] = [];
  loading       = false;
  error         = '';
  currentPage   = 0;
  totalPages    = 0;
  totalElements = 0;
  readonly pageSize = 30;

  displayedColumns = ['timestamp', 'admin', 'action', 'cible', 'details'];

  ngOnInit(): void {
    const resolved = this.route.snapshot.data['audit'] as AuditPageResponse | null;
    if (resolved) {
      this.applyPage(resolved);
    } else {
      this.loadPage(0);
    }
  }

  actionLabel(action: string): string { return ACTION_LABELS[action] ?? action; }
  actionClass(action: string): string { return ACTION_CLASS[action] ?? 'chip-neutral'; }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || this.loading) return;
    this.loadPage(page);
  }

  exportCsv(): void {
    if (!this.logs.length) return;

    const header = ['Date', 'Heure', 'Administrateur', 'Action', 'Utilisateur cible', 'Détails'];
    const rows = this.logs.map(l => [
      this.datePipe.transform(l.timestamp, 'dd/MM/yyyy') ?? '',
      this.datePipe.transform(l.timestamp, 'HH:mm:ss')  ?? '',
      l.adminEmail,
      this.actionLabel(l.action),
      l.targetEmail ?? '',
      (l.details ?? '').replace(/,/g, ';'),
    ]);

    const csv = [header, ...rows]
      .map(r => r.map(v => `"${v}"`).join(','))
      .join('\n');

    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `audit_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  private loadPage(page: number): void {
    this.loading = true;
    this.error   = '';
    this.adminService.getAuditLog(page, this.pageSize).subscribe({
      next: (r) => {
        this.applyPage(r);
        this.loading = false;
      },
      error: () => {
        this.error   = 'Impossible de charger le journal d\'audit.';
        this.loading = false;
      },
    });
  }

  private applyPage(r: AuditPageResponse): void {
    this.logs          = r.content;
    this.currentPage   = r.number;
    this.totalPages    = r.totalPages;
    this.totalElements = r.totalElements;
  }
}
