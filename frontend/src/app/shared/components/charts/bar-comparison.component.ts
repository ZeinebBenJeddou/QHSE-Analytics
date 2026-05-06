import {
  AfterViewInit, Component, ElementRef, Input,
  OnChanges, OnDestroy, SimpleChanges, ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, Plugin, registerables } from 'chart.js';
import { BarreGroupeeData } from '../../../core/models/dashboard.model';

Chart.register(...registerables);

@Component({
  selector: 'app-bar-comparison',
  standalone: true,
  imports: [CommonModule],
  template: `<div class="chart-wrapper"><canvas #chartCanvas></canvas></div>`,
  styles: [`
    :host { display: block; width: 100%; height: 100%; }
    .chart-wrapper { position: relative; width: 100%; height: 100%; min-height: 260px; }
    canvas { width: 100% !important; height: 100% !important; }
  `]
})
export class BarComparisonComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() data: BarreGroupeeData[] = [];
  @Input() labelN1 = 'N-1';
  @Input() labelN  = 'N';

  @ViewChild('chartCanvas', { static: true }) canvas?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart<'bar'>;

  ngAfterViewInit(): void { this.buildChart(); }
  ngOnChanges(c: SimpleChanges): void {
    if (c['data'] || c['labelN1'] || c['labelN']) this.buildChart();
  }
  ngOnDestroy(): void { this.chart?.destroy(); }

  private buildChart(): void {
    this.chart?.destroy();
    if (!this.canvas || !this.data?.length) return;
    const ctx = this.canvas.nativeElement.getContext('2d');
    if (!ctx) return;

    // Gradient fills
    const gradN1 = ctx.createLinearGradient(0, 0, 0, 300);
    gradN1.addColorStop(0, 'rgba(30,111,217,0.75)');
    gradN1.addColorStop(1, 'rgba(30,111,217,0.15)');

    const gradN = ctx.createLinearGradient(0, 0, 0, 300);
    gradN.addColorStop(0, 'rgba(107,70,193,0.80)');
    gradN.addColorStop(1, 'rgba(107,70,193,0.15)');

    const NAVY   = '#2b3674';
    const MUTED  = '#a3aed1';
    const BORDER = '#e2e8f0';
    const GRID   = 'rgba(226,234,246,0.7)';
    const FONT   = "'DM Sans', system-ui, sans-serif";
    const MONO   = "'DM Mono', monospace";

    // Plugin: delta label above each group
    const deltaPlugin: Plugin<'bar'> = {
      id: 'deltaLabels',
      afterDatasetsDraw(chart) {
        const { ctx, data, scales } = chart as any;
        const lignes: BarreGroupeeData[] = (chart as any).__deltaData ?? [];
        if (!lignes.length) return;
        ctx.save();
        lignes.forEach((d, i) => {
          const meta0 = chart.getDatasetMeta(0);
          const meta1 = chart.getDatasetMeta(1);
          if (!meta0.data[i] || !meta1.data[i]) return;
          const x = (meta0.data[i].x + meta1.data[i].x) / 2;
          const y = Math.min(meta0.data[i].y, meta1.data[i].y) - 10;
          const delta = d.variationMoyenne;
          const label = `${delta > 0 ? '+' : ''}${delta.toFixed(1)}%`;
          ctx.font = `700 10px ${MONO}`;
          ctx.textAlign = 'center';
          ctx.fillStyle = delta > 5 ? '#ee5d50' : delta < -5 ? '#05cd99' : MUTED;
          ctx.fillText(label, x, y);
        });
        ctx.restore();
      }
    };

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      plugins: [deltaPlugin],
      data: {
        labels: this.data.map(d => d.categorie),
        datasets: [
          {
            label: this.labelN1,
            data: this.data.map(d => d.valeurMoyenneN1),
            backgroundColor: gradN1,
            borderColor: 'rgba(30,111,217,0.9)',
            borderWidth: 1.5,
            borderRadius: 8,
            borderSkipped: false,
          },
          {
            label: this.labelN,
            data: this.data.map(d => d.valeurMoyenneN),
            backgroundColor: gradN,
            borderColor: 'rgba(107,70,193,0.9)',
            borderWidth: 1.5,
            borderRadius: 8,
            borderSkipped: false,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 700, easing: 'easeOutQuart' },
        plugins: {
          legend: {
            labels: {
              color: MUTED,
              font: { family: FONT, size: 12, weight: 600 },
              boxWidth: 12,
              boxHeight: 12,
              borderRadius: 4,
              useBorderRadius: true,
              padding: 16,
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
              afterBody: (items) => {
                const d = this.data[items[0]?.dataIndex];
                if (!d) return [];
                const delta = d.variationMoyenne;
                return [`Δ variation: ${delta > 0 ? '+' : ''}${delta.toFixed(1)}%`];
              },
            },
          },
        },
        scales: {
          x: {
            ticks: { color: '#707eae', font: { family: FONT, size: 12 } },
            grid: { display: false },
            border: { color: BORDER },
          },
          y: {
            ticks: { color: MUTED, font: { family: MONO, size: 10 } },
            grid: { color: GRID, lineWidth: 1 },
            border: { color: BORDER, dash: [4, 3] },
          },
        },
      },
    };

    this.chart = new Chart(ctx, config);
    // Pass data to delta plugin
    (this.chart as any).__deltaData = this.data;
  }
}