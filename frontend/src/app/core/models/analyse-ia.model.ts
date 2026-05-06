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
  niveauVariation: 'FAIBLE' | 'MODERE' | 'CRITIQUE';
  tendance: 'HAUSSE' | 'BAISSE' | 'STABLE';
  analyseIa?: string;
  riskLevel?: 'Faible' | 'Modéré' | 'Élevé';
  riskJustification?: string;
  issueDetected?: string;
  correctiveAction?: string;
  preventiveAction?: string;
  immediateAction?: string;
  immediatePriority?: 'Haute' | 'Moyenne' | 'Basse';
  requires8d?: boolean;
  eightDDetails?: string;
  aiNote?: string;
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
