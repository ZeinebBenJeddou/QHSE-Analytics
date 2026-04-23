import { UserResponse } from './user-response';

export interface UserListResponse {
  users: UserResponse[];
  totalAdmins: number;
  totalAnalystes: number;
  totalActifs: number;
  totalInactifs: number;
}
