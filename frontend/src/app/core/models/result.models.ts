export interface SubmitExamRequest {
  examCode: string;
  attemptToken: string;
  studentName: string;
  studentClub: string | null;
  studentEmail: string | null;
  /** chosenOption null = sin responder. */
  answers: { questionId: number; chosenOption: number | null }[];
}

export interface AnswerResponse {
  questionId: number;
  studentAnswer: number | null;
  correctAnswer: number;
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
  totalAttempts: number;
  averageScore: number;
  passedCount: number;
  failedCount: number;
  highestScore: number;
  lowestScore: number;
}
