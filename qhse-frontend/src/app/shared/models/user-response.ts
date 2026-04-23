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
