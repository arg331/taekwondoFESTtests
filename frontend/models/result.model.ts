// src/app/models/result.model.ts

export interface Result {
  id: number;
  examId: number;
  examCode: string;
  studentName: string;
  studentClub?: string;
  studentEmail?: string;
  score: number;
  correctAnswers: number;
  totalQuestions: number;
  timeSpent: number;
  completedAt: Date;
  answers?: AnswerDetail[];
}

export interface AnswerDetail {
  questionId: number;
  studentAnswer: number;
  correctAnswer: number;
  isCorrect: boolean;
}

export interface ExamStatistics {
  examId: number;
  totalAttempts: number;
  averageScore: number;
  passedCount: number;
  failedCount: number;
}
