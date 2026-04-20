import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private readonly snackBar: MatSnackBar) {}

  success(message: string): void {
    this.snackBar.open(message, 'OK', {
      duration: 3500,
      panelClass: ['snack-success']
    });
  }

  error(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 4500,
      panelClass: ['snack-error']
    });
  }

  info(message: string): void {
    this.snackBar.open(message, 'OK', {
      duration: 3000,
      panelClass: ['snack-info']
    });
  }
}