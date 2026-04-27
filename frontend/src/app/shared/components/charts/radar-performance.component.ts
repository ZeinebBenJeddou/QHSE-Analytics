import {
  Component, Input, OnChanges, SimpleChanges,
  ElementRef, ViewChild, AfterViewInit, OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { RadarPoint } from '../../../core/models/dashboard.model';

Chart.register(...registerables);

@Component({
  selector: 'app-radar-performance',
  standalone: true,
  imports: [CommonModule],
  template: `<div class="chart-wrap"><canvas #chartCanvas></canvas></div>`,
  styles: [`.chart-wrap { position: relative; height: 300px; } canvas { width: 100% !important; }`],
})
export class RadarPerformanceComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input() data: RadarPoint[] = [];
  @ViewChild('chartCanvas') canvas!: ElementRef<HTMLCanvasElement>;
  private chart?: Chart;

  ngAfterViewInit() { this.buildChart(); }
  ngOnChanges(c: SimpleChanges) { if (c['data'] && this.canvas) this.buildChart(); }
  ngOnDestroy() { this.chart?.destroy(); }

  private buildChart() {
    this.chart?.destroy();
    if (!this.canvas || !this.data?.length) return;
    const ctx = this.canvas.nativeElement.getContext('2d')!;
    const config: ChartConfiguration<'radar'> = {
      type: 'radar',
      data: {
        labels: this.data.map(d => d.categorie),
        datasets: [{
          label: 'Score performance (%)',
          data: this.data.map(d => d.score),
          backgroundColor: 'rgba(99,102,241,0.2)',
          borderColor: '#6366f1',
          pointBackgroundColor: '#6366f1',
          pointBorderColor: '#fff',
          pointRadius: 4,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { labels: { color: '#94a3b8' } } },
        scales: {
          r: {
            min: 0, max: 100,
            ticks: { color: '#475569', backdropColor: 'transparent', stepSize: 25 },
            grid: { color: 'rgba(255,255,255,0.06)' },
            pointLabels: { color: '#94a3b8', font: { size: 11 } },
            angleLines: { color: 'rgba(255,255,255,0.06)' },
          },
        },
      },
    };
    this.chart = new Chart(ctx, config);
  }
}
