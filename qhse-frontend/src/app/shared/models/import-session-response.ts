import { ImportMode } from '../enums/import-mode.enum';
import { ImportStatus } from '../enums/import-status.enum';

export interface ImportSessionResponse {
  id: number;
  mode: ImportMode;
  nomFichier: string;
  periodeN1: number;
  periodeN: number;
  statut: ImportStatus;
  messageErreur?: string;
  createdAt?: string;
  updatedAt?: string;
  nombreTotal: number;
  nombreOk: number;
  nombreCorrige: number;
  nombreManquant: number;
  nombreInvalide: number;
  nombreSuspect: number;
}
