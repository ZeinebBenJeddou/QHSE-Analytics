import {
  AfterViewInit, Component, ElementRef, Input,
  OnChanges, OnDestroy, SimpleChanges, ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { RadarPoint } from '../../../core/models/dashboard.model';

Chart.register(...registerables);

@Component({
  selector: 'app-radar-performance',
  standalone: true,
  imports: [CommonModule],
  template: `<div class="chart-wrapper"><canvas #chartCanvas></canvas></div>`,
  styles: [`
    :host { display: block; width: 100%; height: 100%; }
    .chart-wrapper { position: relative; width: 100%; height: 100%; min-height: 260px; }
    canvas { width: 100% !important; height: 100% !important; }
  `]
})
export class RadarPerformanceComponent implements AfterViewInit, OnChanges, OnDestroy {
  /** Array of RadarPoint for current period N */
  @Input() data: RadarPoint[] = [];
  /** Optional array of RadarPoint for N-1 (if available from backend) */
  @Input() dataN1: RadarPoint[] = [];

  @ViewChild('chartCanvas', { static: true }) canvas?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart<'radar'>;

  ngAfterViewInit(): void { this.buildChart(); }
  ngOnChanges(c: SimpleChanges): void {
    if (c['data'] || c['dataN1']) this.buildChart();
  }
  ngOnDestroy(): void { this.chart?.destroy(); }

  private buildChart(): void {
    this.chart?.destroy();
    if (!this.canvas || !this.data?.length) return;
    const ctx = this.canvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const NAVY   = '#2b3674';
    const MUTED  = '#a3aed1';
    const BORDER = '#e2e8f0';
    const GRID   = 'rgba(226,234,246,0.7)';
    const FONT   = "'DM Sans', system-ui, sans-serif";
    const MONO   = "'DM Mono', monospace";

    const labels = this.data.map(d => d.categorie);
    const hasN1  = this.dataN1?.length === this.data.length;

    const datasets: any[] = [];

    if (hasN1) {
      // N-1 dataset (background reference)
      datasets.push({
        label: 'N-1',
        data: this.dataN1.map(d => d.score),
        backgroundColor: 'rgba(30,111,217,0.08)',
        borderColor: 'rgba(30,111,217,0.5)',
        borderWidth: 1.5,
        borderDash: [5, 4],
        pointBackgroundColor: 'rgba(30,111,217,0.5)',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
        pointRadius: 3,
        pointHoverRadius: 5,
      });
    }

    // N dataset (primary)
    datasets.push({
      label: 'N',
      data: this.data.map(d => d.score),
      backgroundColor: 'rgba(107,70,193,0.15)',
      borderColor: '#6B46C1',
      borderWidth: 2.5,
      pointBackgroundColor: '#6B46C1',
      pointBorderColor: '#fff',
      pointBorderWidth: 2.5,
      pointRadius: 5,
      pointHoverRadius: 7,
      pointHoverBackgroundColor: '#6B46C1',
      pointHoverBorderColor: '#fff',
    });

    const config: ChartConfiguration<'radar'> = {
      type: 'radar',
      data: { labels, datasets },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 800, easing: 'easeOutQuart' },
        plugins: {
          legend: {
            display: hasN1,
            labels: {
              color: MUTED,
              font: { family: FONT, size: 11, weight: 600 },
              boxWidth: 10,
              boxHeight: 10,
              borderRadius: 3,
              useBorderRadius: true,
              padding: 14,
            },
          },
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
              label: (item) => `  Score: ${item.parsed.r.toFixed(1)}%`,
            },
          },
        },
        scales: {
          r: {
            min: 0,
            max: 100,
            ticks: {
              color: MUTED,
              font: { family: MONO, size: 9 },
              backdropColor: 'transparent',
              stepSize: 25,
            },
            grid: { color: GRID, lineWidth: 1 },
            angleLines: { color: BORDER, lineWidth: 1 },
            pointLabels: {
              color: NAVY,
              font: { family: FONT, size: 12, weight: 600 },
            },
          },
        },
      },
    };

    this.chart = new Chart(ctx, config);
  }
}