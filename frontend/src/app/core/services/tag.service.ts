import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TagResponse, CreateTagRequest, RenameTagRequest } from '../models/tag.models';

@Injectable({ providedIn: 'root' })
export class TagService {
  private http = inject(HttpClient);
  private api  = `${environment.apiUrl}/tags`;

  getAll(): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(this.api);
  }

  getById(id: number): Observable<TagResponse> {
    return this.http.get<TagResponse>(`${this.api}/${id}`);
  }

  create(request: CreateTagRequest): Observable<TagResponse> {
    return this.http.post<TagResponse>(this.api, request);
  }

  rename(id: number, request: RenameTagRequest): Observable<TagResponse> {
    return this.http.patch<TagResponse>(`${this.api}/${id}`, request);
  }

  /** RF-08. */
  changeColor(id: number, newColor: string): Observable<TagResponse> {
    return this.http.patch<TagResponse>(`${this.api}/${id}/color`, { newColor });
  }

  /** Lo quita de las preguntas que lo tienen; ellas no se borran (RF-08). */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
