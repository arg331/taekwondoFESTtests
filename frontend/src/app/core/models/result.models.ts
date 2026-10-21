export interface SubmitExamRequest {
  examCode: string;
  attemptToken: string;
  /** Opciones marcadas; vacío = sin responder. */
  answers: { questionId: number; chosenOptions: number[] }[];
}

export interface AnswerResponse {
  questionId: number;
  /** Opciones que marcó; vacío = en blanco. */
  studentAnswers: number[];
  /** null en la vista del alumno: nunca ve cuáles eran las correctas (RF-53). */
  correctAnswers: number[] | null;
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
  /** Su cuenta, para ver todos sus resultados (RF-55); null si es anónimo. */
  studentUserId: number | null;
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
  /** Se corrigió sola al acabarse el plazo, sin que el alumno la entregara (RF-24). */
  autoSubmitted: boolean;
}

/** Cómo se ha respondido una pregunta en todas las entregas de un examen (RF-28). */
export interface QuestionStatsResponse {
  questionId: number;
  text: string;
  options: string[];
  correctAnswers: number[];
  submissions: number;
  correct: number;
  blank: number;
  /** optionCounts[i]: entregas que marcaron la opción i. */
  optionCounts: number[];
  /** % de acierto redondeado; null si nadie ha entregado. */
  successRate: number | null;
}

/** Un usuario y sus entregas, la más reciente primero (RF-55). */
export interface UserResultsResponse {
  userId: number;
  username: string;
  displayName: string;
  clubName: string | null;
  results: ResultResponse[];
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
