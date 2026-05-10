import {
  Component, inject, OnInit, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';

import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';

import { DashboardService } from '../../../../core/services/dashboard.service';
import { AiAnalysisService } from '../../../../core/services/ai-analysis.service';
import { ImportSessionStateService } from '../../../../core/services/import-session-state.service';
import { ResumeAnalysteResponse } from '../../../../core/models/dashboard.model';
import { AiAnalysisStructuredResponse, AiKpiInsightResponse, AnalyseCompleteResponse, ResultatKpiIaResponse } from '../../../../core/models/analyse-ia.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

/* ══════════════════════════════════════════════════════════════════════
   DTOs — miroir exact du backend
══════════════════════════════════════════════════════════════════════ */

/* ── Enriched KPI ───────────────────────────────────────────────────── */
interface EnrichedKpi extends ResultatKpiIaResponse {
  insight?: string;
  insightConfidence?: number;
  structuredCauses: string[];
  structuredRecos: string[];
  structuredAction?: string;
  structuredOwner?: string;
  structuredDue?: string;
  structuredSuccess?: string;
  structuredRisk?: string;
  urgency?: string;
}

/* ── Kanban item ────────────────────────────────────────────────────── */
interface KanbanItem {
  id: string;
  action: string;
  priority: string;
  ownerRole?: string;
  dueHorizon?: string;
  source: 'action_plan' | 'kpi_immediate';
  kpiRef?: string;
}

/* ── Category metadata ─────────────────────────────────────────────── */
interface CategorieInfo {
  code: string;
  libelle: string;
  icon: string;
  color: string;
  bgLight: string;
}

const CATEGORIES: CategorieInfo[] = [
  { code: 'Q', libelle: 'Qualité',       icon: 'verified',        color: '#4318FF', bgLight: '#f0edff' },
  { code: 'H', libelle: 'Hygiène',       icon: 'sanitizer',       color: '#05cd99', bgLight: '#e8f8f0' },
  { code: 'S', libelle: 'Sécurité',      icon: 'security',        color: '#ee5d50', bgLight: '#ffebee' },
  { code: 'E', libelle: 'Environnement', icon: 'eco',             color: '#ff9800', bgLight: '#fff3e0' },
];

@Component({
  selector: 'app-analyse-ia',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatIconModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatTooltipModule, MatButtonModule,
  ],
  templateUrl: './analyse-ia.component.html',
  styleUrls: ['./analyse-ia.component.css'],
})
export class AnalyseIAComponent implements OnInit {

  private dashboardService = inject(DashboardService);
  private aiAnalysisService = inject(AiAnalysisService);
  private sessionState     = inject(ImportSessionStateService);
  private snackBar         = inject(MatSnackBar);
  private route            = inject(ActivatedRoute);
  private http             = inject(HttpClient);
  private readonly dashboardAnalysteBase = `${environment.apiUrl}/api/dashboard/analyste`;
  private readonly dashboardAdminBase    = `${environment.apiUrl}/api/dashboard/admin`;
  // ── State ────────────────────────────────────────────────────────────
  importId        = signal<number | null>(null);
  loading         = signal(true);
  regenerating    = signal(false);
  error           = signal('');
  resume          = signal<ResumeAnalysteResponse | null>(null);
  analyse         = signal<AnalyseCompleteResponse | null>(null);
  structured      = signal<AiAnalysisStructuredResponse | null>(null);

  // ── UI state ─────────────────────────────────────────────────────────
  activeCatCode   = signal('Q');
  expandedKpiId   = signal<number | null>(null);

  // ── Filter state ─────────────────────────────────────────────────────
  searchKpi       = signal('');
  niveauKpiFilter = signal('');
  catFilter       = signal('');

  // ── Constants ─────────────────────────────────────────────────────────
  readonly CATEGORIES = CATEGORIES;

  // ── Computed : résumé plan actions (split par \n) ────────────────────
  planActionsList = computed(() => {
    const pa = this.analyse()?.analyseGlobale?.planActions ?? '';
    return pa.split('\n').map(s => s.trim()).filter(s => s.length > 0);
  });

