import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import {
  ChangePasswordRequest,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserResponse
} from '../models/auth.models';

const TOKEN_KEY = 'fest_token';
const USER_KEY  = 'fest_user';

/**
 * Servicio de autenticación.
 *
 * Usa signals (Angular 17+) para el estado del usuario actual.
 * El token y el usuario se guardan en sessionStorage (DE-10): sobreviven a recargar la página, pero al
 * cerrar el navegador o abrir otra pestaña hay que volver a iniciar sesión. Nada queda en localStorage.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  // Signal con el usuario actual (null si no autenticado)
  private readonly _currentUser = signal<UserResponse | null>(this.loadUserFromStorage());

  constructor() {
    // Las versiones anteriores guardaban la sesión una semana en localStorage: se borra al entrar
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  // Computed públicos para los componentes
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null);
  readonly isAdmin = computed(() => this._currentUser()?.role === 'ADMIN');
  /** Prepara preguntas y exámenes y ve sus resultados: profesor o administrador. */
  readonly canTeach = computed(() => {
    const role = this._currentUser()?.role;
    return role === 'TEACHER' || role === 'ADMIN';
  });
  /** Contraseña temporal del administrador: hay que cambiarla antes de usar la app (RF-33). */
  readonly mustChangePassword = computed(() => this._currentUser()?.mustChangePassword === true);

  // ──────────────────────────────────────────────────
  // Endpoints públicos
  // ──────────────────────────────────────────────────

  register(request: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.apiUrl}/register`, request);
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.saveSession(response))
    );
  }

  // ──────────────────────────────────────────────────
  // Endpoints privados
  // ──────────────────────────────────────────────────

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/me`);
  }

  /** RF-06 y RF-33. El servidor cierra las demás sesiones y devuelve una nueva para esta. */
  changePassword(request: ChangePasswordRequest): Observable<LoginResponse> {
    return this.http.put<LoginResponse>(`${this.apiUrl}/me/password`, request).pipe(
      tap(response => this.saveSession(response))
    );
  }

  /** RF-34, sin sesión: envía el usuario y un enlace a quien tenga ese email. Responde igual exista o no. */
  requestRecovery(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/recovery`, { email });
  }

  /** RF-34, sin sesión: la contraseña nueva con el código del enlace. Cierra las sesiones abiertas. */
  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/recovery/reset`, { token, newPassword });
  }

  // ──────────────────────────────────────────────────
  // Gestión de sesión local
  // ──────────────────────────────────────────────────

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    this._currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  /** Tras cambiar algo del propio usuario (por ejemplo, su club), la sesión guarda la versión nueva. */
  updateCurrentUser(user: UserResponse): void {
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
    this._currentUser.set(user);
  }

  getToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  // ──────────────────────────────────────────────────
  // Privados
  // ──────────────────────────────────────────────────

  private saveSession(response: LoginResponse): void {
    sessionStorage.setItem(TOKEN_KEY, response.token);
    sessionStorage.setItem(USER_KEY, JSON.stringify(response.user));
    this._currentUser.set(response.user);
  }

  private loadUserFromStorage(): UserResponse | null {
    const stored = sessionStorage.getItem(USER_KEY);
    return stored ? JSON.parse(stored) : null;
  }
}
