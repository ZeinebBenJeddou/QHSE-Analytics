import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { BarreGroupeeData } from '../../../core/models/dashboard.model';

Chart.register(...registerables);

@Component({
  selector: 'app-bar-comparison',
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
export class BarComparisonComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() data: BarreGroupeeData[] = [];
  @Input() labelN1 = 'N-1';
  @Input() labelN = 'N';

  @ViewChild('chartCanvas', { static: true }) canvas?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart<'bar'>;

  ngAfterViewInit(): void {
    this.buildChart();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data'] || changes['labelN1'] || changes['labelN']) {
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
    const PURPLE = '#6B46C1';
    const PURPLE_SOFT = 'rgba(107,70,193,0.14)';
    const INK_MID = '#4A5568';
    const BORDER = '#E2EAF6';
    const GRID = 'rgba(226,234,246,0.8)';
    const TICK = '#718096';
    const FONT = "'DM Sans', system-ui, sans-serif";
    const FONT_MONO = "'DM Mono', monospace";

    const tooltipDefaults = {
      backgroundColor: '#fff',
      borderColor: BORDER,
      borderWidth: 1.5,
      titleColor: '#0D1B3E',
      bodyColor: INK_MID,
      titleFont: { family: FONT_MONO, size: 11 },
      bodyFont: { family: FONT, size: 12 },
      padding: 10,
      cornerRadius: 8,
    };

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: this.data.map(d => d.categorie),
        datasets: [
          {
            label: this.labelN1,
            data: this.data.map(d => d.valeurMoyenneN1),
            backgroundColor: BLUE_SOFT,
            borderColor: BLUE,
            borderWidth: 1.5,
            borderRadius: 6,
            borderSkipped: false,
          },
          {
            label: this.labelN,
            data: this.data.map(d => d.valeurMoyenneN),
            backgroundColor: PURPLE_SOFT,
            borderColor: PURPLE,
            borderWidth: 1.5,
            borderRadius: 6,
            borderSkipped: false,
          },
        ],
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
              padding: 14,
            },
          },
          tooltip: {
            ...tooltipDefaults,
            callbacks: {
              afterLabel: (ctx: any) => {
                const d = this.data[ctx.dataIndex];
                if (!d) {
                  return '';
                }
                return `Δ: ${d.variationMoyenne > 0 ? '+' : ''}${d.variationMoyenne.toFixed(1)}%`;
              },
            },
          },
        },
        scales: {
          x: {
            ticks: { color: TICK, font: { family: FONT, size: 11 } },
            grid: { color: GRID, lineWidth: 1 },
            border: { color: BORDER },
          },
          y: {
            ticks: { color: TICK, font: { family: FONT_MONO, size: 10 } },
            grid: { color: GRID, lineWidth: 1 },
            border: { color: BORDER, dash: [4, 3] },
          },
        },
      },
    };

    this.chart = new Chart(ctx, config);
  }
}
