import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ResultResponse, ExamStatisticsResponse, SubmitExamRequest, QuestionStatsResponse, UserResultsResponse
} from '../models/result.models';

@Injectable({ providedIn: 'root' })
export class ResultService {
  private http = inject(HttpClient);
  private api  = `${environment.apiUrl}/results`;

  submit(request: SubmitExamRequest): Observable<ResultResponse> {
    return this.http.post<ResultResponse>(this.api, request);
  }

  getByExam(examId: number): Observable<ResultResponse[]> {
    return this.http.get<ResultResponse[]>(`${this.api}/exam/${examId}`);
  }

  getStatistics(examId: number): Observable<ExamStatisticsResponse> {
    return this.http.get<ExamStatisticsResponse>(`${this.api}/exam/${examId}/statistics`);
  }

  /** Acierto de cada pregunta del examen, en su orden (RF-28). */
  getQuestionStats(examId: number): Observable<QuestionStatsResponse[]> {
    return this.http.get<QuestionStatsResponse[]>(`${this.api}/exam/${examId}/questions`);
  }

  /** Resultados de un usuario: el profesor ve los de sus exámenes; el administrador, todos (RF-55). */
  getUserResults(userId: number): Observable<UserResultsResponse> {
    return this.http.get<UserResultsResponse>(`${this.api}/user/${userId}`);
  }

  /** Excel con las entregas del examen, ordenadas por club (RF-27). */
  exportExcel(examId: number): Observable<Blob> {
    return this.http.get(`${this.api}/exam/${examId}/export`, { responseType: 'blob' });
  }

  getMyAttempts(): Observable<ResultResponse[]> {
    return this.http.get<ResultResponse[]>(`${this.api}/me`);
  }
}
