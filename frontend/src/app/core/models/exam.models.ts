import { TagResponse } from './tag.models';

export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'EXPIRED';
export type Visibility = 'PRIVATE' | 'PUBLIC';
export type AccessMode = 'OPEN' | 'REGISTERED_ONLY';

export const EXAM_STATUS_LABEL: Record<ExamStatus, string> = {
  DRAFT: 'Borrador', PUBLISHED: 'Publicado', EXPIRED: 'Cerrado'
};
export const EXAM_STATUS_COLOR: Record<ExamStatus, string> = {
  DRAFT: '#ff9800', PUBLISHED: '#4caf50', EXPIRED: '#9e9e9e'
};
export const EXAM_STATUS_ICON: Record<ExamStatus, string> = {
  DRAFT: 'edit_note', PUBLISHED: 'check_circle', EXPIRED: 'schedule'
};

export interface ExamConfig {
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
  config: ExamConfig;
  questionIds: number[];
  generationTags: TagResponse[];
  code: string | null;
  createdAt: string;
  expiresAt: string | null;
}

/** Crear borrador vacío o pre-generado (requiredAnyOfTagIds solo aplica al pre-generado). */
export interface ExamDraftRequest extends ExamConfig {
  title: string;
  requiredAnyOfTagIds?: number[];
}

export interface PublishExamRequest {
  visibility: Visibility;
  accessMode: AccessMode;
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

/** Reabrir o ampliar un examen. null = sin expiración. */
export interface ExpirationRequest {
  newExpiresAt: string | null;
}

/** Pregunta tal como la ve el alumno: cada opción lleva su índice original. */
export interface PublicQuestionResponse {
  id: number;
  text: string;
  options: { index: number; text: string }[];
}

export interface ExamAttemptResponse {
  attemptToken: string;
  questions: PublicQuestionResponse[];
}
