import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { vi } from 'vitest';

import { AnalyseIAComponent } from './analyse-ia.component';
import { AiAnalysisService } from '../../../../core/services/ai-analysis.service';
import { DashboardService } from '../../../../core/services/dashboard.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AnalyseCompleteResponse, AiAnalysisStructuredResponse } from '../../../../core/models/analyse-ia.model';

describe('AnalyseIAComponent', () => {
  let fixture: ComponentFixture<AnalyseIAComponent>;
  let component: AnalyseIAComponent;
  let aiAnalysisService: any;
  let dashboardService: any;
  let httpClient: any;

  const legacyResponse: AnalyseCompleteResponse = {
    importSessionId: 42,
    periodeN1: 2024,
    periodeN: 2025,
    analysesKpis: [
      {
        id: 1,
        kpiId: 101,
        kpiNom: 'Heures perdues',
        kpiUnite: 'h',
        categorieCode: 'Q',
        categorieLibelle: 'Qualité',
        periodeN1: 10,
        periodeN: 12,
        valeurN1: 10,
        valeurN: 12,
        variationAbsolue: 2,
        variationRelative: 20,
        niveauVariation: 'CRITIQUE',
        tendance: 'HAUSSE',
        analyseIa: 'Analyse legacy',
        createdAt: '2026-05-07T10:00:00Z',
      },
      {
        id: 2,
        kpiId: 102,
        kpiNom: 'Papiers',
        kpiUnite: 'u',
        categorieCode: 'H',
        categorieLibelle: 'Hygiène',
        periodeN1: 5,
        periodeN: 4,
        valeurN1: 5,
        valeurN: 4,
        variationAbsolue: -1,
        variationRelative: -20,
        niveauVariation: 'MODERE',
        tendance: 'BAISSE',
        analyseIa: null,
        createdAt: '2026-05-07T10:00:00Z',
      },
    ],
    analysesCategories: [
      {
        id: 11,
        importSessionId: 42,
        categorieCode: 'Q',
        categorieLibelle: 'Qualité',
        contenu: 'Contenu catégorie',
        createdAt: '2026-05-07T10:00:00Z',
      },
    ],
    analyseGlobale: {
      id: 99,
      importSessionId: 42,
      synthese: 'Synthèse classique',
      planActions: 'Action 1\nAction 2',
      createdAt: '2026-05-07T10:00:00Z',
    },
  };

  const structuredSuccess: AiAnalysisStructuredResponse = {
    status: 'SUCCESS',
    globalSummary: 'Synthèse structurée',
    confidence: {
      overall: 88,
      sections: { summary: 90, probableCauses: 80, recommendations: 85, actionPlan: 87 },
    },
    kpiInsights: [
      {
        kpiId: 101,
        kpiName: 'Heures perdues',
        confidence: 91,
        insight: 'Risque confirmé',
        probableCauses: ['Cause 1'],
        recommendations: ['Revoir le planning'],
        actionImmediate: 'Agir immédiatement',
        urgency: 'Haute',
        ownerRole: 'Responsable QHSE',
        dueHorizon: '7 jours',
        successMetric: 'Baisse de 10%',
        riskIfNotDone: 'Surcoût',
      },
      {
        kpiId: 102,
        kpiName: 'Papiers',
        confidence: 82,
        insight: 'Alerte modérée',
        probableCauses: [],
        recommendations: [],
        actionImmediate: 'Surveiller',
        urgency: 'Moyenne',
        ownerRole: 'Responsable site',
        dueHorizon: '30 jours',
        successMetric: 'Stabilisation',
        riskIfNotDone: 'Dégradation',
      },
    ],
    probableCauses: ['Cause globale'],
    recommendations: [
      {
        title: 'Revoir le plan',
        rationale: 'La recommandation doit être traçable',
        expectedBenefit: 'Réduction du risque',
        urgency: 'Haute',
      },
    ],
    actionPlan: [
      {
        action: 'Appliquer le plan',
        priority: 'Haute',
        ownerRole: 'Responsable QHSE',
        dueHorizon: '15 jours',
        successMetric: 'Mise en œuvre',
        riskIfNotDone: 'Retard',
      },
    ],
    traceability: {
      modelName: 'groq',
      generatedAt: '2026-05-07T10:10:00Z',
      contextSourcesUsed: [
        { sourceName: 'source 1', relevanceScore: 80 },
      ],
      schemaVersion: '1.1',
      promptVersion: 'structured-qhse-v4',
      importSessionId: 42,
    },
    schemaVersion: '1.1',
    promptVersion: 'structured-qhse-v4',
    importSessionId: 42,
  };

  const structuredPartial: AiAnalysisStructuredResponse = {
    ...structuredSuccess,
    status: 'PARTIAL',
    fallbackReason: 'Couverture KPI insuffisante après retry.',
    kpiInsights: [structuredSuccess.kpiInsights[0]],
  };

  beforeEach(async () => {
    aiAnalysisService = {
      getStructuredAnalysis: vi.fn(),
      getAnalyseComplete: vi.fn(),
      regenerer: vi.fn(),
      runAi: vi.fn(),
    };
    dashboardService = { getResume: vi.fn() };
    httpClient = { get: vi.fn() };

    dashboardService.getResume.mockReturnValue(of({ dernierImportId: 42 } as any));
    aiAnalysisService.getAnalyseComplete.mockReturnValue(of(legacyResponse));
    aiAnalysisService.regenerer.mockReturnValue(of(legacyResponse as any));
    aiAnalysisService.runAi.mockReturnValue(of(legacyResponse as any));
    httpClient.get.mockReturnValue(throwError(() => new Error('legacy endpoint unavailable')));

    await TestBed.configureTestingModule({
      imports: [AnalyseIAComponent],
      providers: [
        { provide: AiAnalysisService, useValue: aiAnalysisService },
        { provide: DashboardService, useValue: dashboardService },
        { provide: HttpClient, useValue: httpClient },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? '42' : null),
              },
            },
          },
        },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();
  });

  function createComponent(structuredResponse: AiAnalysisStructuredResponse) {
    aiAnalysisService.getStructuredAnalysis.mockReturnValue(of(structuredResponse));
    fixture = TestBed.createComponent(AnalyseIAComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    return fixture.whenStable().then(() => {
      fixture.detectChanges();
      return fixture;
    });
  }

  it('renders the structured analysis with full coverage', async () => {
    await createComponent(structuredSuccess);

    expect(component.isStructuredSuccess()).toBe(true);
    expect(component.structuredCoverageLabel()).toBe('2/2');
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Synthèse structurée');
    expect(text).toContain('Couverture KPI: 2/2');
    expect(text).toContain('Version prompt: structured-qhse-v4');
  });

  it('shows PARTIAL banner and legacy fallback when structured coverage is incomplete', async () => {
    await createComponent(structuredPartial);

    expect(component.isStructuredPartial()).toBe(true);
    expect(component.showLegacyFallback()).toBe(true);
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Analyse IA structurée partielle');
    expect(text).toContain('PARTIAL');
    expect(text).toContain('Analyse par Catégorie');
  });

  it('renders defensively when structured sections are missing', async () => {
    const missingSections = {
      ...structuredSuccess,
      recommendations: null,
      actionPlan: null,
      probableCauses: null,
      kpiInsights: null,
      traceability: {
        ...structuredSuccess.traceability,
        generatedAt: '2026-05-07T10:10:00Z',
      },
    } as any;

    await createComponent(missingSections);

    expect(component.isStructuredSuccess()).toBe(true);
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Synthèse structurée');
    expect(text).toContain('Couverture KPI: 0/2');
  });
});
