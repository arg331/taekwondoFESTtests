import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ClubCandidateResponse, ClubMemberResponse, ClubResponse } from '../models/club.models';
import { UserResponse } from '../models/auth.models';

/** Clubes (RF-38 a RF-42). */
@Injectable({ providedIn: 'root' })
export class ClubService {
  private http = inject(HttpClient);
  private api  = `${environment.apiUrl}/clubs`;

  list(): Observable<ClubResponse[]> {
    return this.http.get<ClubResponse[]>(this.api);
  }

  create(name: string): Observable<ClubResponse> {
    return this.http.post<ClubResponse>(this.api, { name });
  }

  rename(id: number, name: string): Observable<ClubResponse> {
    return this.http.patch<ClubResponse>(`${this.api}/${id}`, { name });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  members(id: number): Observable<ClubMemberResponse[]> {
    return this.http.get<ClubMemberResponse[]>(`${this.api}/${id}/members`);
  }

  candidates(id: number, query: string): Observable<ClubCandidateResponse[]> {
    return this.http.get<ClubCandidateResponse[]>(`${this.api}/${id}/candidates`, { params: { q: query } });
  }

  addMember(id: number, userId: number): Observable<ClubMemberResponse> {
    return this.http.put<ClubMemberResponse>(`${this.api}/${id}/members/${userId}`, null);
  }

  removeMember(id: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}/members/${userId}`);
  }

  /** El club del propio usuario; null = ninguno (RF-41). Devuelve el usuario actualizado. */
  chooseMine(clubId: number | null): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${environment.apiUrl}/users/me/club`, { clubId });
  }
}
