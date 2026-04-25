export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  nom: string;
  prenom: string;
  role: string;
  message?: string;
}

export interface OtpRequest {
  email: string;
  code: string;
  rememberMe: boolean;
}

export interface RegisterRequest {
  nom: string;
  prenom: string;
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  password: string;
  confirmPassword: string;
}

export interface GenericResponse {
  message: string;
}

export interface VerifyResponse {
  message: string;
}
