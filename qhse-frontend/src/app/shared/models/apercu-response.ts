import { ImportSessionResponse } from './import-session-response';
import { StagingDonneeResponse } from './staging-donnee-response';

export interface ApercuResponse {
  importSession: ImportSessionResponse;
  donnees: StagingDonneeResponse[];
  peutConfirmer: boolean;
}
