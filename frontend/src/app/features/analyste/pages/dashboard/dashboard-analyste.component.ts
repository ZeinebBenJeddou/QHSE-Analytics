import {
  Component, inject, OnInit, OnDestroy, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Subject, catchError, finalize, of } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { DashboardService } from '../../../../core/services/dashboard.service';
import { AiAnalysisService } from '../../../../core/services/ai-analysis.service';
import { ImportService } from '../../../../core/services/import.service';
import { ImportSessionStateService } from '../../../../core/services/import-session-state.service';
import { AdminService } from '../../../../core/services/admin.service';
import { ProfileResponse } from '../../../admin/models/admin.models';
import {
  ResumeAnalysteResponse,
  ComparatifTableauResponse,
  GraphiquesDataResponse,
  LigneComparatifResponse,
} from '../../../../core/models/dashboard.model';
import { AnalyseCompleteResponse, AiAnalysisStructuredResponse, ResultatKpiIaResponse } from '../../../../core/models/analyse-ia.model';

import { BarComparisonComponent } from '../../../../shared/components/charts/bar-comparison.component';
import { PieDistributionComponent } from '../../../../shared/components/charts/pie-distribution.component';


interface CategorieInfo {
  code: string;
  libelle: string;
  icon: string;
  color: string;
  bgLight: string;
}

const CATEGORIES: CategorieInfo[] = [
  { code: 'Q', libelle: 'Qualité',       icon: 'verified',  color: '#4318FF', bgLight: '#f0edff' },
  { code: 'H', libelle: 'Hygiène',       icon: 'sanitizer', color: '#05cd99', bgLight: '#e8f8f0' },
  { code: 'S', libelle: 'Sécurité',      icon: 'security',  color: '#ee5d50', bgLight: '#ffebee' },
  { code: 'E', libelle: 'Environnement', icon: 'eco',       color: '#ff9800', bgLight: '#fff3e0' },
];

