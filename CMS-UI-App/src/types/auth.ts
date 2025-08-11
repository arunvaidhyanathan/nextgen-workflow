export interface LoginRequest {
  username: string;
  password: string;
}

export interface UserInfo {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  isActive: boolean;
  attributes?: any;
  roles?: string[];
}

export interface LoginResponse {
  success: boolean;
  message: string;
  sessionId: string;
  user: UserInfo;
}

export interface SessionValidationResponse {
  success: boolean;
  message: string;
  user?: UserInfo;
}

export interface AuthState {
  isAuthenticated: boolean;
  user: UserInfo | null;
  sessionId: string | null;
}