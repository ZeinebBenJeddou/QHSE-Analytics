import { ResultatKpiResponse } from './resultat-kpi-response';

export interface AnalyseCompleteResponse {
  importSessionId: number;
  periodeN1: number;
  periodeN: number;
  analysesKpis: ResultatKpiResponse[];
  analysesCategories: AnalyseCategorieResponse[];
  analyseGlobale: AnalyseGlobaleResponse;
}

export interface AnalyseCategorieResponse {
  id: number;
  importSessionId: number;
  categorieCode: string;
  categorieLibelle: string;
  contenu: string;
  createdAt: string;
}

export interface AnalyseGlobaleResponse {
  id: number;
  importSessionId: number;
  synthese: string;
  planActions: string;
  createdAt: string;
}