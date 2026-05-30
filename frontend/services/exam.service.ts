// src/app/services/exam.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Exam, ExamConfig, ExamResponse, ExamSubmission } from '../models/exam.model';
import { Result } from '../models/result.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ExamService {
  private apiUrl = `${environment.apiUrl}/exams`;

  constructor(private http: HttpClient) {}

  /**
   * Generar nuevo examen
   */
  generateExam(config: ExamConfig): Observable<Exam> {
    return this.http.post<Exam>(`${this.apiUrl}/generate`, config);
  }

  /**
   * Obtener examen por código (para estudiantes)
   */
  getExamByCode(code: string): Observable<ExamResponse> {
    return this.http.get<ExamResponse>(`${this.apiUrl}/${code}`);
  }

  /**
   * Enviar respuestas del examen
   */
  submitExam(examId: number, submission: ExamSubmission): Observable<Result> {
    return this.http.post<Result>(`${this.apiUrl}/${examId}/submit`, submission);
  }

  /**
   * Obtener examen activo actual
   */
  getActiveExam(): Observable<Exam> {
    return this.http.get<Exam>(`${this.apiUrl}/active`);
  }

  /**
   * Obtener todos los exámenes (admin)
   */
  getAllExams(): Observable<Exam[]> {
    return this.http.get<Exam[]>(this.apiUrl);
  }
}
