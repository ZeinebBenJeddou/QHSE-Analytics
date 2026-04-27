import {
  Component, Input, OnChanges, SimpleChanges,
  ElementRef, ViewChild, AfterViewInit, OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { BarreGroupeeData } from '../../../core/models/dashboard.model';

Chart.register(...registerables);

@Component({
  selector: 'app-bar-comparison',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="chart-wrap">
      <canvas #chartCanvas></canvas>
    </div>
  `,
  styles: [`
    .chart-wrap { position: relative; height: 300px; }
    canvas { width: 100% !important; }
  `],
})
export class BarComparisonComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input() data: BarreGroupeeData[] = [];
  @Input() labelN1 = 'N-1';
  @Input() labelN = 'N';
  @ViewChild('chartCanvas') canvas!: ElementRef<HTMLCanvasElement>;

  private chart?: Chart;

  ngAfterViewInit() { this.buildChart(); }
  ngOnChanges(c: SimpleChanges) { if (c['data'] && this.canvas) this.buildChart(); }
  ngOnDestroy() { this.chart?.destroy(); }

  private buildChart() {
    this.chart?.destroy();
    if (!this.canvas || !this.data?.length) return;
    const ctx = this.canvas.nativeElement.getContext('2d')!;
    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: this.data.map(d => d.categorie),
        datasets: [
          {
            label: this.labelN1,
            data: this.data.map(d => d.valeurMoyenneN1),
            backgroundColor: 'rgba(99,102,241,0.6)',
            borderColor: '#6366f1',
            borderWidth: 1,
            borderRadius: 6,
          },
          {
            label: this.labelN,
            data: this.data.map(d => d.valeurMoyenneN),
            backgroundColor: 'rgba(34,197,94,0.6)',
            borderColor: '#22c55e',
            borderWidth: 1,
            borderRadius: 6,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { labels: { color: '#94a3b8' } },
          tooltip: {
            callbacks: {
              afterLabel: (ctx) => {
                const d = this.data[ctx.dataIndex];
                return `Δ: ${d.variationMoyenne > 0 ? '+' : ''}${d.variationMoyenne.toFixed(1)}%`;
              }
            }
          }
        },
        scales: {
          x: { ticks: { color: '#64748b' }, grid: { color: 'rgba(255,255,255,0.04)' } },
          y: { ticks: { color: '#64748b' }, grid: { color: 'rgba(255,255,255,0.06)' } },
        },
      },
    };
    this.chart = new Chart(ctx, config);
  }
}