  // ── Computed : structured analysis status ───────────────────────────
  isStructuredSuccess = computed(() => this.structured()?.status === 'SUCCESS');
  isStructuredPartial = computed(() => this.structured()?.status === 'PARTIAL');
  isStructuredFailed = computed(() => this.structured()?.status === 'FAILED');
  showLegacyFallback = computed(() => this.isStructuredFailed() || this.isStructuredPartial());
  structuredExpectedKpis = computed(() => this.analyse()?.analysesKpis?.length ?? 0);
  structuredReceivedKpis = computed(() => this.structured()?.kpiInsights?.length ?? 0);
  structuredMissingKpis = computed(() => Math.max(this.structuredExpectedKpis() - this.structuredReceivedKpis(), 0));
  structuredCoverageLabel = computed(() => {
    const expected = this.structuredExpectedKpis();
    const received = this.structuredReceivedKpis();
    return `${received}/${expected}`;
  });
  structuredStatusLabel = computed(() => {
    if (this.isStructuredPartial()) return 'PARTIAL';
    if (this.isStructuredFailed()) return 'FAILED';
    return 'SUCCESS';
  });

  // ── Computed : confidence badge ─────────────────────────────────────
  showConfidenceBadge = computed(() => {
    const conf = this.structured()?.confidence?.overall ?? 100;
    return conf < 60 || this.isStructuredPartial() || this.isStructuredFailed();
  });

  structuredConfidence = computed(() => this.structured()?.confidence?.overall ?? 0);

  // ── Computed : catégorie active ──────────────────────────────────────
  activeCatAnalyse = computed(() => {
    const code = this.activeCatCode();
    return this.analyse()?.analysesCategories.find(c => c.categorieCode === code) ?? null;
  });

  // ── Computed : KPIs de la catégorie active ───────────────────────────
  activeKpis = computed(() => {
    const code = this.activeCatCode();
    return (this.analyse()?.analysesKpis ?? []).filter(k => k.categorieCode === code);
  });

  // ── Computed : score global simulé à partir des niveaux ──────────────
  globalScore = computed(() => {
    const kpis = this.analyse()?.analysesKpis ?? [];
    if (!kpis.length) return 0;
    const pts = kpis.reduce((acc, k) => {
      if (k.niveauVariation === 'FAIBLE')   return acc + 100;
      if (k.niveauVariation === 'MODERE')   return acc + 60;
      if (k.niveauVariation === 'CRITIQUE') return acc + 20;
      return acc + 70;
    }, 0);
    return Math.round(pts / kpis.length);
  });

  // ── Computed : stats par catégorie ───────────────────────────────────
  catStats = computed(() => {
    const kpis = this.analyse()?.analysesKpis ?? [];
    return CATEGORIES.map(cat => {
      const items     = kpis.filter(k => k.categorieCode === cat.code);
      const critique  = items.filter(k => k.niveauVariation === 'CRITIQUE').length;
      const modere    = items.filter(k => k.niveauVariation === 'MODERE').length;
      const faible    = items.filter(k => k.niveauVariation === 'FAIBLE').length;
      const withIa    = items.filter(k => k.analyseIa).length;
      const hasContenu = !!(this.analyse()?.analysesCategories ?? []).find(c => c.categorieCode === cat.code)?.contenu;
      return { ...cat, total: items.length, critique, modere, faible, withIa, hasContenu };
    });
  });

  // ── Computed : KPIs critiques (tous) ─────────────────────────────────
  kpisCritiques = computed(() =>
    (this.analyse()?.analysesKpis ?? []).filter(k => k.niveauVariation === 'CRITIQUE')
  );

  // ── Computed : score label ────────────────────────────────────────────
  scoreLabel = computed(() => {
    const s = this.globalScore();
    if (s >= 80) return 'Excellent';
    if (s >= 60) return 'Satisfaisant';
    if (s >= 40) return 'À surveiller';
    return 'Critique';
  });

  scoreColor = computed(() => {
    const s = this.globalScore();
    if (s >= 80) return '#05cd99';
    if (s >= 60) return '#4318FF';
    if (s >= 40) return '#ff9800';
    return '#ee5d50';
  });

