import { NiveauVariation } from '../enums/niveau-variation.enum';
import { Tendance } from '../enums/tendance.enum';
import { UniteKpi } from '../enums/unite-kpi.enum';

export interface ResultatKpiResponse {
  id: number;
  kpiId: number;
  kpiNom: string;
  kpiUnite: UniteKpi;
  categorieCode: string;
  categorieLibelle: string;
  periodeN1: number;
  periodeN: number;
  valeurN1?: number;
  valeurN?: number;
  variationAbsolue?: number;
  variationRelative?: number;
  niveauVariation?: NiveauVariation;
  tendance?: Tendance;
  analyseIa?: string;
  createdAt?: string;
}
