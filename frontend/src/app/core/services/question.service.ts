import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { QuestionResponse, CreateQuestionRequest, EditQuestionRequest } from '../models/question.models';

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private http = inject(HttpClient);
  private api  = `${environment.apiUrl}/questions`;

  getAll(): Observable<QuestionResponse[]> {
    return this.http.get<QuestionResponse[]>(this.api);
  }

  getById(id: number): Observable<QuestionResponse> {
    return this.http.get<QuestionResponse>(`${this.api}/${id}`);
  }

  search(tagIds?: number[], textContains?: string): Observable<QuestionResponse[]> {
    let params = new HttpParams();
    if (tagIds?.length) params = params.set('tagIds', tagIds.join(','));
    if (textContains)   params = params.set('textContains', textContains);
    return this.http.get<QuestionResponse[]>(`${this.api}/search`, { params });
  }

  create(request: CreateQuestionRequest): Observable<QuestionResponse> {
    return this.http.post<QuestionResponse>(this.api, request);
  }

  edit(id: number, request: EditQuestionRequest): Observable<QuestionResponse> {
    return this.http.put<QuestionResponse>(`${this.api}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
