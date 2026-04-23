import { UniteKpi } from '../enums/unite-kpi.enum';

export interface UpdateKpiRequest {
  nom?: string;
  definition?: string;
  unite?: UniteKpi;
  categorieCode?: string;
  seuilFaible?: number;
  seuilModere?: number;
  seuilCritique?: number;
  ordre?: number;
}
