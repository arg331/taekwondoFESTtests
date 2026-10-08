import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ExamResponse,
  ExamDraftRequest,
  ExamConfig,
  PublishExamRequest,
  RenameExamRequest,
  UpdateExamQuestionsRequest,
  ChangeExamVisibilityRequest,
  ExpirationRequest,
  ExamAttemptResponse
} from '../models/exam.models';

@Injectable({ providedIn: 'root' })
export class ExamService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/exams`;

  // ── Profesor ──────────────────────────────────────
  getMine(): Observable<ExamResponse[]> {
    return this.http.get<ExamResponse[]>(`${this.api}/mine`);
  }

  getById(id: number): Observable<ExamResponse> {
    return this.http.get<ExamResponse>(`${this.api}/${id}`);
  }

  createDraft(request: ExamDraftRequest): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/drafts`, request);
  }

  preGenerate(request: ExamDraftRequest): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/drafts/pre-generated`, request);
  }

  rename(id: number, request: RenameExamRequest): Observable<ExamResponse> {
    return this.http.patch<ExamResponse>(`${this.api}/${id}/title`, request);
  }

  changeConfig(id: number, request: ExamConfig): Observable<ExamResponse> {
    return this.http.patch<ExamResponse>(`${this.api}/${id}/config`, request);
  }

  updateQuestions(id: number, request: UpdateExamQuestionsRequest): Observable<ExamResponse> {
    return this.http.put<ExamResponse>(`${this.api}/${id}/questions`, request);
  }

  changeVisibility(id: number, request: ChangeExamVisibilityRequest): Observable<ExamResponse> {
    return this.http.patch<ExamResponse>(`${this.api}/${id}/visibility`, request);
  }

  publish(id: number, request: PublishExamRequest): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/${id}/publish`, request);
  }

  close(id: number): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/${id}/close`, {});
  }

  reopen(id: number, request: ExpirationRequest): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/${id}/reopen`, request);
  }

  changeExpiration(id: number, request: ExpirationRequest): Observable<ExamResponse> {
    return this.http.patch<ExamResponse>(`${this.api}/${id}/expiration`, request);
  }

  deleteDraft(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  // ── Favoritos ─────────────────────────────────────
  getFavorites(): Observable<ExamResponse[]> {
    return this.http.get<ExamResponse[]>(`${environment.apiUrl}/favorites`);
  }

  favorite(id: number): Observable<void> {
    return this.http.post<void>(`${this.api}/${id}/favorite`, {});
  }

  unfavorite(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}/favorite`);
  }

  // ── Cualquier usuario con sesión ──────────────────
  getPublic(): Observable<ExamResponse[]> {
    return this.http.get<ExamResponse[]>(`${this.api}/public`);
  }

  // ── Alumno (público) ──────────────────────────────
  getByCode(code: string): Observable<ExamResponse> {
    return this.http.get<ExamResponse>(`${this.api}/by-code/${encodeURIComponent(code)}`);
  }

  /** Empieza un intento: preguntas sin solución + token con la hora de inicio. */
  startAttempt(code: string): Observable<ExamAttemptResponse> {
    return this.http.post<ExamAttemptResponse>(`${this.api}/by-code/${encodeURIComponent(code)}/attempts`, {});
  }
}
