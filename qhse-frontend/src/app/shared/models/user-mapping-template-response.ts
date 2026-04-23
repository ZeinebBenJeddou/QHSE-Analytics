import { ColonneMappingResponse } from './colonne-mapping-response';

export interface UserMappingTemplateResponse {
  id: number;
  nom: string;
  ligneEntete: number;
  createdAt?: string;
  colonnes: ColonneMappingResponse[];
}