interface SummaryCardVm {
  label: string;
  value: string | number;
  icon: string;
  color: 'blue' | 'red' | 'orange' | 'green';
  note: string;
  ratio?: number;
}
interface InsightCardVm {
  title: string;
  description: string;
  icon: string;
  tone: 'danger' | 'success' | 'warning';
  badge: string;
}
@Component({
  selector: 'app-dashboard-analyste',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatChipsModule, MatTooltipModule,
    MatProgressSpinnerModule, MatTabsModule,
    MatInputModule, MatFormFieldModule, MatSelectModule,
    MatExpansionModule, MatSnackBarModule,
    MatBadgeModule, MatDividerModule, MatProgressBarModule,
    BarComparisonComponent, PieDistributionComponent,
  ],
  templateUrl: './dashboard-analyste.component.html',
  styleUrls: ['./dashboard-analyste.component.css'],
})
export class DashboardAnalysteComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dashboardService = inject(DashboardService);
  private aiAnalysisService = inject(AiAnalysisService);
  private importService = inject(ImportService);
  private adminService = inject(AdminService);
  private snackBar = inject(MatSnackBar);
  private sessionState = inject(ImportSessionStateService);
  private readonly destroy$ = new Subject<void>();

  importId = signal<number | null>(null);
  exporting = signal(false);
  loading = signal(true);
  error = signal('');

  resume = signal<ResumeAnalysteResponse | null>(null);
  profile = signal<ProfileResponse | null>(null);

  get hasImport(): boolean {
    return !!this.importId();
  }
  comparatif = signal<ComparatifTableauResponse | null>(null);
  graphiques = signal<GraphiquesDataResponse | null>(null);
  analysesIa = signal<AnalyseCompleteResponse | null>(null);
  structured = signal<AiAnalysisStructuredResponse | null>(null);

  analysteInitiales = computed((): string => {
    const p = this.profile();
    if (!p) return 'A';
    const first = (p.prenom ?? '').charAt(0).toUpperCase();
    const last  = (p.nom   ?? '').charAt(0).toUpperCase();
    return first + last || 'A';
  });

  analysteNomComplet = computed((): string => {
    const p = this.profile();
    if (!p) return 'Analyste QHSE';
    return `${p.prenom ?? ''} ${(p.nom ?? '').toUpperCase()}`.trim() || 'Analyste QHSE';
  });

  scorecardsData = computed(() => {
    const res = this.resume();
    const cmp = this.comparatif();
    const lignes = cmp?.lignes ?? [];
    const total    = res?.nombreTotalKpis ?? lignes.length;
    const critiques = res?.nombreTotalCritiques  ?? cmp?.nombreCritiques ?? 0;
    const moderes   = res?.nombreTotalModeres    ?? cmp?.nombreModeres   ?? 0;
    const faibles   = res?.nombreTotalFaibles    ?? cmp?.nombreFaibles   ?? 0;
    const aiCoverage = lignes.filter(l =>
      l.immediateAction || l.correctiveAction || l.aiNote || l.riskJustification
    ).length;
    const pct = (n: number) => total > 0 ? +(n / total * 100).toFixed(1) : 0;
    return {
      total,
      critiques,   critiquesPct: pct(critiques),
      moderes,     moderesPct:   pct(moderes),
      faibles,     faiblesPct:   pct(faibles),
      aiCoverage,  aiCoveragePct: pct(aiCoverage),
    };
  });

  topKpisDegrades = computed(() => this.graphiques()?.topKpisDegrades ?? []);

  iaGlobalScore = computed(() => {
    const kpis = this.analysesIa()?.analysesKpis ?? [];
    if (!kpis.length) return null;
    const baseByNiveau: Record<string, number> = { FAIBLE: 100, MODERE: 60, CRITIQUE: 20 };
    let weightedSum = 0, totalWeight = 0;
    for (const k of kpis) {
      const base   = baseByNiveau[k.niveauVariation ?? ''] ?? 70;
      const weight = k.niveauVariation === 'CRITIQUE' ? 2 : 1;
      const variation = k.variationRelative ?? 0;
      const penalty = (k.tendance === 'BAISSE' && variation < -15) ? Math.min(15, Math.abs(variation) * 0.1) : 0;
      weightedSum += (base - penalty) * weight;
      totalWeight += weight;
    }
    return Math.max(0, Math.min(100, Math.round(weightedSum / totalWeight)));
  });

  iaScoreLabel = computed(() => {
    const s = this.iaGlobalScore();
    if (s === null) return '';
    if (s >= 80) return 'Excellent';
    if (s >= 60) return 'Satisfaisant';
    if (s >= 40) return 'À surveiller';
    return 'Critique';
  });

  iaScoreColor = computed(() => {
    const s = this.iaGlobalScore();
    if (s === null) return '#a3aed1';
    if (s >= 80) return '#05cd99';
    if (s >= 60) return '#4318FF';
    if (s >= 40) return '#ff9800';
    return '#ee5d50';
  });

  iaScoreRingDash = computed(() => {
    const pct = (this.iaGlobalScore() ?? 0) / 100;
    return `${Math.round(pct * 339)} 339`;
  });

  iaRecommendations = computed(() => this.structured()?.recommendations?.slice(0, 3) ?? []);

  formatPriorityLabel(urgency: string): string {
    const map: Record<string, string> = { HIGH: 'Haute', MEDIUM: 'Moyenne', LOW: 'Basse', haute: 'Haute', moyenne: 'Moyenne', basse: 'Basse' };
    return map[urgency] ?? urgency;
  }

  searchFilter = signal('');
  categorieFilter = signal('');
  niveauFilter = signal('');
  activeCatTab = signal('');

  private statutFilterSig = signal('');
  get statutFilter(): string { return this.statutFilterSig(); }
  set statutFilter(value: string) { this.statutFilterSig.set(value); }

  readonly CATEGORIES = CATEGORIES;

  sortColumn = signal<string>('variationAbsolue');
  sortDir = signal<'asc' | 'desc'>('desc');

  private filtersKey = 'analyste.dashboard.filters';

  expandedRows = signal<Set<number>>(new Set());

  tableColumns = ['expand', 'kpiNom', 'categorieLibelle', 'valeurN1', 'valeurN', 'variationAbsolue', 'variationRelative', 'niveauVariation', 'tendance'];

  getStatut(tendance: string | null, niveau: string | null): string {
    if (tendance === 'HAUSSE' && niveau === 'CRITIQUE')                        return 'Dégradation';
    if (tendance === 'HAUSSE' && niveau === 'PRE_ESCALADE')                    return 'Dégradation';
    if (tendance === 'HAUSSE' && (niveau === 'MODERE' || niveau === 'FAIBLE')) return 'Dégradation légère';
    if (tendance === 'HAUSSE' && niveau === 'EXCELLENT')                       return 'Amélioration';
    if (tendance === 'BAISSE')                                                  return 'Amélioration';
    return 'Stable';
  }

  getStatutClass(statut: string): string {
    if (statut === 'Dégradation')        return 'statut-red';
    if (statut === 'Dégradation légère') return 'statut-orange';
    if (statut === 'Amélioration')       return 'statut-green';
    return 'statut-blue';
  }

  getNiveauClass(niveau: string | null): string {
    if (niveau === 'CRITIQUE')     return 'niveau-critique';
    if (niveau === 'PRE_ESCALADE') return 'niveau-pre-escalade';
    if (niveau === 'MODERE')       return 'niveau-modere';
    if (niveau === 'FAIBLE')       return 'niveau-faible';
    if (niveau === 'EXCELLENT')    return 'niveau-excellent';
    if (niveau === 'INDETERMINE')  return 'niveau-default';
    return 'niveau-default';
  }

  getTendanceIcon(tendance: string | null): string {
    if (tendance === 'HAUSSE') return 'trending_up';
    if (tendance === 'BAISSE') return 'trending_down';
    return 'trending_flat';
  }

  getTendanceIconClass(tendance: string | null, niveau?: string | null): string {
    const statut = this.getStatut(tendance, niveau ?? null);
    if (statut === 'Dégradation')        return 'icon-red';
    if (statut === 'Dégradation légère') return 'icon-orange';
    if (statut === 'Amélioration')       return 'icon-green';
    return 'icon-gray';
  }

  getVariationClass(value: number): string {
    if (value > 10)  return 'var-neg';
    if (value < -10) return 'var-pos';
    return 'var-neutral';
  }

  getVariationClassByStatut(value: number, tendance: string | null, niveau: string | null): string {
    const statut = this.getStatut(tendance, niveau);
    if (statut === 'Dégradation')        return 'var-neg';
    if (statut === 'Dégradation légère') return 'var-neg-light';
    if (statut === 'Amélioration')       return 'var-pos';
    return 'var-neutral';
  }

  variationSign(value: number): string {
    return value > 0 ? '+' : '';
  }

  getCatInfo(code: string): CategorieInfo {
    return CATEGORIES.find(c => c.code === code)
      ?? { code, libelle: code, icon: 'label', color: '#a3aed1', bgLight: '#f4f7fe' };
  }

  setActiveCat(code: string): void {
    this.activeCatTab.set(code);
    this.categorieFilter.set('');
  }

  nombreIndetermines = computed(() =>
    (this.comparatif()?.lignes ?? []).filter(l => l.niveauVariation === 'INDETERMINE').length
  );

  filteredLignes = computed(() => {
    const lignes = this.comparatif()?.lignes ?? [];
    const search = this.searchFilter().toLowerCase();
    const cat = this.categorieFilter() || this.activeCatTab();
    const niveau = this.niveauFilter();
    const statut = this.statutFilterSig();
    const filtered = lignes.filter(l => {
      if (l.niveauVariation === 'INDETERMINE' && niveau !== 'INDETERMINE') return false;
      return (
        (!search || l.kpiNom.toLowerCase().includes(search)) &&
        (!cat    || l.categorieCode === cat) &&
        (!niveau || l.niveauVariation === niveau) &&
        (!statut || this.getStatut(l.tendance, l.niveauVariation) === statut)
      );
    });
    const col = this.sortColumn();
    const dir = this.sortDir();
    return [...filtered].sort((a, b) => {
      const aVal = (a as unknown as Record<string, unknown>)[col] ?? 0;
      const bVal = (b as unknown as Record<string, unknown>)[col] ?? 0;
      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return dir === 'asc' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
      }
      const aNum = Number(aVal);
      const bNum = Number(bVal);
      return dir === 'asc' ? aNum - bNum : bNum - aNum;
    });
  });

  catStats = computed(() => {
    const lignes = this.comparatif()?.lignes ?? [];
    return CATEGORIES.map(cat => {
      const items    = lignes.filter(l => l.categorieCode === cat.code);
      const critique = items.filter(l => l.niveauVariation === 'CRITIQUE').length;
      const modere   = items.filter(l => l.niveauVariation === 'MODERE').length;
      const faible   = items.filter(l => l.niveauVariation === 'FAIBLE').length;
      const determined = items.filter(l => l.niveauVariation !== 'INDETERMINE' && l.variationRelative != null);
      const avgVar   = determined.length
        ? determined.reduce((s, l) => s + l.variationRelative!, 0) / determined.length
        : 0;
      return { ...cat, total: items.length, critique, modere, faible, avgVar };
    });
  });

  get stats() {
    const lignes = this.filteredLignes();
    const total  = lignes.length;
    let amelioration = 0, degradation = 0, stables = 0, sumVar = 0, countVar = 0;
    lignes.forEach(l => {
      if (l.niveauVariation === 'INDETERMINE' || l.variationRelative == null) return;
      if (l.variationRelative < -10)     amelioration++;
      else if (l.variationRelative > 10) degradation++;
      else                               stables++;
      sumVar += l.variationRelative;
      countVar++;
    });
    const avg = countVar > 0 ? sumVar / countVar : 0;
    return {
      total, amelioration, degradation, stables,
      avgVariation: avg,
      ameliorationPct: total ? +(amelioration / total * 100).toFixed(1) : 0,
      degradationPct:  total ? +(degradation  / total * 100).toFixed(1) : 0,
      stablesPct:      total ? +(stables      / total * 100).toFixed(1) : 0,
    };
  }

  maxVariationAbs = computed(() => {
    const lignes = this.comparatif()?.lignes ?? [];
    return Math.max(...lignes.map(l => Math.abs(l.variationRelative)), 1);
  });

  categories = computed(() => {
    const lignes = this.comparatif()?.lignes ?? [];
    return [...new Set(lignes.map(l => l.categorieCode))];
  });

  recommendationCards = computed((): InsightCardVm[] => {
    const lignes = this.filteredLignes();
    const priorityOrder: Record<string, number> = { 'Haute': 3, 'Moyenne': 2, 'Basse': 1 };

    const withActions = [...lignes]
      .filter(l => l.immediateAction || l.correctiveAction || l.preventiveAction)
      .sort((a, b) =>
        ((priorityOrder[b.immediatePriority ?? ''] ?? 0) - (priorityOrder[a.immediatePriority ?? ''] ?? 0)) ||
        (Math.abs(b.variationRelative) - Math.abs(a.variationRelative))
      );

    if (!withActions.length) {
      
      return [];
    }

    return withActions.slice(0, 3).map(l => ({
      title: l.kpiNom,
      description: l.immediateAction ?? l.correctiveAction ?? l.preventiveAction ?? '',
      icon: l.immediatePriority === 'Haute' ? 'bolt' : l.immediatePriority === 'Moyenne' ? 'build_circle' : 'shield',
      tone: (l.immediatePriority === 'Haute' ? 'danger' : l.immediatePriority === 'Moyenne' ? 'warning' : 'success') as 'danger' | 'success' | 'warning',
      badge: l.immediatePriority === 'Haute' ? 'Priorité haute' : l.immediatePriority === 'Moyenne' ? 'Priorité moyenne' : 'Priorité basse',
    }));
  });

  hasAiData = computed(() =>
    !!(this.structured()?.globalSummary) ||
    (this.comparatif()?.lignes ?? []).some(k =>
      k.aiNote || k.riskJustification || k.issueDetected ||
      k.immediateAction || k.correctiveAction || k.riskLevel
    )
  );

  planActionLines = computed((): string[] => {
    const structuredPlan = this.structured()?.actionPlan ?? [];
    if (structuredPlan.length > 0) {
      return structuredPlan.map(a => a.action).filter(Boolean);
    }
    const legacy = this.analysesIa()?.analyseGlobale?.planActions ?? '';
    return legacy.split('\n').map(s => s.trim()).filter(s => s.length > 0);
  });

  topIssues = computed((): LigneComparatifResponse[] => {
    const lignes = this.comparatif()?.lignes ?? [];
    const priorityOrder: Record<string, number> = { CRITIQUE: 3, MODERE: 2, FAIBLE: 1 };

    // Enrichir avec les insights structurés si disponibles
    const structuredInsights = this.structured()?.kpiInsights ?? [];
    const insightByName = new Map(structuredInsights.map(i => [i.kpiName?.toLowerCase().trim(), i]));

    const critical = [...lignes]
      .filter(k => k.niveauVariation === 'CRITIQUE' || k.niveauVariation === 'MODERE')
      .sort((a, b) =>
        (priorityOrder[b.niveauVariation ?? 'FAIBLE'] ?? 0) -
        (priorityOrder[a.niveauVariation ?? 'FAIBLE'] ?? 0)
      )
      .slice(0, 6)
      .map(k => {
        const ins = insightByName.get(k.kpiNom?.toLowerCase().trim() ?? '');
        return ins
          ? { ...k, issueDetected: ins.insight ?? k.issueDetected, riskJustification: ins.riskIfNotDone ?? k.riskJustification }
          : k;
      });

    if (critical.length > 0) return critical;

    return [...lignes]
      .filter(k => k.riskJustification || k.aiNote || k.riskLevel || k.issueDetected)
      .sort((a, b) => Math.abs(b.variationRelative ?? 0) - Math.abs(a.variationRelative ?? 0))
      .slice(0, 6);
  });

  topActions = computed((): LigneComparatifResponse[] => {
    // Prioriser les actions du plan structuré (plus riche) si disponibles
    const structuredInsights = this.structured()?.kpiInsights ?? [];
    if (structuredInsights.length > 0) {
      const levelOrder: Record<string, number> = { HIGH: 3, MEDIUM: 2, LOW: 1 };
      const lignes = this.comparatif()?.lignes ?? [];
      const ligneByName = new Map(lignes.map(l => [l.kpiNom?.toLowerCase().trim(), l]));

      return [...structuredInsights]
        .filter(i => i.actionImmediate)
        .sort((a, b) => (levelOrder[b.urgency ?? ''] ?? 0) - (levelOrder[a.urgency ?? ''] ?? 0))
        .slice(0, 5)
        .map(i => {
          const base = ligneByName.get(i.kpiName?.toLowerCase().trim() ?? '');
          return {
            ...(base ?? {} as LigneComparatifResponse),
            kpiNom: i.kpiName,
            immediateAction: i.actionImmediate,
            immediatePriority: i.urgency === 'HIGH' ? 'Haute' : i.urgency === 'MEDIUM' ? 'Moyenne' : 'Basse',
            niveauVariation: base?.niveauVariation ?? null,
          } as LigneComparatifResponse;
        });
    }

    const levelOrder: Record<string, number> = {
      Haute: 5, CRITIQUE: 4, Moyenne: 3, MODERE: 2, Basse: 1, FAIBLE: 0
    };
    return [...this.comparatif()?.lignes ?? []]
      .filter(k => !!(k.immediateAction || k.correctiveAction || k.preventiveAction || k.riskJustification))
      .sort((a, b) => {
        const pa = levelOrder[a.immediatePriority ?? a.niveauVariation ?? 'Basse'] ?? 0;
        const pb = levelOrder[b.immediatePriority ?? b.niveauVariation ?? 'Basse'] ?? 0;
        return pb - pa;
      })
      .slice(0, 5);
  });

  dynamicSynthese = computed((): string | null => {
    const structuredGlobal = this.structured()?.globalSummary?.trim();
    if (structuredGlobal) return structuredGlobal;

    const legacyGlobal = this.analysesIa()?.analyseGlobale?.synthese?.trim();
    if (legacyGlobal) return legacyGlobal;

    const withAi = [...this.comparatif()?.lignes ?? []]
      .filter(k => k.aiNote || k.riskJustification || k.issueDetected)
      .slice(0, 3);
    if (!withAi.length) return null;

    return withAi
      .map(k => `• ${k.kpiNom} : ${k.aiNote ?? k.riskJustification ?? k.issueDetected}`)
      .join('\n');
  });

  get dataYearN(): number | null {
    return this.comparatif()?.periodeN ?? this.resume()?.periodeN ?? null;
  }

  get dataYearN1(): number | null {
    return this.comparatif()?.periodeN1 ?? this.resume()?.periodeN1 ?? null;
  }

  get analysisDateLabel(): string | null {
    const raw = this.comparatif()?.dateAnalyse ?? this.resume()?.dateAnalyse ?? null;
    if (!raw) return null;
    const d = new Date(raw);
    if (isNaN(d.getTime())) return raw;
    const date = d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    const time = d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    return `${date} à ${time}`;
  }

  get periodLabel(): string {
    const yearN = this.dataYearN;
    if (!yearN) return 'Période en cours';
    return `Janv. - Déc. ${yearN}`;
  }

  get previousPeriodLabel(): string {
    const yearN1 = this.dataYearN1;
    return yearN1 ? String(yearN1) : 'N-1';
  }

  get titleSuffix(): string {
    const yearN = this.dataYearN;
    const yearN1 = this.dataYearN1;
    if (!yearN || !yearN1) return 'Comparatif de performance';
    return `Comparatif Année ${yearN1} vs Année ${yearN}`;
  }

  get summaryCards(): SummaryCardVm[] {
    const resume = this.resume();
    const comparatif = this.comparatif();
    const lignes = comparatif?.lignes ?? [];
    const totalKpis = resume?.nombreTotalKpis ?? lignes.length ?? 0;
    const critiques     = resume?.nombreTotalCritiques  ?? comparatif?.nombreCritiques ?? 0;
    const preEscalades  = resume?.nombreTotalPreEscalades  ?? 0;
    const moderes       = resume?.nombreTotalModeres    ?? comparatif?.nombreModeres   ?? 0;
    const faibles       = resume?.nombreTotalFaibles    ?? comparatif?.nombreFaibles   ?? 0;
    const excellents    = resume?.nombreTotalExcellents    ?? 0;
    const indetermines  = resume?.nombreTotalIndetermines  ?? 0;

    return [
      {
        label: 'INDICATEURS SUIVIS',
        value: totalKpis,
        icon: 'assignment',
        color: 'blue',
        note: `${this.dataYearN1 ?? 'N-1'} vs ${this.dataYearN ?? 'N'}`,
      },
      {
        label: 'ALERTES CRITIQUES',
        value: critiques,
        icon: 'warning',
        color: 'red',
        note: critiques > 0 ? `${critiques} à traiter en priorité` : 'Tous les seuils respectés',
      },
      {
        label: 'PRÉ-ESCALADE',
        value: preEscalades,
        icon: 'trending_up',
        color: 'orange',
        note: totalKpis ? `${Math.round((preEscalades / totalKpis) * 100)}% du total` : '',
      },
      {
        label: 'MODÉRÉS',
        value: moderes,
        icon: 'swap_vert',
        color: 'orange',
        note: totalKpis ? `${Math.round((moderes / totalKpis) * 100)}% du total` : '',
      },
      {
        label: 'FAIBLES',
        value: faibles,
        icon: 'trending_down',
        color: 'green',
        note: totalKpis ? `${Math.round((faibles / totalKpis) * 100)}% du total` : '',
      },
      {
        label: 'EXCELLENTS',
        value: excellents,
        icon: 'star',
        color: 'green',
        note: totalKpis ? `${Math.round((excellents / totalKpis) * 100)}% du total` : '',
      },
      {
        label: 'INDÉTERMINÉS',
        value: indetermines,
        icon: 'help_outline',
        color: 'blue',
        note: indetermines > 0 ? 'Données N-1 manquantes' : 'Aucun KPI indéterminé',
      },
    ];
  }

  get donutChartData() {
    const lignes = this.comparatif()?.lignes ?? [];
    let enHausseCritique = 0;
    let enHausseModeree = 0;
    let enBaisseModeree = 0;
    let enBaisseFaible = 0;

    lignes.forEach(l => {
      if (l.tendance === 'HAUSSE') {
        if (l.niveauVariation === 'CRITIQUE') enHausseCritique++;
        else enHausseModeree++;
      } else if (l.tendance === 'BAISSE') {
        if (l.niveauVariation === 'MODERE' || l.niveauVariation === 'CRITIQUE') enBaisseModeree++;
        else enBaisseFaible++;
      }
    });

    return { enHausseCritique, enHausseModeree, enBaisseModeree, enBaisseFaible };
  }

  ngOnInit() {
    this.adminService.getCurrentProfile()
      .pipe(catchError(() => of(null)), takeUntil(this.destroy$))
      .subscribe(p => { if (p) this.profile.set(p); });

    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.importId.set(+id);
        this.loadSelectedImport(+id);
      } else {
        this.loadResume();
      }
    });

    this.loadSavedFilters();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadResume() {
    this.dashboardService.getResume().subscribe({
      next: res => {
        this.resume.set(res);
        this.importId.set(res.dernierImportId);
        this.sessionState.setActiveImport(res.dernierImportId);
        this.sessionState.patch({ resume: res });
        this.loadDashboardData(res.dernierImportId, { includeComparatif: true });
      },
      error: err => {
        this.error.set(this.extractErrorMessage(err, 'Aucune donnée disponible. Veuillez importer des données.'));
        this.loading.set(false);
      }
    });
  }

  loadSelectedImport(importId: number) {
    this.loading.set(true);
    this.error.set('');
    this.comparatif.set(null);
    this.graphiques.set(null);
    this.analysesIa.set(null);
    this.structured.set(null);
    this.expandedRows.set(new Set());
    this.sessionState.setActiveImport(importId);

    this.dashboardService.getComparatif(importId).subscribe({
      next: res => {
        this.comparatif.set(res);
        this.sessionState.patch({ comparatif: res });
        this.loadDashboardData(importId, { includeComparatif: false });
      },
      error: err => {
        this.error.set(this.extractErrorMessage(err, 'Import non disponible. Sélectionnez un import traité depuis l\'historique.'));
        this.loading.set(false);
      }
    });
  }


  private loadSavedFilters(): void {
    try {
      const raw = localStorage.getItem(this.filtersKey);
      if (!raw) return;
      const obj = JSON.parse(raw);
      if (obj.search) this.searchFilter.set(obj.search);
      if (obj.categorie) this.categorieFilter.set(obj.categorie);
      if (obj.niveau) this.niveauFilter.set(obj.niveau);
    } catch {
      
    }
  }

  private saveFilters(): void {
    const payload = {
      search: this.searchFilter(),
      categorie: this.categorieFilter(),
      niveau: this.niveauFilter(),
    };
    try { localStorage.setItem(this.filtersKey, JSON.stringify(payload)); } catch {}
  }

  onCategoryChange(val: string): void {
    this.categorieFilter.set(val);
    this.saveFilters();
  }

  onSearchChange(val: string): void {
    this.searchFilter.set(val);
    this.saveFilters();
  }

  onNiveauChange(val: string): void {
    this.niveauFilter.set(val);
    this.saveFilters();
  }

  toggleSort(col: string): void {
    if (this.sortColumn() === col) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortColumn.set(col);
      this.sortDir.set('desc');
    }
  }

  sortIcon(col: string): string {
    if (this.sortColumn() !== col) return 'unfold_more';
    return this.sortDir() === 'asc' ? 'arrow_upward' : 'arrow_downward';
  }

  getBarWidth(val: number): string {
    const max = this.maxVariationAbs();
    if (!max) return '0%';
    return Math.min(100, Math.round((Math.abs(val) / max) * 100)) + '%';
  }

  variationBarClass(row: LigneComparatifResponse): string {
    if (row.tendance === 'HAUSSE' && row.niveauVariation === 'CRITIQUE') return 'bar-critique';
    if (row.tendance === 'HAUSSE') return 'bar-hausse';
    if (row.tendance === 'BAISSE') return 'bar-baisse';
    return 'bar-stable';
  }

  catLabel(code: string): string {
    const map: Record<string, string> = { Q: 'Qualité', H: 'Hygiène', S: 'Sécurité', E: 'Environnement' };
    return map[code?.toUpperCase()] ?? code;
  }

  catCount(code: string): number {
    return (this.comparatif()?.lignes ?? []).filter(l => l.categorieCode === code).length;
  }


  loadDashboardData(importId: number, options: { includeComparatif?: boolean } = {}) {
    const { includeComparatif = false } = options;
    this.loading.set(true);
    this.error.set('');

    const endpoints = includeComparatif ? 4 : 3;
    let done = 0;
    const check = () => { if (++done === endpoints) this.loading.set(false); };

    if (includeComparatif) {
      this.dashboardService.getComparatif(importId)
        .pipe(
          catchError(err => {
            this.error.set(this.extractErrorMessage(err, 'Impossible de charger le comparatif.'));
            return of(null);
          }),
          finalize(check)
        )
        .subscribe({ next: r => { if (r) this.comparatif.set(r); } });
    }

    this.dashboardService.getGraphiques(importId)
      .pipe(
        catchError(err => {
          this.error.set(this.extractErrorMessage(err, 'Impossible de charger les graphiques.'));
          return of(null);
        }),
        finalize(check)
      )
      .subscribe({ next: r => {
        if (r) {
          this.graphiques.set(r);
          this.sessionState.patch({ graphiques: r });
        }
      } });

    this.dashboardService.getAnalysesIa(importId)
      .pipe(
        catchError(() => of(null)),
        finalize(check)
      )
      .subscribe({ next: r => {
        if (r && (r.analysesKpis?.length ?? 0) > 0) {
          this.analysesIa.set(r);
          this.sessionState.patch({ analysesIa: r });
          this.applyAnalysesToComparatif();
        }
      } });

    this.aiAnalysisService.getStructuredAnalysis(importId)
      .pipe(
        catchError(() => of(null)),
        finalize(check)
      )
      .subscribe({ next: r => {
        if (r && r.status !== 'FAILED') {
          this.structured.set(r);
        }
      } });
  }

  private applyAnalysesToComparatif(): void {
    const comparatif = this.comparatif();
    const analyses = this.analysesIa();
    if (!comparatif || !analyses?.analysesKpis?.length) return;

    const analysisMap = new Map<number, ResultatKpiIaResponse>();
    analyses.analysesKpis.forEach(item => analysisMap.set(item.kpiId, item));

    const mergedLignes = comparatif.lignes.map(row => {
      const analysis = analysisMap.get(row.kpiId);
      if (!analysis) return row;
      return {
        ...row,
        riskLevel: analysis.riskLevel ?? row.riskLevel,
        riskJustification: analysis.riskJustification ?? row.riskJustification,
        issueDetected: analysis.issueDetected ?? row.issueDetected,
        correctiveAction: analysis.correctiveAction ?? row.correctiveAction,
        preventiveAction: analysis.preventiveAction ?? row.preventiveAction,
        immediateAction: analysis.immediateAction ?? row.immediateAction,
        immediatePriority: analysis.immediatePriority ?? row.immediatePriority,
        requires8d: analysis.requires8d ?? row.requires8d,
        eightDDetails: analysis.eightDDetails ?? row.eightDDetails,
        aiNote: analysis.aiNote ?? row.aiNote,
      };
    });

    this.comparatif.set({ ...comparatif, lignes: mergedLignes });
  }

  

  toggleRow(kpiId: number): void {
    const current = new Set(this.expandedRows());
    if (current.has(kpiId)) {
      current.delete(kpiId);
    } else {
      current.add(kpiId);
    }
    this.expandedRows.set(current);
  }

  isExpanded(kpiId: number): boolean {
    return this.expandedRows().has(kpiId);
  }


  parse8D(json: string | undefined): Record<string, string> | null {
    if (!json) return null;
    try {
      return JSON.parse(json);
    } catch {
      return null;
    }
  }

  eightDSteps(json: string | undefined): { key: string; label: string; value: string }[] {
    const data = this.parse8D(json);
    if (!data) return [];
    const labels: Record<string, string> = {
      D1: 'Équipe',
      D2: 'Description du problème',
      D3: 'Actions de confinement',
      D4: 'Cause racine',
      D5: 'Actions correctives',
      D6: 'Validation',
      D7: 'Prévention',
      D8: 'Clôture',
    };
    return Object.entries(data).map(([key, value]) => ({
      key,
      label: labels[key] ?? key,
      value,
    }));
  }

  

  private extractErrorMessage(err: unknown, fallback: string): string {
    if (!err) return fallback;
    if (err instanceof HttpErrorResponse) {
      if (err.error && typeof err.error === 'object' && (err.error as { message?: string }).message)
        return (err.error as { message: string }).message;
      if (typeof err.error === 'string') return err.error;
      if (err.message) return err.message;
    }
    return fallback;
  }

  variationStyle(val: number): string {
    if (val > 10) return 'critique';
    if (val > 0) return 'hausse';
    if (val < 0) return 'baisse';
    return 'stable';
  }

  catClass(code: string): string {
    const map: Record<string, string> = {
      'Q': 'cat-q',
      'H': 'cat-h',
      'S': 'cat-s',
      'E': 'cat-e',
    };
    return map[code?.toUpperCase()] ?? 'cat-default';
  }

  accentClass(accent: string): string {
    return `accent-${accent}`;
  }

  niveauClass(n: string): string {
    return { CRITIQUE: 'chip-critique', MODERE: 'chip-modere', FAIBLE: 'chip-faible' }[n] ?? '';
  }

  riskClass(r: string | undefined): string {
    if (!r) return 'risk-unknown';
    return { 'Élevé': 'risk-high', 'Modéré': 'risk-medium', 'Faible': 'risk-low' }[r] ?? 'risk-unknown';
  }

  riskIcon(r: string | undefined): string {
    return { 'Élevé': '🔴', 'Modéré': '🟠', 'Faible': '🟢' }[r ?? ''] ?? '⚪';
  }

  priorityClass(p: string | undefined): string {
    if (!p) return '';
    return { 'Haute': 'priority-high', 'Moyenne': 'priority-medium', 'Basse': 'priority-low' }[p] ?? '';
  }

  tendanceIcon(t: string): string {
    return { HAUSSE: 'trending_up', BAISSE: 'trending_down', STABLE: 'trending_flat' }[t] ?? 'remove';
  }

  tendanceClass(t: string): string {
    return { HAUSSE: 'trend-up', BAISSE: 'trend-down', STABLE: 'trend-stable' }[t] ?? '';
  }

  hasDeepAnalysis(row: LigneComparatifResponse): boolean {
    return !!(row.riskLevel || row.aiNote || row.correctiveAction);
  }

  exportPdf() {
    const id = this.importId();
    if (!id) {
      this.snackBar.open('Aucun import sélectionné pour l\'export.', 'OK', { duration: 3000 });
      return;
    }
    this.exporting.set(true);
    this.importService.exportAnalyste(id).subscribe({
      next: (blob) => this.downloadFile(blob, `rapport-import-${id}.pdf`),
      error: () => this.snackBar.open('Erreur lors de l\'export PDF.', 'OK', { duration: 4000 }),
      complete: () => this.exporting.set(false),
    });
  }

  async exportExcel(): Promise<void> {
    const XLSX = await import('xlsx');
    const rows = this.filteredLignes().map(r => ({
      'Indicateur': r.kpiNom,
      'Catégorie': r.categorieLibelle,
      'Année N-1': r.valeurN1,
      'Année N': r.valeurN,
      'Variation Absolue': r.variationAbsolue,
      'Variation Relative (%)': r.variationRelative,
      'Niveau': r.niveauVariation,
      'Tendance': r.tendance,
    }));

    const ws = XLSX.utils.json_to_sheet(rows);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Comparatif');
    const buf = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
    const blob = new Blob([buf], { type: 'application/octet-stream' });
    this.downloadFile(blob, `comparatif-${this.importId() ?? 'resume'}.xlsx`);
  }

  goToIA() {
    const id = this.importId() ?? this.resume()?.dernierImportId ?? null;
    if (!id) {
      this.snackBar.open('Aucun import sélectionné pour l\'analyse IA.', 'OK', { duration: 3000 });
      return;
    }
    window.scrollTo({ top: 0, behavior: 'instant' });
    this.router.navigate(['/analyste/ia', id]);
  }

  private downloadFile(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    window.URL.revokeObjectURL(url);
  }

  goToImport() {
    this.router.navigate(['/analyste/import']);
  }
}

