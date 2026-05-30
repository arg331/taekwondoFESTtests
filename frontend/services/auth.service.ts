// src/app/services/auth.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { LoginRequest, JwtResponse } from '../models/auth.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject: BehaviorSubject<JwtResponse | null>;
  public currentUser: Observable<JwtResponse | null>;

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<JwtResponse | null>(
      storedUser ? JSON.parse(storedUser) : null
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  /**
   * Obtener valor actual del usuario
   */
  public get currentUserValue(): JwtResponse | null {
    return this.currentUserSubject.value;
  }

  /**
   * Login
   */
  login(credentials: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        // Guardar en localStorage
        localStorage.setItem('currentUser', JSON.stringify(response));
        this.currentUserSubject.next(response);
      })
    );
  }

  /**
   * Logout
   */
  logout(): void {
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }

  /**
   * Verificar si está autenticado
   */
  isAuthenticated(): boolean {
    return this.currentUserValue !== null;
  }

  /**
   * Obtener token actual
   */
  getToken(): string | null {
    return this.currentUserValue?.token || null;
  }

  /**
   * Verificar si hay usuarios en el sistema
   */
  hasUsers(): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/status`);
  }
}
