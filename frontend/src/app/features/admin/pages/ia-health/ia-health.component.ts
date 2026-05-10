import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { interval, Subscription } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import { IaHealthResponse, ProviderStatusResponse } from '../../models/admin.models';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-ia-health',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule, MatDialogModule,
  ],
  templateUrl: './ia-health.component.html',
  styleUrls: ['./ia-health.component.css'],
})
export class IaHealthComponent implements OnInit, OnDestroy {
  private readonly adminService = inject(AdminService);
  private readonly snackBar     = inject(MatSnackBar);
  private readonly dialog       = inject(MatDialog);
  private readonly route        = inject(ActivatedRoute);

  health: IaHealthResponse | null = null;
  loading  = false;
  error    = '';
  clearingCache = false;

  private refreshSub?: Subscription;
  lastRefreshed: Date | null = null;

  ngOnInit(): void {
    const resolved = this.route.snapshot.data['health'] as IaHealthResponse | null;
    if (resolved) {
      this.health = resolved;
      this.lastRefreshed = new Date();
    } else {
      this.refresh();
    }
    // auto-refresh toutes les 60 secondes
    this.refreshSub = interval(60_000).subscribe(() => this.refresh());
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  refresh(): void {
    this.loading = true;
    this.error = '';
    this.adminService.getIaHealth().subscribe({
      next: (h) => { this.health = h; this.lastRefreshed = new Date(); },
      error: () => { this.error = 'Impossible de charger les métriques IA.'; },
      complete: () => { this.loading = false; },
    });
  }

  clearCache(): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Vider le cache LLM',
        message: 'Toutes les analyses en cache seront supprimées. Les prochaines requêtes recalculeront via le LLM (Groq/Gemini).',
        confirmLabel: 'Vider le cache',
        danger: true,
      },
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.clearingCache = true;
      this.adminService.clearIaCache().subscribe({
        next: () => { this.snackBar.open('Cache LLM vidé.', 'Fermer', { duration: 3000 }); },
        error: () => { this.snackBar.open('Erreur lors du vidage du cache.', 'Fermer', { duration: 3000 }); },
        complete: () => { this.clearingCache = false; },
      });
    });
  }

  clearCooldown(provider: ProviderStatusResponse): void {
    this.adminService.clearProviderCooldown(provider.name).subscribe({
      next: () => {
        this.snackBar.open(`Cooldown "${provider.name}" réinitialisé.`, 'Fermer', { duration: 3000 });
        this.refresh();
      },
      error: () => this.snackBar.open('Erreur lors de la réinitialisation.', 'Fermer', { duration: 3000 }),
    });
  }

  // ── Computed helpers ─────────────────────────────────────────────────────

  get successRate(): number {
    if (!this.health) return 0;
    const m = this.health.metrics;
    const total = m.successCount + m.failedCount;
    return total > 0 ? Math.round((m.successCount / total) * 100) : 0;
  }

  get cacheHitRate(): number {
    if (!this.health) return 0;
    const m = this.health.metrics;
    const total = m.cacheHits + m.cacheMisses;
    return total > 0 ? Math.round((m.cacheHits / total) * 100) : 0;
  }

  get totalRequests(): number {
    if (!this.health) return 0;
    return this.health.metrics.successCount + this.health.metrics.failedCount;
  }

  providerLabel(name: string): string {
    const map: Record<string, string> = {
      'groq':             'Groq',
      'gemini':           'Gemini LLM',
      'gemini-embedding': 'Gemini Embedding',
    };
    return map[name] ?? name;
  }

  providerIcon(name: string): string {
    if (name === 'groq')             return 'bolt';
    if (name === 'gemini')           return 'auto_awesome';
    if (name === 'gemini-embedding') return 'hub';
    return 'cloud';
  }

  cooldownRemaining(provider: ProviderStatusResponse): string {
    if (!provider.cooldownUntil) return '';
    const diff = new Date(provider.cooldownUntil).getTime() - Date.now();
    if (diff <= 0) return '';
    const secs = Math.ceil(diff / 1000);
    return secs < 60 ? `${secs}s` : `${Math.ceil(secs / 60)}min`;
  }

  formatLatency(ms: number): string {
    return ms > 0 ? ms.toFixed(0) + ' ms' : '—';
  }

  fmt(n: number): string {
    return n > 0 ? n.toFixed(0) : '0';
  }
}
