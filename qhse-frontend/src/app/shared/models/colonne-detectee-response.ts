import { KpiResponse } from './kpi-response';

export interface ColonneDetecteeResponse {
  nomColonne: string;
  indexColonne: number;
  kpiSuggere?: KpiResponse;
  scoreSimilarite?: number;
  noteSuggestion?: string;
}