  // dash-array for SVG ring (circumference ≈ 339)
  scoreRingDash = computed(() => {
    const pct = this.globalScore() / 100;
    return `${Math.round(pct * 339)} 339`;
  });

  // ── Lifecycle ─────────────────────────────────────────────────────────
  ngOnInit() {
    // importId peut venir de la route (:id) ou être déduit via /resume
    const routeId = this.route.snapshot.paramMap.get('id');
    if (routeId) {
      this.importId.set(+routeId);
      this.loadAnalyse(+routeId);
    } else {
      this.loadResume();
    }
  }

  loadResume() {
    this.dashboardService.getResume().subscribe({
      next: res => {
        this.resume.set(res);
        if (res.dernierImportId) {
          this.importId.set(res.dernierImportId);
          this.loadAnalyse(res.dernierImportId);
        } else {
          this.loading.set(false);
          this.error.set('Aucun import disponible. Veuillez importer des données.');
        }
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Impossible de charger les données.');
      }
    });
  }

  loadAnalyse(importId: number): void {
    this.loading.set(true);
    this.error.set('');
    this.structured.set(null);
    this.analyse.set(null);
    this.loadKanbanState();
    this.sessionState.setActiveImport(importId);

    // Resolve legacy source: cache hit avoids the HTTP round-trip entirely
    const cached = this.sessionState.getDashboardData();
    const legacySource$ = (cached.analysesIa != null)
      ? of(cached.analysesIa)
      : this.http.get<AnalyseCompleteResponse>(`${this.dashboardAnalysteBase}/analyses/${importId}`).pipe(
          catchError(() =>
            this.http.get<AnalyseCompleteResponse>(`${this.dashboardAdminBase}/analyses/${importId}`).pipe(
              catchError(() =>
                this.aiAnalysisService.getAnalyseComplete(importId).pipe(
                  catchError(() => of(null))
                )
              )
            )
          )
        );

    forkJoin({
      structured: this.aiAnalysisService.getStructuredAnalysis(importId).pipe(
        catchError(() => of(null))
      ),
      legacy: legacySource$,
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ structured, legacy }) => {
        if (structured) {
          this.structured.set(structured);
        }
        if (legacy) {
          this.analyse.set(legacy);
          if (cached.analysesIa == null) {
            this.sessionState.patch({ analysesIa: legacy });
          }
          const firstCat = CATEGORIES.find(c =>
            legacy.analysesKpis.some(k => k.categorieCode === c.code)
          );
          if (firstCat) this.activeCatCode.set(firstCat.code);
        } else if (!structured || structured.status === 'FAILED') {
          this.error.set('Aucune analyse IA disponible pour cet import. Lancez une analyse.');
        }
      });
  }

  regenerer() {
    const id = this.importId();
    if (!id || this.regenerating()) return;

    this.regenerating.set(true);
    this.aiAnalysisService.regenerer(id)
      .pipe(finalize(() => this.regenerating.set(false)))
      .subscribe({
        next: data => {
          this.analyse.set(data);
          this.snackBar.open('Analyse IA régénérée avec succès', 'Fermer', { duration: 4000 });
          // Reload structured after regeneration
          this.loadAnalyse(id);
        },
        error: () => this.snackBar.open('Erreur lors de la régénération', 'Fermer', { duration: 4000 })
      });
  }

  // ── UI helpers ────────────────────────────────────────────────────────
  setActiveCat(code: string) { this.activeCatCode.set(code); }

  toggleKpi(id: number) {
    this.expandedKpiId.set(this.expandedKpiId() === id ? null : id);
  }

  isExpanded(id: number): boolean { return this.expandedKpiId() === id; }

  getCatInfo(code: string): CategorieInfo {
    return CATEGORIES.find(c => c.code === code)
      ?? { code, libelle: code, icon: 'label', color: '#a3aed1', bgLight: '#f4f7fe' };
  }

  getNiveauClass(niveau: string | null): string {
    if (niveau === 'CRITIQUE') return 'niveau-critique';
    if (niveau === 'MODERE')   return 'niveau-modere';
    if (niveau === 'FAIBLE')   return 'niveau-faible';
    return 'niveau-default';
  }

  getTendanceIcon(tendance: string | null): string {
    if (tendance === 'HAUSSE') return 'trending_up';
    if (tendance === 'BAISSE') return 'trending_down';
    return 'trending_flat';
  }

  getTendanceClass(tendance: string | null): string {
    if (tendance === 'HAUSSE') return 'icon-red';
    if (tendance === 'BAISSE') return 'icon-green';
    return 'icon-blue';
  }

  formatVariation(v: number): string {
    const sign = v > 0 ? '+' : '';
    return `${sign}${v.toFixed(1)}%`;
  }

  getVariationClass(v: number): string {
    if (v > 10)  return 'var-neg';
    if (v < -10) return 'var-pos';
    return 'var-neutral';
  }

  // Extrait les lignes de méthode 8D depuis analyseIa
  parse8D(analyseIa: string | null): string[] {
    if (!analyseIa) return [];
    const idx = analyseIa.toLowerCase().indexOf('8d');
    if (idx < 0) return [];
    return analyseIa.substring(idx).split(/\n|\./).map(s => s.trim()).filter(s => s.length > 4).slice(0, 8);
  }

  // Extrait les recommandations inline du texte analyseIa
  extractRecos(analyseIa: string | null): string[] {
    if (!analyseIa) return [];
    const lines = analyseIa.split(/\n/).map(s => s.trim()).filter(s => s.length > 10);
    // Lignes qui ressemblent à des actions (commencent par verbe ou tiret)
    return lines.filter(l => /^[-•–*]|^[A-ZÉÈÀÂ]/.test(l)).slice(0, 5);
  }

  // Score ring pour mini rings catégories
  miniRingDash(cat: { critique: number; modere: number; faible: number; total: number }): string {
    if (!cat.total) return '0 339';
    const score = Math.round(
      ((cat.faible * 100 + cat.modere * 60 + (cat.total - cat.faible - cat.modere - cat.critique) * 70 + cat.critique * 20) / cat.total)
    );
    const pct = score / 100;
    return `${Math.round(pct * 220)} 220`;
  }

  miniScore(cat: { critique: number; modere: number; faible: number; total: number }): number {
    if (!cat.total) return 0;
    return Math.round(
      (cat.faible * 100 + cat.modere * 60 + cat.critique * 20 +
       (cat.total - cat.faible - cat.modere - cat.critique) * 70) / cat.total
    );
  }

  get periode(): string {
    const a = this.analyse();
    if (!a) return '';
    return `${a.periodeN1} → ${a.periodeN}`;
  }

  get createdAt(): string {
    const ag = this.analyse()?.analyseGlobale;
    if (!ag?.createdAt) return '';
    return new Date(ag.createdAt).toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  getStructuredFallbackMessage(): string {
    return this.structured()?.fallbackReason || 'Affichage des analyses classiques en fallback.';
  }

  getRecommendationTitle(rec: { title?: string; rationale?: string; expectedBenefit?: string } | null | undefined): string {
    return rec?.title?.trim() || rec?.rationale?.trim() || rec?.expectedBenefit?.trim() || 'Recommandation';
  }

  getRecommendationRationale(rec: { rationale?: string; expectedBenefit?: string } | null | undefined): string {
    return rec?.rationale?.trim() || rec?.expectedBenefit?.trim() || '';
  }

  getRecommendationBenefit(rec: { expectedBenefit?: string } | null | undefined): string {
    return rec?.expectedBenefit?.trim() || '';
  }

  // ── P3.3 — Kanban action board ────────────────────────────────────────
  readonly KANBAN_COLS = ['todo', 'in_progress', 'done'] as const;
  readonly KANBAN_LABELS: Record<string, string> = { todo: 'À Faire', in_progress: 'En Cours', done: 'Terminé' };
  readonly KANBAN_ICONS:  Record<string, string> = { todo: 'assignment', in_progress: 'pending_actions', done: 'task_alt' };

  kanbanState = signal<Record<string, string>>({});

  kanbanItems = computed((): KanbanItem[] => {
    const structured = this.structured();
    if (!structured) return [];
    const items: KanbanItem[] = [];

    (structured.actionPlan ?? []).forEach((item, i) => {
      items.push({ id: `plan-${i}`, action: item.action, priority: item.priority, ownerRole: item.ownerRole, dueHorizon: item.dueHorizon, source: 'action_plan' });
    });

    (structured.kpiInsights ?? []).filter(ins => ins.urgency === 'HIGH' && ins.actionImmediate).forEach((ins, i) => {
      items.push({ id: `kpi-imm-${i}`, action: ins.actionImmediate, priority: 'HIGH', ownerRole: ins.ownerRole, dueHorizon: ins.dueHorizon, source: 'kpi_immediate', kpiRef: ins.kpiName });
    });

    return items;
  });

  hasKanban = computed(() => this.kanbanItems().length > 0);

  itemsInColumn(col: string): KanbanItem[] {
    const state = this.kanbanState();
    return this.kanbanItems().filter(item => (state[item.id] ?? 'todo') === col);
  }

  moveItem(id: string, col: string): void {
    this.kanbanState.update(s => ({ ...s, [id]: col }));
    this.saveKanbanState();
  }

  kanbanPriorityClass(priority: string): string {
    if (priority === 'HIGH' || priority === 'CRITIQUE')   return 'k-prio-high';
    if (priority === 'MEDIUM' || priority === 'MODERE')  return 'k-prio-medium';
    return 'k-prio-low';
  }

  private kanbanStorageKey(): string {
    return `qhse-kanban-${this.importId() ?? 'default'}`;
  }

  private saveKanbanState(): void {
    try { localStorage.setItem(this.kanbanStorageKey(), JSON.stringify(this.kanbanState())); } catch { /* quota */ }
  }

  private loadKanbanState(): void {
    try {
      const raw = localStorage.getItem(this.kanbanStorageKey());
      if (raw) this.kanbanState.set(JSON.parse(raw));
    } catch { /* parse error */ }
  }

  // ── P1.2 — Root cause analysis helpers ────────────────────────────────
  hasRootCauses = computed(() => (this.structured()?.rootCauseAnalysis ?? []).length > 0);

  ishikawaClass(category: string): string {
    const map: Record<string, string> = {
      'Homme':   'ishi-homme',
      'Machine': 'ishi-machine',
      'Méthode': 'ishi-methode',
      'Milieu':  'ishi-milieu',
      'Matière': 'ishi-matiere',
    };
    return map[category] ?? 'ishi-default';
  }

  // ── P1.3 — Predictive alerts helpers ──────────────────────────────────
  hasAlerts = computed(() => (this.structured()?.predictiveAlerts ?? []).length > 0);
  criticalAlertsCount = computed(() =>
    (this.structured()?.predictiveAlerts ?? []).filter(a => a.severity === 'HIGH').length
  );

  alertSeverityClass(severity: string): string {
    if (severity === 'HIGH')   return 'alert-sev-high';
    if (severity === 'MEDIUM') return 'alert-sev-medium';
    return 'alert-sev-low';
  }

  alertSeverityLabel(severity: string): string {
    if (severity === 'HIGH')   return 'Critique';
    if (severity === 'MEDIUM') return 'Modérée';
    return 'Faible';
  }

  alertSeverityIcon(severity: string): string {
    if (severity === 'HIGH')   return 'error';
    if (severity === 'MEDIUM') return 'warning';
    return 'info';
  }

  // ── Enriched KPIs (merge legacy + structured) ────────────────────────
  enrichedKpis = computed((): EnrichedKpi[] => {
    const legacy   = this.analyse()?.analysesKpis ?? [];
    const insights = this.structured()?.kpiInsights ?? [];

    const byId   = new Map<number, AiKpiInsightResponse>();
    const byName = new Map<string, AiKpiInsightResponse>();
    insights.forEach(ins => {
      if (ins.kpiId != null) byId.set(ins.kpiId, ins);
      if (ins.kpiName) byName.set(ins.kpiName.toLowerCase().trim(), ins);
    });

    if (legacy.length > 0) {
      return legacy.map(kpi => {
        const ins = byId.get(kpi.kpiId) ?? byName.get(kpi.kpiNom?.toLowerCase().trim() ?? '');
        return {
          ...kpi,
          insight:           ins?.insight ?? kpi.analyseIa ?? undefined,
          insightConfidence: ins?.confidence,
          structuredCauses:  ins?.probableCauses ?? [],
          structuredRecos:   ins?.recommendations ?? [],
          structuredAction:  ins?.actionImmediate ?? kpi.immediateAction,
          structuredOwner:   ins?.ownerRole,
          structuredDue:     ins?.dueHorizon,
          structuredSuccess: ins?.successMetric,
          structuredRisk:    ins?.riskIfNotDone,
          urgency:           ins?.urgency,
        };
      });
    }

    // Structured-only fallback (no legacy data)
    return insights.map((ins, i): EnrichedKpi => ({
      id: i, kpiId: ins.kpiId ?? i, kpiNom: ins.kpiName, kpiUnite: '',
      categorieCode: '', categorieLibelle: '',
      periodeN1: 0, periodeN: 0, valeurN1: 0, valeurN: 0,
      variationAbsolue: 0, variationRelative: 0,
      niveauVariation: null, tendance: null,
      analyseIa: ins.insight ?? null, createdAt: '',
      insight: ins.insight, insightConfidence: ins.confidence,
      structuredCauses: ins.probableCauses ?? [],
      structuredRecos:  ins.recommendations ?? [],
      structuredAction: ins.actionImmediate,
      structuredOwner:  ins.ownerRole,
      structuredDue:    ins.dueHorizon,
      structuredSuccess: ins.successMetric,
      structuredRisk:   ins.riskIfNotDone,
      urgency:          ins.urgency,
    }));
  });

  filteredKpis = computed(() => {
    const all    = this.enrichedKpis();
    const search = this.searchKpi().toLowerCase().trim();
    const cat    = this.catFilter();
    const niveau = this.niveauKpiFilter();
    return all.filter(k =>
      (!search || k.kpiNom?.toLowerCase().includes(search)) &&
      (!cat    || k.categorieCode === cat) &&
      (!niveau || k.niveauVariation === niveau)
    );
  });

  heroStats = computed(() => {
    const kpis     = this.analyse()?.analysesKpis ?? [];
    const total    = kpis.length || (this.structured()?.kpiInsights?.length ?? 0);
    const critiques = kpis.filter(k => k.niveauVariation === 'CRITIQUE').length;
    const moderes   = kpis.filter(k => k.niveauVariation === 'MODERE').length;
    const faibles   = kpis.filter(k => k.niveauVariation === 'FAIBLE').length;
    const withAi    = kpis.filter(k => k.analyseIa).length
                    + (this.structured()?.kpiInsights?.length ?? 0);
    return { total, critiques, moderes, faibles, withAi: Math.min(withAi, total) };
  });

  get modelLabel(): string {
    const m = this.structured()?.traceability?.modelName ?? '';
    if (m.toLowerCase().includes('gemini')) return 'Gemini';
    if (m.toLowerCase().includes('llama') || m.toLowerCase().includes('qwen') || m.toLowerCase().includes('groq')) return 'Groq';
    if (m) return m;
    return 'IA';
  }

  globalSummaryText(): string {
    return this.structured()?.globalSummary?.trim()
        || this.analyse()?.analyseGlobale?.synthese?.trim()
        || '';
  }

  probableCausesList(): string[] {
    const s = this.structured()?.probableCauses ?? [];
    if (s.length) return s;
    return this.planActionsList().slice(0, 5);
  }

  // ── Filter setters ───────────────────────────────────────────────────
  setCatFilter(code: string)   { this.catFilter.set(code); }
  setNiveauFilter(n: string)   { this.niveauKpiFilter.set(n); }
  setSearch(v: string)         { this.searchKpi.set(v); }

  catCountAll(code: string): number {
    return this.enrichedKpis().filter(k => k.categorieCode === code).length;
  }
}
