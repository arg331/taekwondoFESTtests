import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ExamResponse } from '../models/exam.models';
import { QuestionResponse } from '../models/question.models';
import { ExamShareResponse, InvitationResponse, ShareCandidateResponse } from '../models/share.models';

/**
 * Compartir exámenes entre profesores (RF-51): el dueño invita y el invitado acepta o rechaza.
 * Al aceptar, el invitado consulta el mismo examen y sus resultados, sin poder cambiarlo.
 */
@Injectable({ providedIn: 'root' })
export class ShareService {
  private http = inject(HttpClient);
  private api  = environment.apiUrl;

  // ── El dueño ──────────────────────────────────────
  list(examId: number): Observable<ExamShareResponse[]> {
    return this.http.get<ExamShareResponse[]>(`${this.api}/exams/${examId}/shares`);
  }

  candidates(examId: number, query: string): Observable<ShareCandidateResponse[]> {
    return this.http.get<ShareCandidateResponse[]>(`${this.api}/exams/${examId}/shares/candidates`,
      { params: new HttpParams().set('query', query) });
  }

  invite(examId: number, teacherId: number): Observable<ExamShareResponse> {
    return this.http.post<ExamShareResponse>(`${this.api}/exams/${examId}/shares`, { teacherId });
  }

  /** Cancela la invitación o retira el acceso. */
  revoke(examId: number, teacherId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/exams/${examId}/shares/${teacherId}`);
  }

  // ── El invitado ───────────────────────────────────
  invitations(): Observable<InvitationResponse[]> {
    return this.http.get<InvitationResponse[]>(`${this.api}/shares/invitations`);
  }

  accept(invitationId: number): Observable<ExamResponse> {
    return this.http.post<ExamResponse>(`${this.api}/shares/invitations/${invitationId}/accept`, null);
  }

  reject(invitationId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/shares/invitations/${invitationId}`);
  }

  /** Preguntas de los exámenes que me han compartido (RF-57): solo se consultan. */
  questions(): Observable<QuestionResponse[]> {
    return this.http.get<QuestionResponse[]>(`${this.api}/shares/questions`);
  }

  /** «Dejar de ver» un examen que me compartieron. */
  leave(examId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/exams/${examId}/shares/me`);
  }
}
