import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { RadarPoint } from '../../../core/models/dashboard.model';

Chart.register(...registerables);

@Component({
  selector: 'app-radar-performance',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="chart-wrapper">
      <canvas #chartCanvas></canvas>
    </div>
  `,
  styles: [
    `:host { display: block; width: 100%; min-height: 320px; }
     .chart-wrapper { position: relative; width: 100%; min-height: 320px; }
     canvas { width: 100% !important; height: 100% !important; }
    `
  ]
})
export class RadarPerformanceComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() data: RadarPoint[] = [];

  @ViewChild('chartCanvas', { static: true }) canvas?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart<'radar'>;

  ngAfterViewInit(): void {
    this.buildChart();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data']) {
      this.buildChart();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private buildChart(): void {
    this.chart?.destroy();
    if (!this.canvas || !this.data?.length) {
      return;
    }

    const ctx = this.canvas.nativeElement.getContext('2d');
    if (!ctx) {
      return;
    }

    const BLUE = '#1E6FD9';
    const BLUE_SOFT = 'rgba(30,111,217,0.12)';
    const BORDER = '#E2EAF6';
    const INK_MID = '#4A5568';
    const GRID = 'rgba(226,234,246,0.8)';
    const TICK = '#718096';
    const FONT = "'DM Sans', system-ui, sans-serif";
    const FONT_MONO = "'DM Mono', monospace";

    const config: ChartConfiguration<'radar'> = {
      type: 'radar',
      data: {
        labels: this.data.map(d => d.categorie),
        datasets: [{
          label: 'Score performance (%)',
          data: this.data.map(d => d.score),
          backgroundColor: BLUE_SOFT,
          borderColor: BLUE,
          borderWidth: 2,
          pointBackgroundColor: BLUE,
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            labels: {
              color: INK_MID,
              font: { family: FONT, size: 11, weight: 500 },
              boxWidth: 10,
              boxHeight: 10,
              borderRadius: 3,
              useBorderRadius: true,
              padding: 16,
            },
          },
          tooltip: {
            backgroundColor: '#fff',
            borderColor: BORDER,
            borderWidth: 1.5,
            titleColor: '#0D1B3E',
            bodyColor: INK_MID,
            titleFont: { family: FONT_MONO, size: 11 },
            bodyFont: { family: FONT, size: 12 },
            padding: 10,
            cornerRadius: 8,
          },
        },
        scales: {
          r: {
            min: 0,
            max: 100,
            ticks: {
              color: TICK,
              font: { family: FONT_MONO, size: 9 },
              backdropColor: 'transparent',
              stepSize: 25,
            },
            grid: { color: GRID, lineWidth: 1 },
            angleLines: { color: BORDER, lineWidth: 1 },
            pointLabels: {
              color: INK_MID,
              font: { family: FONT, size: 11, weight: 500 },
            },
          },
        },
      },
    };

    this.chart = new Chart(ctx, config);
  }
}
