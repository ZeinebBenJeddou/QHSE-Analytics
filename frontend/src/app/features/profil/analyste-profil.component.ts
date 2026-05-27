import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AnalysteProfilService } from '../../core/services/analyste-profil.service';
import { AnalysteProfil } from '../../core/models/analyste-profil.model';

@Component({
  selector: 'app-analyste-profil',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  templateUrl: './analyste-profil.component.html',
})
export class AnalysteProfilComponent implements OnInit {
  private profilService = inject(AnalysteProfilService);
  private snackBar = inject(MatSnackBar);

  isSaving = signal(false);
  isLoading = signal(true);

  profil: AnalysteProfil = {};

  readonly tailleSiteOptions = ['< 50', '50-200', '200-500', '500+'];

  ngOnInit() {
    this.profilService.getProfil().subscribe({
      next: (data) => {
        this.profil = { ...data };
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  save() {
    this.isSaving.set(true);
    this.profilService.saveProfil(this.profil).subscribe({
      next: (saved) => {
        this.profil = { ...saved };
        this.isSaving.set(false);
        this.snackBar.open('Profil enregistré avec succès.', 'OK', { duration: 3000 });
      },
      error: () => {
        this.isSaving.set(false);
        this.snackBar.open('Erreur lors de l\'enregistrement du profil.', 'OK', { duration: 4000 });
      },
    });
  }
}
