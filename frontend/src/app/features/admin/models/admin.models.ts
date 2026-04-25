export interface UserResponse {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: string;
  verified: boolean;
  active: boolean;
  createdAt: string;
  isSystemAdmin: boolean;
}

export interface ProfileResponse {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: string;
  createdAt: string;
}

export interface UpdateProfilRequest {
  nom: string;
  prenom: string;
}

export interface ChangePasswordRequest {
  ancienPassword: string;
  nouveauPassword: string;
  confirmPassword: string;
}

export interface UserListResponse {
  users: UserResponse[];
  totalAdmins: number;
  totalAnalystes: number;
  totalActifs: number;
  totalInactifs: number;
}

export interface CreateAnalysteRequest {
  nom: string;
  prenom: string;
  email: string;
}

export interface UpdateUserRequest {
  nom: string;
  prenom: string;
  email: string;
}

export interface AdminStatsResponse {
  totalAnalyses: number;
  totalKpisCritiques: number;
  nombreAnalystesActifs: number;
  nombreAdmins: number;
}

export interface AdminAnalysteItemResponse {
  userId: number;
  nom: string;
  prenom: string;
  email: string;
  dernierImportId: number | null;
  dernierePeriode: string;
  nombreKpisCritiques: number;
  statut: string;
}

export interface AdminKpiCritiqueResponse {
  kpiId: number;
  kpiNom: string;
  categorieCode: string;
  categorieLibelle: string;
  nombreAnalystesAvecCritique: number;
  variationMoyenne: number;
}

export interface AdminRepartitionCategorieItem {
  categorieCode: string;
  categorieLibelle: string;
  nombreFaibles: number;
  nombreModeres: number;
  nombreCritiques: number;
  total: number;
}

export interface AdminRepartitionResponse {
  categories: AdminRepartitionCategorieItem[];
}

export interface AdminGraphiquesDataResponse {
  repartitionNiveaux: Record<string, number>;
  kpisCritiquesByCategorie: Record<string, number>;
  evolutionParCategorie: Array<{ categorie: string; serieA: number; serieB: number; serieC: number }>;
}

export interface KpiResponse {
  id: number;
  nom: string;
  definition: string;
  unite: string;
  categorieCode: string;
  categorieLibelle: string;
  seuilFaible: number;
  seuilModere: number;
  seuilCritique: number;
  ordre: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategorieKpiResponse {
  id: number;
  code: string;
  libelle: string;
  description: string;
  nombreKpisActifs: number;
}

export interface CreateKpiRequest {
  nom: string;
  definition: string;
  unite: string;
  categorieCode: string;
  seuilFaible: number;
  seuilModere: number;
  seuilCritique: number;
  ordre: number;
}

export interface UpdateKpiRequest {
  nom?: string;
  definition?: string;
  unite?: string;
  categorieCode?: string;
  seuilFaible?: number;
  seuilModere?: number;
  seuilCritique?: number;
  ordre?: number;
}

export interface AnalyseCompleteResponse {
  importId: number;
  periode: string;
  titre: string;
  resume: string;
  details: string;
}

export interface MessageResponse {
  message: string;
}
