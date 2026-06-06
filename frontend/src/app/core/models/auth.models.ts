export interface RegisterRequest {
  username: string;
  email: string;
  plainPassword: string;
  displayName: string;
}

export interface LoginRequest {
  usernameOrEmail: string;
  plainPassword: string;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  displayName: string;
  role: 'ADMIN' | 'STUDENT';
  active: boolean;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  user: UserResponse;
}
