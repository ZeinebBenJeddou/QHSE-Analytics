export interface ImportSessionResponse {
  id: number;
  nomFichier: string;
  periodeN1: number;
  periodeN: number;
  statut: 'EN_TRAITEMENT' | 'TRAITE' | 'ERREUR' | 'ANNULE';
  messageErreur?: string;
  mode: 'AUTO' | 'MANUEL';
  createdAt: string;
  updatedAt: string;
}

export interface AutoImportResultResponse {
  session: ImportSessionResponse;
  nombreKpisImportes: number;
  nombreKpisRejetes: number;
  resultats: ResultatKpiResponse[];
}

export interface ResultatKpiResponse {
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
  confidenceScore?: number;
  qualityStatus?: string;
  createdAt: string;
}

export interface ResultatGlobalResponse {
  importSessionId: number;
  periodeN1: number;
  periodeN: number;
  resultats: ResultatKpiResponse[];
}

export interface HistoriqueItemResponse {
  importId: number;
  nomFichier: string;
  periodeN1: number;
  periodeN: number;
  statut: string;
  nombreKpisCritiques: number;
  dateImport: string;
}

export interface HistoriqueAnalysteResponse {
  items: HistoriqueItemResponse[];
  totalImports: number;
  totalTraites: number;
  totalErreurs: number;
}
