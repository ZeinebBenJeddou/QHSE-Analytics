export interface Profil {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: 'ADMIN' | 'ANALYSTE';
  createdAt?: string;
}

export interface UpdateProfilRequest {
  nom: string;
  prenom: string;
  email?: string;
}

export interface ChangePasswordRequest {
  ancienPassword: string;
  nouveauPassword: string;
  confirmPassword: string;
}

export interface MessageResponse {
  message: string;
}
