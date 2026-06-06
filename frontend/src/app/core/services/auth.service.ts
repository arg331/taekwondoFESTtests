import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import {
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
 * El token y el usuario se persisten en localStorage para
 * sobrevivir recargas de página.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly apiUrl = `${environment.apiUrl}/auth`;

  // Signal con el usuario actual (null si no autenticado)
  private _currentUser = signal<UserResponse | null>(this.loadUserFromStorage());

  // Computed públicos para los componentes
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null);
  readonly isAdmin = computed(() => this._currentUser()?.role === 'ADMIN');

  constructor(private http: HttpClient, private router: Router) {}

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

  // ──────────────────────────────────────────────────
  // Gestión de sesión local
  // ──────────────────────────────────────────────────

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this._currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  // ──────────────────────────────────────────────────
  // Privados
  // ──────────────────────────────────────────────────

  private saveSession(response: LoginResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
    this._currentUser.set(response.user);
  }

  private loadUserFromStorage(): UserResponse | null {
    const stored = localStorage.getItem(USER_KEY);
    return stored ? JSON.parse(stored) : null;
  }
}
