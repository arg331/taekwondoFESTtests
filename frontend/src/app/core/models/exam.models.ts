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
/**
 * Público = examen de prueba: aparece en «Exámenes de prueba» para practicar.
 * Privado = examen oficial: solo se entra con el código o el QR (RF-49).
 * Intentos y corrección siguen las mismas reglas en los dos.
 */
export const VISIBILITY_LABEL: Record<Visibility, string> = {
  PUBLIC: 'De prueba', PRIVATE: 'Oficial'
};
export const VISIBILITY_ICON: Record<Visibility, string> = {
  PUBLIC: 'school', PRIVATE: 'verified'
};
export const VISIBILITY_COLOR: Record<Visibility, string> = {
  PUBLIC: '#757575', PRIVATE: '#2e7d32'
};

/** Un examen se publica con las preguntas elegidas, entre estos límites (RF-46). */
export const EXAM_MIN_QUESTIONS = 5;
export const EXAM_MAX_QUESTIONS = 50;

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

/**
 * Pregunta tal como se publicó en un examen (DE-03): es lo que ven y contestan los alumnos,
 * aunque después se edite en el banco. editedSincePublished avisa de que el banco ya no coincide.
 */
export interface PublishedQuestionResponse {
  questionId: number;
  text: string;
  options: string[];
  correctAnswer: number;
  explanation: string | null;
  editedSincePublished: boolean;
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
  /** Abierto hasta que se cierre a mano (RF-48); no se combina con expiresAt. */
  neverExpires?: boolean;
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
