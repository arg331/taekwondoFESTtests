import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ExamResponse,
  PublicExamResponse,
  ExamDraftRequest,
  ExamConfig,
  PublishExamRequest,
  RenameExamRequest,
  UpdateExamQuestionsRequest,
  ChangeExamVisibilityRequest,
  ExpirationRequest,
  ExamAttemptResponse,
  ChangeAttemptsRequest,
  PublishedQuestionResponse,
  StartAttemptRequest,
  SaveAnswerRequest
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

  /** Copia el examen como borrador nuevo para ampliarlo (RF-18). */
  /** Las preguntas de un examen publicado tal como las ven los alumnos (DE-03). */
  publishedQuestions(id: number): Observable<PublishedQuestionResponse[]> {
    return this.http.get<PublishedQuestionResponse[]>(`${this.api}/${id}/published-questions`);
  }

  /** Subir o bajar los intentos por alumno de un examen publicado (RF-52). */
  changeAttempts(id: number, request: ChangeAttemptsRequest): Observable<ExamResponse> {
    return this.http.patch<ExamResponse>(`${this.api}/${id}/attempts`, request);
  }

  copy(id: number): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/${id}/copy`, {});
  }

  deleteDraft(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  // ── Cualquier usuario con sesión ──────────────────
  getPublic(): Observable<PublicExamResponse[]> {
    return this.http.get<PublicExamResponse[]>(`${this.api}/public`);
  }

  // ── Alumno (público) ──────────────────────────────
  getByCode(code: string): Observable<PublicExamResponse> {
    return this.http.get<PublicExamResponse>(`${this.api}/by-code/${encodeURIComponent(code)}`);
  }

  /** Empieza un intento a nombre del alumno (DE-04); si ya tiene uno abierto, lo recupera (RF-24). */
  startAttempt(code: string, identity: StartAttemptRequest): Observable<ExamAttemptResponse> {
    return this.http.post<ExamAttemptResponse>(`${this.api}/by-code/${encodeURIComponent(code)}/attempts`, identity);
  }

  /** El intento tal como se dejó, tras recargar o perder la conexión (RF-24). */
  resumeAttempt(code: string, attemptToken: string): Observable<ExamAttemptResponse> {
    return this.http.post<ExamAttemptResponse>(`${this.api}/by-code/${encodeURIComponent(code)}/attempts/resume`,
      { attemptToken });
  }

  /** Guarda lo marcado en una pregunta en cuanto se contesta (RF-24). */
  saveAnswer(code: string, request: SaveAnswerRequest): Observable<void> {
    return this.http.put<void>(`${this.api}/by-code/${encodeURIComponent(code)}/attempts/answers`, request);
  }
}
