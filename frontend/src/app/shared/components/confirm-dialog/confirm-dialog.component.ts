import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="confirm-dialog">
      <div class="confirm-header" [class.danger]="data.danger">
        <mat-icon>{{ data.danger ? 'warning' : 'help_outline' }}</mat-icon>
        <h2 mat-dialog-title>{{ data.title }}</h2>
      </div>
      <mat-dialog-content>
        <p>{{ data.message }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-stroked-button (click)="cancel()">
          {{ data.cancelLabel ?? 'Annuler' }}
        </button>
        <button mat-flat-button [class.btn-danger]="data.danger" (click)="confirm()">
          {{ data.confirmLabel ?? 'Confirmer' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .confirm-dialog { padding: 8px; min-width: 320px; max-width: 420px; }
    .confirm-header { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; color: #374151; }
    .confirm-header.danger { color: #dc2626; }
    .confirm-header mat-icon { font-size: 24px; width: 24px; height: 24px; }
    h2[mat-dialog-title] { margin: 0; font-size: 1.05rem; font-weight: 600; }
    mat-dialog-content p { color: #6b7280; font-size: 0.92rem; margin: 0; }
    mat-dialog-actions { gap: 8px; padding: 16px 0 4px; }
    .btn-danger { background-color: #dc2626 !important; color: #fff !important; }
  `],
})
export class ConfirmDialogComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData,
  ) {}

  confirm(): void { this.dialogRef.close(true); }
  cancel(): void  { this.dialogRef.close(false); }
}
