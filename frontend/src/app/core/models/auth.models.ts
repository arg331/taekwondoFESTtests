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

/** Registro público (RF-01): email y club opcionales (null = sin ellos). */
export interface RegisterRequest {
  username: string;
  email: string | null;
  plainPassword: string;
  displayName: string;
  clubId: number | null;
}

export interface LoginRequest {
  usernameOrEmail: string;
  plainPassword: string;
}

export interface UserResponse {
  id: number;
  username: string;
  /** Opcional (RF-01). */
  email: string | null;
  displayName: string;
  role: UserRole;
  /** Como mucho un club (RF-39); null = ninguno. */
  club: { id: number; name: string } | null;
  active: boolean;
  /** Tiene una contraseña temporal del administrador y debe cambiarla antes de seguir (RF-33). */
  mustChangePassword: boolean;
  createdAt: string;
}

/** Cambiar la propia contraseña (RF-06): el servidor devuelve una sesión nueva. */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface LoginResponse {
  token: string;
  user: UserResponse;
}
