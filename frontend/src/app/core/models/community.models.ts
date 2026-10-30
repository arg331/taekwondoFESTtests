import { ExamStatus } from './exam.models';

/** Examen de prueba (público) de otro profesor (RF-30). */
export interface CommunityExamResponse {
  id: number;
  title: string;
  code: string;
  ownerName: string;
  questionCount: number;
  /** PUBLISHED abierto; EXPIRED cerrado: sus preguntas se pueden copiar igual. */
  status: ExamStatus;
  createdAt: string;
  expiresAt: string | null;
  /** Si lo tengo en favoritos (RF-31). */
  favorite: boolean;
}

/** Pregunta tal como se publicó en ese examen, con su solución (DE-12). */
export interface CommunityQuestionResponse {
  questionId: number;
  text: string;
  options: string[];
  correctAnswers: number[];
  explanation: string | null;
}

/** Cuántas han pasado a mi banco y cuántas no, porque ya tenía una igual. */
export interface CopyQuestionsResponse {
  copied: number;
  alreadyInBank: number;
}
