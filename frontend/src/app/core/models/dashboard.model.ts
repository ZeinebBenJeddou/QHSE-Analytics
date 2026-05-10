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
  nombreTotalModeres: number;
  nombreTotalFaibles: number;
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
  niveauVariation: 'FAIBLE' | 'MODERE' | 'CRITIQUE';
  tendance: 'HAUSSE' | 'BAISSE' | 'STABLE';
  status?: string;
  commentaire?: string;

  // Legacy AI note (Groq primary, Gemini fallback in the backend)
  analyseIa?: string;

  // ── Deep AI Analysis ─────────────────────────────────────────────────
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
  eightDDetails?: string;   // JSON string with D1–D8
  methode8D?: string;       // JSON string with D1–D8
  aiNote?: string;          // Final business note in French
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
