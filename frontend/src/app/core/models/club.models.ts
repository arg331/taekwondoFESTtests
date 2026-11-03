import { UserRole } from './auth.models';

/** Club (RF-38). La lista es pública: sirve también para el registro. */
export interface ClubResponse {
  id: number;
  name: string;
}

/** Miembro de un club; lo ven el administrador y los profesores del club. */
export interface ClubMemberResponse {
  id: number;
  username: string;
  displayName: string;
  role: UserRole;
  /** Si quien mira puede abrir sus resultados; si no, su nombre va sin enlace (DE-23). */
  resultsVisible: boolean;
}

/** Usuario que se puede añadir; clubName es el club en el que está ahora (solo para el administrador). */
export interface ClubCandidateResponse {
  id: number;
  username: string;
  displayName: string;
  role: UserRole;
  clubName: string | null;
}
