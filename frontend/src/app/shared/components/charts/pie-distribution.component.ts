import {
  AfterViewInit, Component, ElementRef, Input,
  OnChanges, OnDestroy, SimpleChanges, ViewChild, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, Plugin, registerables } from 'chart.js';
import { MatIconModule } from '@angular/material/icon';

Chart.register(...registerables);

@Component({
  selector: 'app-pie-distribution',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="chart-wrapper">
      @if (isEmpty()) {
        <div class="empty-chart">
          <mat-icon>info_outline</mat-icon>
          <span>Aucune donnée classifiée disponible</span>
        </div>
      } @else {
        <canvas #chartCanvas></canvas>
      }
    </div>
  `,
  styles: [`
    :host { display: block; width: 100%; height: 100%; }
    .chart-wrapper { position: relative; width: 100%; height: 100%; min-height: 180px; display: flex; align-items: center; justify-content: center; }
    canvas { width: 100% !important; height: 100% !important; }
    .empty-chart { display: flex; flex-direction: column; align-items: center; gap: 8px; color: #94A3B8; font-size: 0.8rem; text-align: center; padding: 16px; }
    .empty-chart mat-icon { font-size: 32px; width: 32px; height: 32px; color: #CBD5E1; }
  `]
})
export class PieDistributionComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() enHausseCritique = 0;
  @Input() enHausseModeree  = 0;
  @Input() enBaisseModeree  = 0;
  @Input() enBaisseFaible   = 0;

  @ViewChild('chartCanvas') canvas?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart<'doughnut'>;

  isEmpty = signal(false);

  ngAfterViewInit(): void { this.buildChart(); }
  ngOnChanges(c: SimpleChanges): void {
    if (c['enHausseCritique'] || c['enHausseModeree'] || c['enBaisseModeree'] || c['enBaisseFaible']) {
      this.buildChart();
    }
  }
  ngOnDestroy(): void { this.chart?.destroy(); }

  private get total(): number {
    return this.enHausseCritique + this.enHausseModeree + this.enBaisseModeree + this.enBaisseFaible;
  }

  private buildChart(): void {
    this.chart?.destroy();
    if (this.total === 0) {
      this.isEmpty.set(true);
      return;
    }
    this.isEmpty.set(false);
    setTimeout(() => {
      if (!this.canvas) return;
      const ctx = this.canvas.nativeElement.getContext('2d');
      if (!ctx) return;
      this.renderChart(ctx);
    });
  }

  private renderChart(ctx: CanvasRenderingContext2D): void {

    const BORDER = '#E2E8F0';
    const MUTED  = '#94A3B8';
    const NAVY   = '#0F172A';
    const FONT   = "'Inter', system-ui, sans-serif";
    const MONO   = "'DM Mono', monospace";

    const totalVal = this.total;

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      plugins: [],
      data: {
        labels: ['Hausse critique', 'Hausse modérée', 'Baisse modérée', 'Baisse faible'],
        datasets: [{
          data: [this.enHausseCritique, this.enHausseModeree, this.enBaisseModeree, this.enBaisseFaible],
          backgroundColor: ['#1E40AF', '#6366F1', '#10B981', '#F59E0B'],
          borderColor: '#fff',
          borderWidth: 3,
          hoverOffset: 10,
          hoverBorderWidth: 4,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '70%',
        animation: { animateRotate: true, duration: 800, easing: 'easeOutCubic' },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#fff',
            borderColor: BORDER,
            borderWidth: 1.5,
            titleColor: NAVY,
            bodyColor: '#4a5568',
            titleFont: { family: MONO, size: 11, weight: 700 },
            bodyFont: { family: FONT, size: 12 },
            padding: 12,
            cornerRadius: 10,
            callbacks: {
              label: (item) => {
                const val = item.parsed;
                const pct = totalVal > 0 ? ((val / totalVal) * 100).toFixed(1) : '0.0';
                return `  ${item.label}: ${val} (${pct}%)`;
              },
            },
          },
        },
      },
    };

    this.chart = new Chart(ctx, config);
  }
}