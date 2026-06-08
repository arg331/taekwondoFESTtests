import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private api  = `${environment.apiUrl}/users`;

  getAll(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.api);
  }

  promote(id: number): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.api}/${id}/promote`, {});
  }

  demote(id: number): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.api}/${id}/demote`, {});
  }
}
