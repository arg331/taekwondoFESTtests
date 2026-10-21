export interface SubmitExamRequest {
  examCode: string;
  attemptToken: string;
  /** chosenOption null = sin responder. */
  answers: { questionId: number; chosenOption: number | null }[];
}

export interface AnswerResponse {
  questionId: number;
  studentAnswer: number | null;
  /** null en la vista del alumno: nunca ve cuál era la correcta (RF-53). */
  correctAnswer: number | null;
  correct: boolean;
}

/**
 * Si el examen oculta la nota y quien consulta es el alumno,
 * scoreVisible es false y score/passed/correctAnswers/answers llegan a null.
 */
export interface ResultResponse {
  id: number;
  examId: number;
  examTitle: string | null;
  studentName: string;
  studentClub: string | null;
  studentEmail: string | null;
  /** La entrega la hizo un usuario con cuenta (no un anónimo). */
  registered: boolean;
  /** Qué alumno la hizo, para agrupar sus intentos (RF-54). */
  studentKey: string;
  scoreVisible: boolean;
  answers: AnswerResponse[] | null;
  correctAnswers: number | null;
  totalQuestions: number;
  score: number | null;
  passed: boolean | null;
  timeSpentSeconds: number;
  completedAt: string;
}

export interface ExamStatisticsResponse {
  examId: number;
  /** Entregas; el resto de cifras es por alumno, con su mejor nota (RF-54). */
  totalAttempts: number;
  totalStudents: number;
  averageScore: number;
  passedCount: number;
  failedCount: number;
  highestScore: number;
  lowestScore: number;
}
