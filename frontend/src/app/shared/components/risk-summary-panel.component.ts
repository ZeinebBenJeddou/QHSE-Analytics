import { Component, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { KpiCalculatedDTO } from '../../core/models/import-session.model';

/**
 * Risk Summary Panel Component
 * 
 * Displays:
 * - Total CRITICAL KPIs count
 * - Overall Risk Score
 * - Top risky KPIs with variation metrics
 * - Color-coded severity indicators
 */
@Component({
  selector: 'app-risk-summary-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatChipsModule
  ],
  templateUrl: './risk-summary-panel.component.html',
  styleUrls: ['./risk-summary-panel.component.css']
})
export class RiskSummaryPanelComponent {
  @Input() risks: KpiCalculatedDTO[] | null = null;
  @Input() riskScore: number | null = null;
  @Input() totalKpis: number = 0;

  expandedDetails = signal(false);

  get criticalCount(): number {
    return this.risks?.length ?? 0;
  }

  get riskPercentage(): number {
    if (this.totalKpis === 0) return 0;
    return Math.round((this.criticalCount / this.totalKpis) * 100);
  }

  get riskLevel(): 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' {
    if (!this.riskScore) return 'LOW';
    if (this.riskScore >= 10) return 'CRITICAL';
    if (this.riskScore >= 5) return 'HIGH';
    if (this.riskScore >= 1) return 'MEDIUM';
    return 'LOW';
  }

  get riskLevelLabel(): string {
    return {
      'CRITICAL': 'Critique',
      'HIGH': 'Élevé',
      'MEDIUM': 'Moyen',
      'LOW': 'Faible'
    }[this.riskLevel];
  }

  getRiskLevelIcon(): string {
    return {
      'CRITICAL': 'dangerous',
      'HIGH': 'warning',
      'MEDIUM': 'info',
      'LOW': 'check_circle'
    }[this.riskLevel];
  }

  getRiskLevelColor(): string {
    return {
      'CRITICAL': 'red',
      'HIGH': 'orange',
      'MEDIUM': 'amber',
      'LOW': 'green'
    }[this.riskLevel];
  }

  /**
   * Get top risky KPIs (up to 5)
   */
  get topRiskyKpis(): KpiCalculatedDTO[] {
    return (this.risks ?? []).slice(0, 5);
  }

  /**
   * Format variation for display
   */
  formatVariation(variation: number | undefined): string {
    if (variation === undefined) return 'N/A';
    return (variation > 0 ? '+' : '') + (variation ?? 0).toFixed(1) + '%';
  }

  toggleDetails(): void {
    this.expandedDetails.set(!this.expandedDetails());
  }

  hasRisks(): boolean {
    return (this.risks?.length ?? 0) > 0;
  }
}
