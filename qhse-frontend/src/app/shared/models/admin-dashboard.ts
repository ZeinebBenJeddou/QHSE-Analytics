export interface AdminStatsResponse {
  totalAnalyses: number;
  totalKpisCritiques: number;
  nombreAnalystesActifs: number;
  nombreAdmins: number;
}

export interface BarreGroupeeData {
  categorie: string;
  valeurMoyenneN1: number;
  valeurMoyenneN: number;
  variationMoyenne: number;
}

export interface AdminGraphiquesDataResponse {
  importsEvolution: any; // Chart.js data format
  repartitionCategories: any; // Chart.js data format
  kpiParAnalyste: any; // Chart.js data format
  performanceMensuelle: any; // Chart.js data format
}

export interface RepartitionCategorieItem {
  categorieCode: string;
  categorieLibelle: string;
  nombreFaibles: number;
  nombreModeres: number;
  nombreCritiques: number;
  total: number;
}

export interface AdminRepartitionResponse {
  categories: RepartitionCategorieItem[];
}

export interface AdminAnalysteItemResponse {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  kpisCount: number;
  lastActivity: string;
}

export interface AdminKpiCritiqueResponse {
  nom: string;
  valeur: number;
  unite: string;
  niveauCriticite: string;
}