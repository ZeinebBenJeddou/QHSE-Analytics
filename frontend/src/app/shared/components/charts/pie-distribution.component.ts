import {
  Component, Input, OnChanges, SimpleChanges,
  ElementRef, ViewChild, AfterViewInit, OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-pie-distribution',
  standalone: true,
  imports: [CommonModule],
  template: `<div class="chart-wrap"><canvas #chartCanvas></canvas></div>`,
  styles: [`.chart-wrap { position: relative; height: 280px; } canvas { width: 100% !important; }`],
})
export class PieDistributionComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input() critiques = 0;
  @Input() moderes = 0;
  @Input() faibles = 0;
  @ViewChild('chartCanvas') canvas!: ElementRef<HTMLCanvasElement>;
  private chart?: Chart;

  ngAfterViewInit() { this.buildChart(); }
  ngOnChanges(c: SimpleChanges) { if (this.canvas) this.buildChart(); }
  ngOnDestroy() { this.chart?.destroy(); }

  private buildChart() {
    this.chart?.destroy();
    if (!this.canvas) return;
    const ctx = this.canvas.nativeElement.getContext('2d')!;
    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: ['Critiques', 'Modérés', 'Faibles'],
        datasets: [{
          data: [this.critiques, this.moderes, this.faibles],
          backgroundColor: [
            'rgba(239,68,68,0.75)',
            'rgba(234,179,8,0.75)',
            'rgba(34,197,94,0.75)',
          ],
          borderColor: ['#ef4444', '#eab308', '#22c55e'],
          borderWidth: 2,
          hoverOffset: 8,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: '#94a3b8', padding: 16, font: { size: 12 } },
          },
        },
      },
    };
    this.chart = new Chart(ctx, config);
  }
}
