import { ResultatKpiResponse } from './resultat-kpi-response';

export interface ResultatGlobalResponse {
  importSessionId: number;
  periodeN1: number;
  periodeN: number;
  resultats: ResultatKpiResponse[];
  nombreFaible: number;
  nombreModere: number;
  nombreCritique: number;
  analyseGlobaleIa?: string;
}
