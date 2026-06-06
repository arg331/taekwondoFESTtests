export interface AnswerSubmissionRequest {
  questionId: number;
  chosenOption: number;
}

export interface SubmitExamRequest {
  examCode: string;
  studentName: string;
  studentClub: string | null;
  studentEmail: string | null;
  answers: AnswerSubmissionRequest[];
  timeSpentSeconds: number;
}

export interface AnswerResponse {
  questionId: number;
  studentAnswer: number;
  correctAnswer: number;
  correct: boolean;
}

export interface ResultResponse {
  id: number;
  examId: number;
  studentName: string;
  studentClub: string | null;
  studentEmail: string | null;
  answers: AnswerResponse[];
  correctAnswers: number;
  totalQuestions: number;
  score: number;
  passed: boolean;
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
