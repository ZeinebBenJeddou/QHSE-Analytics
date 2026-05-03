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

  // Legacy AI note (from AnalysisAgent/Groq)
  analyseIa?: string;

  // ── Gemini Deep Analysis ──────────────────────────────────────────────
  riskLevel?: 'Faible' | 'Modéré' | 'Élevé';
  riskJustification?: string;
  objectiveReached?: boolean;
  improvementDetected?: boolean;
  issueDetected?: string;
  correctiveAction?: string;
  preventiveAction?: string;
  immediateAction?: string;
  immediatePriority?: 'Haute' | 'Moyenne' | 'Basse';
  requires8d?: boolean;
  eightDDetails?: string;   // JSON string with D1–D8
  aiNote?: string;          // Final business note in French
}

export interface ComparatifTableauResponse {
  importId: number;
  periodeN1: number;
  periodeN: number;
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
