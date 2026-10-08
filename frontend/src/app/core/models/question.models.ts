import { TagResponse } from './tag.models';

export type Difficulty = 'FACIL' | 'MEDIO' | 'DIFICIL';

export const DIFFICULTIES: Difficulty[] = ['FACIL', 'MEDIO', 'DIFICIL'];

export const DIFFICULTY_LABEL: Record<Difficulty, string> = {
  FACIL: 'Fácil',
  MEDIO: 'Medio',
  DIFICIL: 'Difícil'
};

export const DIFFICULTY_COLOR: Record<Difficulty, string> = {
  FACIL: '#4caf50',
  MEDIO: '#ff9800',
  DIFICIL: '#f44336'
};

export const MIN_OPTIONS = 2;
export const MAX_OPTIONS = 4;

export interface QuestionResponse {
  id: number;
  text: string;
  options: string[];
  correctAnswer: number;
  explanation: string | null;
  difficulty: Difficulty;
  tags: TagResponse[];
  createdAt: string;
  updatedAt: string;
}

/** Cuerpo para crear y para editar una pregunta. */
export interface QuestionRequest {
  text: string;
  options: string[];
  correctAnswer: number;
  explanation: string;
  difficulty: Difficulty;
  tagIds: number[];
}
