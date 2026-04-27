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
  analyseIa?: string;
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
