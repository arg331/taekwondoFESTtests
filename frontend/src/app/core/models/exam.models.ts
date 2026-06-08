import { TagResponse } from './tag.models';

export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'EXPIRED';
export type Visibility = 'PRIVATE' | 'PUBLIC';
export type AccessMode = 'OPEN' | 'REGISTERED_ONLY';

export interface ExamConfigResponse {
  numberOfQuestions: number;
  timeLimitMinutes: number | null;
  showScore: boolean;
  randomizeOptions: boolean;
  randomizeQuestionOrder: boolean;
}

export interface ExamResponse {
  id: number;
  title: string;
  ownerId: number;
  status: ExamStatus;
  visibility: Visibility;
  accessMode: AccessMode;
  config: ExamConfigResponse;
  questionIds: number[];
  generationTags: TagResponse[];
  code: string | null;
  createdAt: string;
  expiresAt: string | null;
}

export interface CreateExamDraftRequest {
  title: string;
  numberOfQuestions: number;
  timeLimitMinutes: number | null;
  showScore: boolean;
  randomizeOptions: boolean;
  randomizeQuestionOrder: boolean;
}

export interface PreGenerateExamDraftRequest {
  title: string;
  numberOfQuestions: number;
  timeLimitMinutes: number | null;
  showScore: boolean;
  randomizeOptions: boolean;
  randomizeQuestionOrder: boolean;
  requiredAnyOfTagIds: number[];
}

export interface ChangeExamConfigRequest {
  numberOfQuestions: number;
  timeLimitMinutes: number | null;
  showScore: boolean;
  randomizeOptions: boolean;
  randomizeQuestionOrder: boolean;
}

export interface PublishExamRequest {
  visibility: Visibility;
  expiresAt: string | null;
}

export interface RenameExamRequest {
  newTitle: string;
}

export interface UpdateExamQuestionsRequest {
  questionIds: number[];
}

export interface ChangeExamVisibilityRequest {
  newVisibility: Visibility;
}

export interface ExtendExamExpirationRequest {
  newExpiresAt: string | null;
}

export interface ReopenExamRequest {
  newExpiresAt: string | null;
}

export interface PublicQuestionResponse {
  id: number;
  text: string;
  options: string[];
}

export interface PublicQuestionResponse {
  id: number;
  text: string;
  options: string[];
}
