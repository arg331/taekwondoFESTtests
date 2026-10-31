import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CommunityExamResponse,
  CommunityQuestionResponse,
  CopyQuestionsResponse
} from '../models/community.models';

/** Exámenes de prueba de otros profesores (RF-30), favoritos (RF-31) y copiar sus preguntas (DE-12). */
@Injectable({ providedIn: 'root' })
export class CommunityService {
  private http = inject(HttpClient);
  private api = environment.apiUrl;

  list(): Observable<CommunityExamResponse[]> {
    return this.http.get<CommunityExamResponse[]>(`${this.api}/community/exams`);
  }

  favorite(examId: number): Observable<void> {
    return this.http.post<void>(`${this.api}/exams/${examId}/favorite`, {});
  }

  unfavorite(examId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/exams/${examId}/favorite`);
  }

  questions(examId: number): Observable<CommunityQuestionResponse[]> {
    return this.http.get<CommunityQuestionResponse[]>(`${this.api}/community/exams/${examId}/questions`);
  }

  copyQuestions(examId: number, questionIds: number[]): Observable<CopyQuestionsResponse> {
    return this.http.post<CopyQuestionsResponse>(`${this.api}/community/exams/${examId}/copy-questions`, { questionIds });
  }
}
