export type UserRole = 'ADMIN' | 'ANALYSTE';
export type ImportMode = 'TEMPLATE_OFFICIEL' | 'FICHIER_LIBRE';
export type ImportStatut = 'EN_ATTENTE' | 'EN_TRAITEMENT' | 'TRAITE' | 'ERREUR';
export type StatutNettoyage = 'OK' | 'CORRIGE' | 'MANQUANT' | 'INVALIDE' | 'SUSPECT' | 'IGNORE';

export interface MessageResponse {
  message: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  email: string;
  nom: string;
  prenom: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nom: string;
  prenom: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface VerifyOtpRequest {
  email: string;
  code: string;
  rememberMe?: boolean;
}

export interface ResetPasswordRequest {
  token: string;
  password: string;
  confirmPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface UserResponse {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: UserRole;
  verified: boolean;
  active: boolean;
  createdAt?: string;
  systemAdmin?: boolean;
}

export interface UserListResponse {
  users: UserResponse[];
  totalAdmins: number;
  totalAnalystes: number;
  totalActifs: number;
  totalInactifs: number;
}

export interface CategorieKpiResponse {
  id: number;
  code: string;
  libelle: string;
  description?: string;
  nombreKpisActifs: number;
}

export interface KpiResponse {
  id: number;
  nom: string;
  definition?: string;
  unite?: string;
  categorieCode?: string;
  categorieLibelle?: string;
  seuilFaible?: number;
  seuilModere?: number;
  seuilCritique?: number;
  ordre?: number;
  isActive: boolean;
}

export interface ColumnMappingResponse {
  nomColonne?: string;
  indexColonne?: number;
  kpiSuggere?: KpiResponse;
  scoreSimilarite?: number;
  noteSuggestion?: string;
}

export interface UserMappingTemplateResponse {
  id: number;
  nom: string;
  ligneEntete?: number;
  createdAt?: string;
  colonnes?: ColumnMappingResponse[];
}

export interface ImportSessionResponse {
  id: number;
  mode: ImportMode;
  nomFichier: string;
  periodeN1: number;
  periodeN: number;
  statut: ImportStatut;
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

export interface StagingDonneeResponse {
  id: number;
  kpiId: number;
  kpiNom: string;
  kpiUnite?: string;
  categorieCode?: string;
  valeurBruteN1?: string;
  valeurBruteN?: string;
  valeurN1?: number;
  valeurN?: number;
  statutNettoyage?: StatutNettoyage;
  noteNettoyage?: string;
}

export interface ApercuResponse {
  importSession: ImportSessionResponse;
  donnees: StagingDonneeResponse[];
  peutConfirmer: boolean;
}

export interface ResultatKpiResponse {
  id: number;
  kpiId: number;
  kpiNom: string;
  kpiUnite?: string;
  categorieCode?: string;
  categorieLibelle?: string;
  periodeN1: number;
  periodeN: number;
  valeurN1: number;
  valeurN: number;
  variationAbsolue?: number;
  variationRelative?: number;
  niveauVariation?: string;
  tendance?: string;
  analyseIa?: string;
  createdAt?: string;
}

export interface ResultatGlobalResponse {
  importSessionId: number;
  periodeN1: number;
  periodeN: number;
  resultats: ResultatKpiResponse[];
  nombreFaible: number;
  nombreModere: number;
  nombreCritique: number;
  analyseGlobaleIa?: string;
}

export interface CorrectionRequest {
  stagingDonneeId: number;
  valeurN1?: number | null;
  valeurN?: number | null;
}

export interface AdminStatsResponse {
  totalAnalyses: number;
  totalKpisCritiques: number;
  nombreAnalystesActifs: number;
  nombreAdmins: number;
}

export interface AdminKpiCritiqueResponse {
  kpiId: number;
  kpiNom: string;
  categorieCode?: string;
  categorieLibelle?: string;
  nombreAnalystesAvecCritique: number;
  variationMoyenne?: number;
}

export interface AdminAnalysteItemResponse {
  userId: number;
  nom: string;
  prenom: string;
  email: string;
  dernierImportId?: number;
  dernierePeriode?: string;
  nombreKpisCritiques: number;
  statut?: string;
}

export interface AdminGraphiquesDataResponse {
  repartitionNiveaux?: Record<string, number>;
  kpisCritiquesByCategorie?: Record<string, number>;
}

export interface SaveMappingRequest {
  nom: string;
  ligneEntete: number;
  colonnes: ColumnMappingRequest[];
}

export interface ColumnMappingRequest {
  nomColonne: string;
  indexColonne: number;
  kpiId?: number;
}

export interface CreateAnalysteRequest {
  nom: string;
  prenom: string;
  email: string;
  password: string;
}

export interface UpdateUserRequest {
  nom: string;
  prenom: string;
  email: string;
  role: UserRole;
  active: boolean;
}