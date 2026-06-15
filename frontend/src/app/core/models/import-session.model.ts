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
  classification: 'FAIBLE' | 'MODERE' | 'CRITIQUE' | 'INDETERMINE' | 'UNKNOWN';
  tendance: 'HAUSSE' | 'BAISSE' | 'STABLE' | null;
  matchedKpi?: string | null;
  matchedKpiId?: number | null;
  matchingType?: 'EXACT' | 'INCLUSION' | 'JARO_WINKLER' | 'NON_RECONNU' | null;
  matchConfidence?: number | null;       
  
  direction?: 'HIGHER_IS_BETTER' | 'LOWER_IS_BETTER';
  calcConfidence?: number; 
  classificationReason?: string;
  reviewRequired?: boolean;
  aiEnriched?: boolean;
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
  likelySemantic: 'KPI_NAME' | 'VALUE_N' | 'VALUE_N1' | 'CATEGORY' | 'UNIT' | 'UNKNOWN';
  confidenceScore: number;
  warningMessage?: string;
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
  exactDuplicateRows?: number;
  conflictDuplicateRows?: number;
  outlierRows: number;
  uppercaseConvertedRows?: number;
  nullValuesCount?: number;
  nullValuesDetail?: string;
  resultingRowsCount?: number;
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
  issuesTruncated?: boolean;
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

