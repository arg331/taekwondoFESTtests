import { TagResponse } from './tag.models';

export type Difficulty = 'FACIL' | 'MEDIO' | 'DIFICIL';

export interface QuestionResponse {
  id: number;
  text: string;
  options: string[];
  correctAnswer: number;
  explanation: string;
  difficulty: Difficulty;
  tags: TagResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateQuestionRequest {
  text: string;
  options: string[];
  correctAnswer: number;
  explanation: string;
  difficulty: Difficulty;
  tagIds: number[];
}

export interface EditQuestionRequest {
  text: string;
  options: string[];
  correctAnswer: number;
  explanation: string;
  difficulty: Difficulty;
  tagIds: number[];
}
