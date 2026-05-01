import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DashboardService } from '../../../../core/services/dashboard.service';
import { AiAnalysisService } from '../../../../core/services/ai-analysis.service';
import { AnalyseCompleteResponse } from '../../../../core/models/analyse-ia.model';

@Component({
  selector: 'app-ia-insights',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatExpansionModule, MatProgressSpinnerModule,
    MatDividerModule, MatSnackBarModule, MatChipsModule, MatTooltipModule,
  ],
  templateUrl: './ia-insights.component.html',
  styleUrls: ['./ia-insights.component.css'],
})
export class IaInsightsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dashboardService = inject(DashboardService);
  private aiService = inject(AiAnalysisService);
  private snackBar = inject(MatSnackBar);

  importId = signal<number | null>(null);
  analyse = signal<AnalyseCompleteResponse | null>(null);
  loading = signal(true);
  regenerating = signal(false);
  polling = signal(false);
  error = signal('');

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.importId.set(+id);
      this.load(+id);
    } else {
      this.error.set('Aucun import sélectionné.');
      this.loading.set(false);
    }
  }

  load(importId: number) {
    this.loading.set(true);
    this.error.set('');
    this.dashboardService.getAnalysesIa(importId).subscribe({
      next: res => {
        this.analyse.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger la synthèse et le résumé KPI.");
        this.loading.set(false);
      }
    });
  }

  runAi() {
    const id = this.importId();
    if (!id) {
      this.snackBar.open('Aucun import sélectionné pour l’analyse IA.', 'OK', { duration: 3000 });
      return;
    }
    this.regenerating.set(true);
    this.loading.set(true);
    this.error.set('');
    this.polling.set(true);

    this.aiService.runAi(id).subscribe({
      next: () => {
        this.snackBar.open('Analyse IA lancée, récupération des résultats en arrière-plan.', 'OK', { duration: 3000 });
        this.pollAnalyse(id, 0);
      },
      error: () => {
        this.regenerating.set(false);
        this.loading.set(false);
        this.polling.set(false);
        this.snackBar.open('Erreur lors de l’analyse IA.', 'OK', { duration: 3000 });
      }
    });
  }

  private pollAnalyse(importId: number, attempt: number) {
    if (attempt >= 12) {
      this.loading.set(false);
      this.regenerating.set(false);
      this.polling.set(false);
      this.error.set('Impossible de récupérer l’analyse IA. Réessayez dans quelques instants.');
      return;
    }

    this.dashboardService.getAnalysesIa(importId).subscribe({
      next: res => {
        this.analyse.set(res);
        this.loading.set(false);
        this.regenerating.set(false);
        this.polling.set(false);
      },
      error: () => {
        window.setTimeout(() => this.pollAnalyse(importId, attempt + 1), 2500);
      }
    });
  }

  niveauClass(n: string): string {
    return { CRITIQUE: 'chip-critique', MODERE: 'chip-modere', FAIBLE: 'chip-faible' }[n] ?? '';
  }

  tendanceIcon(t: string): string {
    return { HAUSSE: 'trending_up', BAISSE: 'trending_down', STABLE: 'trending_flat' }[t] ?? 'remove';
  }

  goBack() {
    const id = this.importId();
    this.router.navigate(['/analyste/dashboard', id]);
  }
}
