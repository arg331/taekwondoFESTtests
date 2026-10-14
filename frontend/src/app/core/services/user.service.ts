import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse, UserRole } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private api  = `${environment.apiUrl}/users`;

  getAll(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.api);
  }

  /** Solo el administrador, y nunca su propio rol (RF-36). */
  changeRole(id: number, role: UserRole): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.api}/${id}/role`, { role });
  }

  /** Desactivar o reactivar una cuenta (RF-05); solo el administrador, y nunca la propia. */
  setActive(id: number, active: boolean): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.api}/${id}/active`, { active });
  }

  /** Solo el administrador, y nunca su propia contraseña (RF-35). */
  resetPassword(id: number, temporaryPassword: string): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.api}/${id}/password`, { temporaryPassword });
  }
}
