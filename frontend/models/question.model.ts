// src/app/models/question.model.ts

export interface Question {
  id?: number;
  category: 'PUNTUACION' | 'REGLAMENTO' | 'PENALIZACIONES' | 'SITUACIONES_ESPECIALES';
  difficulty: 'FACIL' | 'MEDIO' | 'DIFICIL';
  text: string;
  options: string[];
  correctAnswer: number;
  explanation?: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface QuestionDTO {
  id?: number;
  category: string;
  difficulty: string;
  text: string;
  options: string[];
  correctAnswer: number;
  explanation?: string;
  createdAt?: string;
  updatedAt?: string;
}
