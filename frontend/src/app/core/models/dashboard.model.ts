export interface ResumeCategorieResponse {
  categorieCode: string;
  categorieLibelle: string;
  variationMoyenne: number;
  nombreKpisCritiques: number;
  nombreKpisModeres: number;
  nombreKpisFaibles: number;
  couleur: 'VERT' | 'ORANGE' | 'ROUGE';
}

export interface ResumeAnalysteResponse {
  dernierImportId: number;
  periodeN1: number;
  periodeN: number;
  dateAnalyse: string;
  resumeCategories: ResumeCategorieResponse[];
  nombreTotalKpis: number;
  nombreTotalCritiques: number;
  nombreTotalPreEscalades?: number;
  nombreTotalModeres: number;
  nombreTotalFaibles: number;
  nombreTotalExcellents?: number;
  nombreTotalIndetermines?: number;
  messageErreurIa?: string;
}

export interface LigneComparatifResponse {
  kpiId: number;
  kpiNom: string;
  unite: string;
  categorieCode: string;
  categorieLibelle: string;
  valeurN1: number;
  valeurN: number;
  variationAbsolue: number;
  variationRelative: number;
  niveauVariation: 'FAIBLE' | 'MODERE' | 'CRITIQUE' | 'PRE_ESCALADE' | 'EXCELLENT' | 'INDETERMINE' | null;
  tendance: 'HAUSSE' | 'BAISSE' | 'STABLE' | null;
  status?: string;
  commentaire?: string;

  analyseIa?: string;

  riskLevel?: 'Faible' | 'Modéré' | 'Élevé';
  riskJustification?: string;
  identificationRisque?: string;
  objectiveReached?: boolean;
  improvementDetected?: boolean;
  issueDetected?: string;
  problemeDetecte?: string;
  correctiveAction?: string;
  preventiveAction?: string;
  actionsPreventives?: string;
  immediateAction?: string;
  actionImmediate?: string;
  immediatePriority?: 'Haute' | 'Moyenne' | 'Basse';
  prioriteAction?: 'Haute' | 'Moyenne' | 'Basse';
  requires8d?: boolean;
  eightDDetails?: string;   
  methode8D?: string;       
  aiNote?: string;          
  noteFinale?: string;
}

export interface ComparatifTableauResponse {
  importId: number;
  periodeN1: number;
  periodeN: number;
  dateAnalyse?: string;
  lignes: LigneComparatifResponse[];
  nombreCritiques: number;
  nombreModeres: number;
  nombreFaibles: number;
}

export interface BarreGroupeeData {
  categorie: string;
  valeurMoyenneN1: number;
  valeurMoyenneN: number;
  variationMoyenne: number;
}

export interface RadarPoint {
  categorie: string;
  score: number;
}

export interface KpiDegrade {
  kpiNom: string;
  categorieCode: string;
  variationRelative: number;
  niveauVariation: string;
  tendance: string;
}

export interface GraphiquesDataResponse {
  barresGroupees: BarreGroupeeData[];
  radarData: RadarPoint[];
  topKpisDegrades: KpiDegrade[];
}

export interface AlerteKpiItemResponse {
  kpiId: number;
  kpiNom: string;
  categorieCode: string;
  categorieLibelle: string;
  variationRelative: number;
  variationAbsolue: number;
  valeurN: number;
  valeurN1: number;
  niveauVariation: string;
  tendance: string | null;
}

export interface AlertesResponse {
  importSessionId: number | null;
  periodeN1: number | null;
  periodeN: number | null;
  count: number;
  alertes: AlerteKpiItemResponse[];
}
