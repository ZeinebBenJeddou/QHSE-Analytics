import {
  AfterViewInit, Component, ElementRef, Input,
  OnChanges, OnDestroy, SimpleChanges, ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, Plugin, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-pie-distribution',
  standalone: true,
  imports: [CommonModule],
  template: `<div class="chart-wrapper"><canvas #chartCanvas></canvas></div>`,
  styles: [`
    :host { display: block; width: 100%; height: 100%; }
    .chart-wrapper { position: relative; width: 100%; height: 100%; min-height: 180px; }
    canvas { width: 100% !important; height: 100% !important; }
  `]
})
export class PieDistributionComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() enHausseCritique = 0;
  @Input() enHausseModeree  = 0;
  @Input() enBaisseModeree  = 0;
  @Input() enBaisseFaible   = 0;

  @ViewChild('chartCanvas', { static: true }) canvas?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart<'doughnut'>;

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
    if (!this.canvas) return;
    const ctx = this.canvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const BORDER = '#E2E8F0';
    const MUTED  = '#94A3B8';
    const NAVY   = '#0F172A';
    const FONT   = "'Inter', system-ui, sans-serif";
    const MONO   = "'DM Mono', monospace";

    const totalVal = this.total;

    // Center text plugin
    const centerTextPlugin: Plugin<'doughnut'> = {
      id: 'centerText',
      beforeDraw(chart) {
        const { ctx, chartArea: { top, bottom, left, right } } = chart;
        const cx = (left + right) / 2;
        const cy = (top + bottom) / 2;
        ctx.save();
        ctx.font = `800 22px ${MONO}`;
        ctx.fillStyle = NAVY;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(String(totalVal), cx, cy - 8);
        ctx.font = `500 11px ${FONT}`;
        ctx.fillStyle = MUTED;
        ctx.fillText('KPI', cx, cy + 12);
        ctx.restore();
      }
    };

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      plugins: [centerTextPlugin],
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