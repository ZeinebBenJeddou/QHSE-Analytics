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
  classification: 'EXCELLENT' | 'FAIBLE' | 'MODERE' | 'PRE_ESCALADE' | 'CRITIQUE' | 'INDETERMINE' | 'UNKNOWN';
  tendance: 'HAUSSE' | 'BAISSE' | 'STABLE' | null;
  matchedKpi?: string | null;
  matchedKpiId?: number | null;
  matchConfidence?: number | null;       
  
  businessClassification?: 'CRITICAL' | 'WARNING' | 'OK';
  isAnomaly?: boolean;
 
  direction?: 'HIGHER_IS_BETTER' | 'LOWER_IS_BETTER' | 'TARGET_IS_BEST';
  calcConfidence?: number; 
  classificationReason?: string;
  reviewRequired?: boolean;
  dataFlags?: string[];
  
  spcMean?: number | null;
  spcStd?: number | null;
  spcUcl?: number | null;
  spcLcl?: number | null;
  spcOutOfControl?: boolean | null;
  
  riskProbability?: number | null;
  riskImpact?: number | null;
  riskScore?: number | null;
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | null;
}


export interface ColumnProfileDTO {
  columnIndex: number;
  detectedHeader: string;
  inferredType: 'TEXT' | 'NUMERIC' | 'BOOLEAN' | 'DATE' | 'MIXED';
  sampleValues: string[];
  totalRows: number;
  nullCount: number;
  uniqueCount: number;
  numericMin?: number | null;
  numericMax?: number | null;
  numericMean?: number | null;
  likelySemantic: 'KPI_NAME' | 'VALUE_N' | 'VALUE_N1' | 'CATEGORY' | 'UNIT' | 'UNKNOWN';
  semanticConfidence: number; // 0–1
}

export interface CategoryScoreDTO {
  categoryCode: string;
  categoryLibelle: string;
  kpiCount: number;
  excellentCount: number;
  faibleCount: number;
  preEscaladeCount: number;
  modereCount: number;
  critiqueCount: number;
  indetermineCount: number;
  compositeScore: number;   
  compositeLabel: string;   
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
  totalRows: number;
  validRows: number;
  invalidRows: number;
  warningRows: number;
  duplicateRows: number;
  outlierRows: number;
  qualityScore: number;
  blocking: boolean;
  errors: ImportIssue[];
  warnings: ImportIssue[];
  infos: ImportIssue[];
  extractionIssues?: ImportIssue[];
  importMode?: 'STRICT' | 'PARTIAL';
  importedRowsCount?: number;
  rejectedRowsCount?: number;
  rejectedRowIndexes?: number[];
  rejectedReasons?: RejectedReasonSummary[];
  hardBlocking?: boolean;
  softBlocking?: boolean;
  blockingReason?: string;
}


export interface DualFileProfileResponse {
  columnsN1: ColumnProfileDTO[];
  columnsN: ColumnProfileDTO[];
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
  risks?: KpiCalculatedDTO[];
  riskScore?: number;
  qualityReport?: ImportQualityReport;
  categoryScores?: CategoryScoreDTO[];
}

