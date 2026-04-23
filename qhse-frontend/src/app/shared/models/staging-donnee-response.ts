import { StatutNettoyage } from '../enums/statut-nettoyage.enum';

export interface StagingDonneeResponse {
  id: number;
  kpiId: number;
  kpiNom: string;
  kpiUnite: string;
  categorieCode: string;
  valeurBruteN1: string;
  valeurBruteN: string;
  valeurN1?: number;
  valeurN?: number;
  statutNettoyage: StatutNettoyage;
  noteNettoyage?: string;
}
