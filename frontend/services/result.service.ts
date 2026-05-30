// src/app/services/result.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Result, ExamStatistics } from '../models/result.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ResultService {
  private apiUrl = `${environment.apiUrl}/results`;

  constructor(private http: HttpClient) {}

  /**
   * Obtener todos los resultados
   */
  getAllResults(): Observable<Result[]> {
    return this.http.get<Result[]>(this.apiUrl);
  }

  /**
   * Obtener resultado por ID
   */
  getResultById(id: number): Observable<Result> {
    return this.http.get<Result>(`${this.apiUrl}/${id}`);
  }

  /**
   * Obtener resultados por examen
   */
  getResultsByExam(examId: number): Observable<Result[]> {
    return this.http.get<Result[]>(`${this.apiUrl}/exam/${examId}`);
  }

  /**
   * Obtener resultados por examen ordenados por puntuación
   */
  getResultsByExamOrdered(examId: number): Observable<Result[]> {
    return this.http.get<Result[]>(`${this.apiUrl}/exam/${examId}/ordered`);
  }

  /**
   * Buscar resultados por nombre de estudiante
   */
  searchResults(studentName: string): Observable<Result[]> {
    return this.http.get<Result[]>(`${this.apiUrl}/search`, {
      params: { student: studentName }
    });
  }

  /**
   * Obtener últimos resultados
   */
  getLatestResults(): Observable<Result[]> {
    return this.http.get<Result[]>(`${this.apiUrl}/latest`);
  }

  /**
   * Obtener estadísticas de un examen
   */
  getExamStatistics(examId: number): Observable<ExamStatistics> {
    return this.http.get<ExamStatistics>(`${this.apiUrl}/exam/${examId}/statistics`);
  }

  /**
   * Eliminar resultado
   */
  deleteResult(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
