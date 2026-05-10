import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly duration = 5000;

  constructor(private readonly snackBar: MatSnackBar) {}

  error(message: string): void {
    this.open(message, 'snack-error', 7000);
  }

  warning(message: string): void {
    this.open(message, 'snack-warning', this.duration);
  }

  success(message: string): void {
    this.open(message, 'snack-success', this.duration);
  }

  info(message: string): void {
    this.open(message, 'snack-info', this.duration);
  }

  private open(message: string, panelClass: string, duration: number): void {
    const config: MatSnackBarConfig = {
      duration,
      panelClass: [panelClass],
      horizontalPosition: 'right',
      verticalPosition: 'top',
    };
    this.snackBar.open(message, '✕', config);
  }
}
