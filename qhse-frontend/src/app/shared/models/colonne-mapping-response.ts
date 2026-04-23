import { TypeValeur } from '../enums/type-valeur.enum';

export interface ColonneMappingResponse {
  nomColonne: string;
  indexColonne: number;
  kpiId?: number;
  typeValeur: TypeValeur;
}
