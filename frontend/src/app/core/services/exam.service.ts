import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ExamResponse,
  CreateExamDraftRequest,
  PreGenerateExamDraftRequest,
  PublishExamRequest,
  RenameExamRequest,
  ChangeExamConfigRequest,
  UpdateExamQuestionsRequest,
  ChangeExamVisibilityRequest,
  ExtendExamExpirationRequest,
  ReopenExamRequest
} from '../models/exam.models';

@Injectable({ providedIn: 'root' })
export class ExamService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/exams`;

  // ── Consultas ──────────────────────────────────────
  getMine(): Observable<ExamResponse[]> {
    return this.http.get<ExamResponse[]>(`${this.api}/mine`);
  }

  getPublic(): Observable<ExamResponse[]> {
    return this.http.get<ExamResponse[]>(`${this.api}/public`);
  }

  getById(id: number): Observable<ExamResponse> {
    return this.http.get<ExamResponse>(`${this.api}/${id}`);
  }

  getByCode(code: string): Observable<ExamResponse> {
    return this.http.get<ExamResponse>(`${this.api}/by-code/${code}`);
  }

  getFavorites(): Observable<ExamResponse[]> {
    return this.http.get<ExamResponse[]>(`${environment.apiUrl}/favorites`);
  }

  // ── Creación ──────────────────────────────────────
  createDraft(request: CreateExamDraftRequest): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/drafts`, request);
  }

  preGenerate(request: PreGenerateExamDraftRequest): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/drafts/pre-generated`, request);
  }

  // ── Edición ──────────────────────────────────────
  rename(id: number, request: RenameExamRequest): Observable<ExamResponse> {
    return this.http.patch<ExamResponse>(`${this.api}/${id}/title`, request);
  }

  changeConfig(id: number, request: ChangeExamConfigRequest): Observable<ExamResponse> {
    return this.http.patch<ExamResponse>(`${this.api}/${id}/config`, request);
  }

  updateQuestions(id: number, request: UpdateExamQuestionsRequest): Observable<ExamResponse> {
    return this.http.put<ExamResponse>(`${this.api}/${id}/questions`, request);
  }

  changeVisibility(id: number, request: ChangeExamVisibilityRequest): Observable<ExamResponse> {
    return this.http.patch<ExamResponse>(`${this.api}/${id}/visibility`, request);
  }

  // ── Transiciones de estado ──────────────────────────────────────
  publish(id: number, request: PublishExamRequest): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/${id}/publish`, request);
  }

  close(id: number): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/${id}/close`, {});
  }

  reopen(id: number, request: ReopenExamRequest): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/${id}/reopen`, request);
  }

  extendExpiration(id: number, request: ExtendExamExpirationRequest): Observable<ExamResponse> {
    return this.http.patch<ExamResponse>(`${this.api}/${id}/expiration`, request);
  }

  // ── Borrado ──────────────────────────────────────
  deleteDraft(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  // ── Favoritos ──────────────────────────────────────
  favorite(id: number): Observable<void> {
    return this.http.post<void>(`${this.api}/${id}/favorite`, {});
  }

  unfavorite(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}/favorite`);
  }
}
