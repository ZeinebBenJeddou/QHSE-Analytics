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
  rememberMe: boolean;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface MessageResponse {
  message: string;
}