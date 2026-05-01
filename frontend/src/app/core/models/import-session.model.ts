export interface ImportSessionResponse {
  id: number;
  nomFichier: string;
  periodeN1: number;
  periodeN: number;
  statut: 'EN_TRAITEMENT' | 'TRAITE' | 'ERREUR' | 'ANNULE' | 'IMPORTED' | 'CALCULATED' | 'READY_FOR_AI';
  messageErreur?: string;
  mode: 'AUTO' | 'MANUEL' | 'TEMPLATE' | 'FLEXIBLE';
  createdAt: string;
  updatedAt: string;
}

export interface HistoriqueItemResponse {
  importId: number;
  nomFichier: string;
  periodeN1: number;
  periodeN: number;
  statut: string;
  messageErreur?: string;
  nombreKpisCritiques: number;
  dateImport: string;
  utilisateurId?: number;
  utilisateurNom?: string;
  utilisateurEmail?: string;
}

export interface HistoriqueAnalysteResponse {
  items: HistoriqueItemResponse[];
  totalImports: number;
  totalTraites: number;
  totalErreurs: number;
}

export interface KpiCalculatedDTO {
  rowIndex: number;
  kpiName: string;
  categorie: string;
  categorieCode?: string;
  unite?: string;
  valeurN1: number;
  valeurN: number;
  seuilFaible?: number;
  seuilModere?: number;
  seuilCritique?: number;
  definition?: string;
  variationAbsolute?: number;
  variationPercentage: number;
  classification: 'FAIBLE' | 'MODERE' | 'CRITIQUE' | 'UNKNOWN';
  tendance: 'HAUSSE' | 'BAISSE' | 'STABLE' | null;
  matchedKpi?: string | null;
  matchedKpiId?: number | null;
  // Business Intelligence Enrichment Fields
  businessClassification?: 'CRITICAL' | 'WARNING' | 'OK';
  isAnomaly?: boolean;
}

export interface KpiRawDataDTO {
  rowIndex: number;
  kpiName: string;
  categorie: string;
  unite?: string;
  valeurN1?: number;
  valeurN?: number;
  valeurN1Raw?: string;
  valeurNRaw?: string;
  valid: boolean;
  validationMessage?: string;
  methodeExtraction?: string;
  scoreConfiance: number;
}

export interface ChartSeries {
  label: string;
  categories: string[];
  values: number[];
}

export interface ChartResponseDTO {
  barCharts: ChartSeries[];
  lineCharts: ChartSeries[];
  comparisonTable?: unknown[];
}

export interface ImportProcessingResponse {
  importSessionId?: number;
  calculatedData?: KpiCalculatedDTO[];
  rawData?: KpiRawDataDTO[];
  extractionMethod?: string;
  qualityScore?: number;
  detectedHeaders?: string[];
  charts?: ChartResponseDTO;
  analyseIa?: string;
  // Risk Intelligence Fields
  risks?: KpiCalculatedDTO[];
  riskScore?: number;
}
