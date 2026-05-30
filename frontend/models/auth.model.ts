// src/app/models/auth.model.ts

export interface LoginRequest {
  username: string;
  password: string;
}

export interface JwtResponse {
  token: string;
  type: string;
  username: string;
  email: string;
  role: string;
}

export interface User {
  id: number;
  username: string;
  email: string;
  role: 'ADMIN';
  active: boolean;
}
