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
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
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
  dernierePeriode: string | null;
  dernierImportDate: string | null;
  nombreKpisCritiques: number;
  statut: string;
  confidenceScore: number | null;
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
  evolutionParCategorie: Array<{
    categorie: string;
    valeurMoyenneN1: number;
    valeurMoyenneN: number;
    variationMoyenne: number;
  }>;
}

export type UniteKpi = 'POURCENTAGE' | 'NOMBRE' | 'KWH' | 'KG';

export interface KpiResponse {
  id: number;
  nom: string;
  definition: string;
  unite: UniteKpi;
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
  unite: UniteKpi;
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

export interface AuditLogResponse {
  id: number;
  adminEmail: string;
  action: string;
  targetUserId: number | null;
  targetEmail: string | null;
  details: string | null;
  timestamp: string;
}

export interface AuditPageResponse {
  content: AuditLogResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}


export interface RagKnowledgeResponse {
  id: number;
  kpiName: string;
  definition: string | null;
  thresholds: string | null;
  category: string | null;
  hasEmbedding: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RagKnowledgeRequest {
  kpiName: string;
  definition: string;
  thresholds: string | null;
  category: string | null;
}

export interface RagSearchTestRequest {
  query: string;
  topK: number;
  threshold: number;
}

export interface RagSearchTestResultItem {
  id: number;
  kpiName: string;
  category: string | null;
  definition: string | null;
}

