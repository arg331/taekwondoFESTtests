import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse, UserRole } from '../models/auth.models';
import { PageResponse } from '../models/page.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private api  = `${environment.apiUrl}/users`;

  /** Una página de usuarios por fecha de alta; q busca en el nombre, el usuario o el email (RF-37, RNF-09). */
  search(q: string, page: number, size: number): Observable<PageResponse<UserResponse>> {
    const params = new HttpParams().set('q', q).set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<UserResponse>>(this.api, { params });
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
