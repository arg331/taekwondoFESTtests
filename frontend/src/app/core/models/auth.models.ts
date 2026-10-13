/**
 * Rol de un usuario (RF-04): el administrador gestiona usuarios y roles y además hace todo lo
 * del profesor; el profesor prepara preguntas y exámenes y ve sus resultados; el alumno hace exámenes.
 */
export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT';

export const USER_ROLES: readonly UserRole[] = ['ADMIN', 'TEACHER', 'STUDENT'];

export const ROLE_LABEL: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  TEACHER: 'Profesor',
  STUDENT: 'Alumno'
};

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
  role: UserRole;
  active: boolean;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  user: UserResponse;
}
