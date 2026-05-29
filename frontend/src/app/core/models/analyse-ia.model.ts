export interface ResultatKpiIaResponse {
  id: number;
  kpiId: number;
  kpiNom: string;
  kpiUnite: string;
  categorieCode: string;
  categorieLibelle: string;
  periodeN1: number;
  periodeN: number;
  valeurN1: number;
  valeurN: number;
  variationAbsolue: number;
  variationRelative: number;
  niveauVariation: 'FAIBLE' | 'MODERE' | 'CRITIQUE' | 'INDETERMINE' | null;
  tendance: 'HAUSSE' | 'BAISSE' | 'STABLE' | null;
  analyseIa: string | null;
  createdAt: string;
}

export interface AnalyseCategorieResponse {
  id: number;
  importSessionId: number;
  categorieCode: string;
  categorieLibelle: string;
  contenu: string;
  createdAt: string;
}

export interface AnalyseGlobaleResponse {
  id: number;
  importSessionId: number;
  synthese: string;
  planActions: string;
  createdAt: string;
}

export interface AnalyseCompleteResponse {
  importSessionId: number;
  periodeN1: number;
  periodeN: number;
  analysesKpis: ResultatKpiIaResponse[];
  analysesCategories: AnalyseCategorieResponse[];
  analyseGlobale?: AnalyseGlobaleResponse;
}

export interface MappingConfigRequest {
  excelColumn: string;
  kpiId: number;
  categorieCode?: string;
}

export interface MappingTemplateRequest {
  templateName: string;
  mappings: MappingConfigRequest[];
}

export interface MappingTemplateResponse {
  id: number;
  templateName: string;
  mappings: MappingConfigRequest[];
  createdAt: string;
}

export interface AiConfidenceResponse {
  overall: number;
  sections: { [key: string]: number };
}

export interface AiTraceabilityResponse {
  modelName: string;
  generatedAt: string;
  contextSourcesUsed: AiContextSourceResponse[];
  schemaVersion?: string;
  promptVersion?: string;
  importSessionId?: number | null;
}

export interface AiContextSourceResponse {
  sourceName: string;
  relevanceScore: number;
}

export interface AiKpiInsightResponse {
  kpiId?: number;
  kpiName: string;
  confidence: number;
  insight: string;
  probableCauses: string[];
  recommendations: string[];
  actionImmediate: string;
  urgency: string;
  ownerRole: string;
  dueHorizon: string;
  successMetric: string;
  riskIfNotDone: string;
  note?: string;
}

export interface AiRecommendationResponse {
  title: string;
  rationale: string;
  expectedBenefit: string;
  urgency: string;
}

export interface AiActionPlanItemResponse {
  action: string;
  priority: string;
  ownerRole: string;
  dueHorizon: string;
  successMetric: string;
  riskIfNotDone: string;
}

export interface AiAnalysisStructuredResponse {
  status: 'SUCCESS' | 'PARTIAL' | 'FAILED';
  fallbackReason?: string;
  globalSummary: string;
  confidence: AiConfidenceResponse;
  kpiInsights: AiKpiInsightResponse[];
  probableCauses: string[];
  recommendations: AiRecommendationResponse[];
  actionPlan: AiActionPlanItemResponse[];
  traceability: AiTraceabilityResponse;
  schemaVersion?: string;
  promptVersion?: string;
  importSessionId?: number | null;
}
