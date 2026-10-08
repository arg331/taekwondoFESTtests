import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ResultResponse, ExamStatisticsResponse, SubmitExamRequest } from '../models/result.models';

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

  getMyAttempts(): Observable<ResultResponse[]> {
    return this.http.get<ResultResponse[]>(`${this.api}/me`);
  }
}
