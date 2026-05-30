// src/app/models/exam.model.ts

export interface Exam {
  id?: number;
  code: string;
  numberOfQuestions: number;
  showScore: boolean;
  timeLimit: number;
  randomizeOptions: boolean;
  questionIds: number[];
  active: boolean;
  createdAt?: Date;
  expiresAt?: Date;
}

export interface ExamConfig {
  numberOfQuestions: number;
  showScore: boolean;
  timeLimit: number;
  randomizeOptions: boolean;
}

export interface ExamResponse {
  examId: number;
  code: string;
  numberOfQuestions: number;
  timeLimit: number;
  showScore: boolean;
  questions: any[]; // QuestionDTO[]
}

export interface ExamSubmission {
  studentName: string;
  studentClub?: string;
  studentEmail?: string;
  answers: ExamAnswer[];
  timeSpent: number;
}

export interface ExamAnswer {
  questionId: number;
  studentAnswer: number;
}
