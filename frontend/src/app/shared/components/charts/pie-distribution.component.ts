import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-pie-distribution',
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
export class PieDistributionComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() critiques = 0;
  @Input() moderes = 0;
  @Input() faibles = 0;

  @ViewChild('chartCanvas', { static: true }) canvas?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart<'doughnut'>;

  ngAfterViewInit(): void {
    this.buildChart();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['critiques'] || changes['moderes'] || changes['faibles']) {
      this.buildChart();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private buildChart(): void {
    this.chart?.destroy();
    if (!this.canvas) {
      return;
    }

    const ctx = this.canvas.nativeElement.getContext('2d');
    if (!ctx) {
      return;
    }

    const BORDER = '#E2EAF6';
    const INK_MID = '#4A5568';
    const FONT = "'DM Sans', system-ui, sans-serif";
    const FONT_MONO = "'DM Mono', monospace";

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: ['Critiques', 'Modérés', 'Faibles'],
        datasets: [{
          data: [this.critiques, this.moderes, this.faibles],
          backgroundColor: [
            'rgba(229,62,62,0.65)',
            'rgba(221,107,32,0.65)',
            'rgba(56,161,105,0.65)',
          ],
          borderColor: ['#fff', '#fff', '#fff'],
          borderWidth: 3,
          hoverOffset: 8,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '68%',
        plugins: {
          legend: {
            position: 'bottom',
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
      },
    };

    this.chart = new Chart(ctx, config);
  }
}
