// src/app/services/question.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { QuestionDTO } from '../models/question.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class QuestionService {
  private apiUrl = `${environment.apiUrl}/questions`;

  constructor(private http: HttpClient) {}

  /**
   * Obtener todas las preguntas
   */
  getAllQuestions(): Observable<QuestionDTO[]> {
    return this.http.get<QuestionDTO[]>(this.apiUrl);
  }

  /**
   * Obtener pregunta por ID
   */
  getQuestionById(id: number): Observable<QuestionDTO> {
    return this.http.get<QuestionDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Obtener preguntas por categoría
   */
  getQuestionsByCategory(category: string): Observable<QuestionDTO[]> {
    return this.http.get<QuestionDTO[]>(`${this.apiUrl}/category/${category}`);
  }

  /**
   * Buscar preguntas por texto
   */
  searchQuestions(text: string): Observable<QuestionDTO[]> {
    return this.http.get<QuestionDTO[]>(`${this.apiUrl}/search`, {
      params: { text }
    });
  }

  /**
   * Crear nueva pregunta
   */
  createQuestion(question: QuestionDTO): Observable<QuestionDTO> {
    return this.http.post<QuestionDTO>(this.apiUrl, question);
  }

  /**
   * Actualizar pregunta existente
   */
  updateQuestion(id: number, question: QuestionDTO): Observable<QuestionDTO> {
    return this.http.put<QuestionDTO>(`${this.apiUrl}/${id}`, question);
  }

  /**
   * Eliminar pregunta
   */
  deleteQuestion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Contar preguntas totales
   */
  countQuestions(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/count`);
  }

  /**
   * Contar preguntas por categoría
   */
  countQuestionsByCategory(category: string): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/count/category/${category}`);
  }
}
