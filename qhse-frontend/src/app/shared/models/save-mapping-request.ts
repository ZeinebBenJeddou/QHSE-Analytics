import { ColonneMappingRequest } from './colonne-mapping-request';

export interface SaveMappingRequest {
  nom: string;
  ligneEntete: number;
  colonnes: ColonneMappingRequest[];
}
