import { TypeValeur } from '../enums/type-valeur.enum';

export interface ColonneMappingRequest {
  nomColonne: string;
  indexColonne: number;
  kpiId?: number;
  typeValeur: TypeValeur;
}
