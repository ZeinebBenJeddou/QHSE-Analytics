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
    MatDividerModule, MatSnackBarModule, MatChipsModule,
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
    this.dashboardService.getAnalysesIa(importId).subscribe({
      next: res => { this.analyse.set(res); this.loading.set(false); },
      error: () => {
        this.error.set("Impossible de charger l'analyse IA.");
        this.loading.set(false);
      }
    });
  }

  regenerer() {
    const id = this.importId();
    if (!id) return;
    this.regenerating.set(true);
    this.aiService.regenerer(id).subscribe({
      next: res => {
        this.analyse.set(res);
        this.regenerating.set(false);
        this.snackBar.open('Analyses régénérées avec succès !', 'OK', { duration: 3000 });
      },
      error: () => {
        this.regenerating.set(false);
        this.snackBar.open("Erreur lors de la régénération.", 'OK', { duration: 3000 });
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
