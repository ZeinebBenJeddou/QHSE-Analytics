import { UniteKpi } from '../enums/unite-kpi.enum';

export interface CreateKpiRequest {
  nom: string;
  definition: string;
  unite: UniteKpi;
  categorieCode: string;
  seuilFaible: number;
  seuilModere: number;
  seuilCritique: number;
  ordre: number;
}
