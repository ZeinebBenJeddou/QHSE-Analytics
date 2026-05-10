import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminService } from '../../../../core/services/admin.service';
import { DataRetentionPolicyResponse } from '../../models/admin.models';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-data-retention',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatDialogModule, MatTooltipModule,
  ],
  templateUrl: './data-retention.component.html',
  styleUrls: ['./data-retention.component.css'],
})
export class DataRetentionComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly snackBar     = inject(MatSnackBar);
  private readonly dialog       = inject(MatDialog);
  private readonly route        = inject(ActivatedRoute);

  policy: DataRetentionPolicyResponse | null = null;
  loading  = false;
  purging  = false;
  error    = '';

  ngOnInit(): void {
    const resolved = this.route.snapshot.data['policy'] as DataRetentionPolicyResponse | null;
    if (resolved) {
      this.policy = resolved;
    } else {
      this.loadPolicy();
    }
  }

  get totalRowsDeleted(): number {
    if (!this.policy) return 0;
    return (this.policy.lastRawRowsDeleted ?? 0) + (this.policy.lastPreviewRowsDeleted ?? 0);
  }

  get durationSeconds(): string {
    if (!this.policy?.lastDurationMs) return '—';
    return (this.policy.lastDurationMs / 1000).toFixed(1) + 's';
  }

  triggerPurge(): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Déclencher la purge',
        message: 'Supprimer définitivement les données raw et preview des imports terminés au-delà de la période de rétention ?',
        confirmLabel: 'Purger',
        danger: true,
      },
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.purging = true;
      this.adminService.triggerPurge().subscribe({
        next: (result) => {
          this.policy = result;
          this.snackBar.open('Purge terminée avec succès.', 'Fermer', { duration: 4000 });
        },
        error: () => this.snackBar.open('Erreur lors de la purge.', 'Fermer', { duration: 4000 }),
        complete: () => { this.purging = false; },
      });
    });
  }

  refresh(): void {
    this.loadPolicy();
  }

  private loadPolicy(): void {
    this.loading = true;
    this.adminService.getDataRetention().subscribe({
      next:     (r) => { this.policy = r; },
      error:    () => { this.error = 'Impossible de charger la politique de rétention.'; },
      complete: () => { this.loading = false; },
    });
  }
}
