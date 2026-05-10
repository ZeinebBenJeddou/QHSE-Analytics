import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminService } from '../../../../core/services/admin.service';
import { AiConfigResponse } from '../../models/admin.models';

@Component({
  selector: 'app-ia-config',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule,
  ],
  templateUrl: './ia-config.component.html',
  styleUrls: ['./ia-config.component.css'],
})
export class IaConfigComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly snackBar     = inject(MatSnackBar);
  private readonly route        = inject(ActivatedRoute);

  configs: AiConfigResponse[] = [];
  loading  = false;
  saving   = false;
  error    = '';

  editingKey: string | null = null;
  editValue  = '';

  ngOnInit(): void {
    const resolved = this.route.snapshot.data['configs'] as AiConfigResponse[] | null;
    if (resolved?.length) {
      this.configs = resolved;
    } else {
      this.loadConfigs();
    }
  }

  get groqConfigs(): AiConfigResponse[] {
    return this.configs.filter(c => c.key.startsWith('groq.'));
  }

  get ragConfigs(): AiConfigResponse[] {
    return this.configs.filter(c => c.key.startsWith('rag.'));
  }

  startEdit(cfg: AiConfigResponse): void {
    this.editingKey = cfg.key;
    this.editValue  = cfg.value;
  }

  cancelEdit(): void {
    this.editingKey = null;
    this.editValue  = '';
  }

  saveEdit(): void {
    if (!this.editingKey) return;
    const val = this.editValue.trim();
    if (!val || isNaN(Number(val))) {
      this.snackBar.open('Valeur numérique requise.', 'Fermer', { duration: 3000 });
      return;
    }
    this.saving = true;
    this.adminService.updateAiConfig(this.editingKey, { value: val }).subscribe({
      next: (updated) => {
        const idx = this.configs.findIndex(c => c.key === updated.key);
        if (idx !== -1) this.configs[idx] = updated;
        this.snackBar.open('Configuration mise à jour.', 'Fermer', { duration: 3000 });
        this.cancelEdit();
      },
      error: () => this.snackBar.open('Erreur lors de la mise à jour.', 'Fermer', { duration: 4000 }),
      complete: () => { this.saving = false; },
    });
  }

  formatKey(key: string): string {
    const labels: Record<string, string> = {
      'groq.temperature.json':  'Température JSON',
      'groq.temperature.text':  'Température texte',
      'groq.timeout.seconds':   'Timeout (secondes)',
      'rag.search.threshold':   'Seuil similarité',
      'rag.search.top.k':       'Résultats max (top-k)',
    };
    return labels[key] ?? key;
  }

  private loadConfigs(): void {
    this.loading = true;
    this.adminService.getAiConfigs().subscribe({
      next:     (r) => { this.configs = r; },
      error:    () => { this.error = 'Impossible de charger la configuration.'; },
      complete: () => { this.loading = false; },
    });
  }
}
