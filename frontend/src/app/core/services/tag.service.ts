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
}
