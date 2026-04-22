export interface Kpi {
  id: number;
  nom: string;
  definition: string;
  unite: 'POURCENTAGE' | 'NOMBRE' | 'KWH' | 'KG';
  categorieCode: string;
  categorieLibelle?: string;
  seuilFaible: number;
  seuilModere: number;
  seuilCritique: number;
  ordre: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CategorieKpi {
  id: number;
  code: string;
  libelle: string;
  description?: string;
  nombreKpisActifs: number;
}

export interface CreateKpiRequest {
  nom: string;
  definition: string;
  unite: 'POURCENTAGE' | 'NOMBRE' | 'KWH' | 'KG';
  categorieCode: string;
  seuilFaible: number;
  seuilModere: number;
  seuilCritique: number;
  ordre: number;
}

export interface UpdateKpiRequest {
  nom?: string;
  definition?: string;
  unite?: 'POURCENTAGE' | 'NOMBRE' | 'KWH' | 'KG';
  categorieCode?: string;
  seuilFaible?: number;
  seuilModere?: number;
  seuilCritique?: number;
  ordre?: number;
}

export interface KpiDeleteResponse {
  message: string;
  deleted: boolean;
}
