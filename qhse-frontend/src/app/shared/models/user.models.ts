export interface User {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: 'ADMIN' | 'ANALYSTE';
  verified: boolean;
  active: boolean;
  createdAt?: string;
  isSystemAdmin?: boolean;
}

export interface UserListResponse {
  users: User[];
  totalAdmins: number;
  totalAnalystes: number;
  totalActifs: number;
  totalInactifs: number;
}

export interface CreateUserRequest {
  nom: string;
  prenom: string;
  email: string;
}

export interface UpdateUserRequest {
  nom: string;
  prenom: string;
  email: string;
}

export interface MessageResponse {
  message: string;
}
