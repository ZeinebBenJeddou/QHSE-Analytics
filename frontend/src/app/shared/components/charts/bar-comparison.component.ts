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

    const COLOR_N1 = '#CBD5E1';   // gris clair N-1
    const COLOR_N  = '#1E40AF';   // bleu marine N

    const NAVY   = '#0F172A';
    const MUTED  = '#94A3B8';
    const BORDER = '#E2E8F0';
    const GRID   = '#F1F5F9';
    const FONT   = "'Inter', system-ui, sans-serif";
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
            backgroundColor: COLOR_N1,
            borderColor: '#94A3B8',
            borderWidth: 1,
            borderRadius: 6,
            borderSkipped: false,
          },
          {
            label: this.labelN,
            data: this.data.map(d => d.valeurMoyenneN),
            backgroundColor: COLOR_N,
            borderColor: '#1E3A8A',
            borderWidth: 1,
            borderRadius: 6,
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
            border: { color: BORDER, dash: [3, 3] },
          },
        },
      },
    };

    this.chart = new Chart(ctx, config);
    // Pass data to delta plugin
    (this.chart as any).__deltaData = this.data;
  }
}