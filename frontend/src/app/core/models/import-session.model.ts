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
  // Nouveaux champs qualité
  originalKpiName?: string;
  normalizedKpiName?: string;
  rowQualityScore?: number;
  issues?: ImportIssue[];
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

// ── Quality Report ────────────────────────────────────────────────────────────

export type IssueSeverity = 'ERROR' | 'WARNING' | 'INFO';

export interface ImportIssue {
  rowIndex: number | null;
  column: string | null;
  code: string;
  severity: IssueSeverity;
  message: string;
  originalValue: string | null;
  cleanedValue: string | null;
}

export interface RejectedReasonSummary {
  code: string;
  count: number;
}

export interface ImportQualityReport {
  // Metrics qualité unifiées
  // totalRows = lignes lues dans le fichier, doublons inclus
  totalRows: number;
  validRows: number;
  invalidRows: number;
  warningRows: number;
  // duplicateRows = occurrences de doublons écartées
  duplicateRows: number;
  outlierRows: number;
  qualityScore: number;
  blocking: boolean;
  errors: ImportIssue[];
  warnings: ImportIssue[];
  infos: ImportIssue[];
  extractionIssues?: ImportIssue[];
  // New partial import fields
  importMode?: 'STRICT' | 'PARTIAL';
  importedRowsCount?: number;
  rejectedRowsCount?: number;
  rejectedRowIndexes?: number[];
  rejectedReasons?: RejectedReasonSummary[];
  hardBlocking?: boolean;
  softBlocking?: boolean;
  blockingReason?: string;
}

// ── Import Processing Response ────────────────────────────────────────────────

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
  // Quality Report
  qualityReport?: ImportQualityReport;
}

