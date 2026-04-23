import { UniteKpi } from '../enums/unite-kpi.enum';

export interface KpiResponse {
  id: number;
  nom: string;
  definition: string;
  unite: UniteKpi;
  categorieCode: string;
  categorieLibelle: string;
  seuilFaible: number;
  seuilModere: number;
  seuilCritique: number;
  ordre: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}
