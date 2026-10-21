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
/** Intentos por alumno que puede fijar el profesor al publicar (RF-52). */
export const EXAM_MIN_ATTEMPTS = 1;
export const EXAM_MAX_ATTEMPTS = 4;

export const EXAM_STATUS_ICON: Record<ExamStatus, string> = {
  DRAFT: 'edit_note', PUBLISHED: 'check_circle', EXPIRED: 'schedule'
};

export interface ExamConfig {
  numberOfQuestions: number;
  timeLimitMinutes: number | null;
  showScore: boolean;
  /** Con la nota, el alumno ve sus respuestas bien o mal (nunca la correcta, RF-53). */
  showAnswers: boolean;
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
  /** Intentos por alumno (RF-52); null = sin límite. */
  maxAttempts: number | null;
}

/**
 * Pregunta tal como se publicó en un examen (DE-03): es lo que ven y contestan los alumnos,
 * aunque después se edite en el banco. editedSincePublished avisa de que el banco ya no coincide.
 */
export interface PublishedQuestionResponse {
  questionId: number;
  text: string;
  options: string[];
  correctAnswers: number[];
  explanation: string | null;
  editedSincePublished: boolean;
}

/** Qué ve el alumno al entregar (RF-53). */
export type ResultDisplay = 'NOTHING' | 'SCORE' | 'SCORE_AND_ANSWERS';

export const RESULT_DISPLAYS: readonly ResultDisplay[] = ['NOTHING', 'SCORE', 'SCORE_AND_ANSWERS'];

export const RESULT_DISPLAY_LABEL: Record<ResultDisplay, string> = {
  NOTHING: 'Nada',
  SCORE: 'Nota',
  SCORE_AND_ANSWERS: 'Nota y respuestas'
};

/** Cómo lo dice el resumen del examen. */
export const RESULT_DISPLAY_SUMMARY: Record<ResultDisplay, string> = {
  NOTHING: 'No muestra nada',
  SCORE: 'Muestra solo la nota',
  SCORE_AND_ANSWERS: 'Muestra nota y respuestas'
};

export function resultDisplayOf(config: Pick<ExamConfig, 'showScore' | 'showAnswers'>): ResultDisplay {
  if (!config.showScore) return 'NOTHING';
  return config.showAnswers ? 'SCORE_AND_ANSWERS' : 'SCORE';
}

export function resultDisplayFlags(display: ResultDisplay): Pick<ExamConfig, 'showScore' | 'showAnswers'> {
  return { showScore: display !== 'NOTHING', showAnswers: display === 'SCORE_AND_ANSWERS' };
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
  /** Intentos por alumno (RF-52): de 1 a 4, o unlimitedAttempts; sin ninguno, 1. */
  maxAttempts?: number;
  unlimitedAttempts?: boolean;
}

/** Cambiar los intentos de un examen publicado: un número de 1 a 4 o sin límite. */
export type ChangeAttemptsRequest = { maxAttempts: number } | { unlimitedAttempts: true };

/** Cuerpo para pedir n intentos, o sin límite si es null. */
export function attemptsRequest(maxAttempts: number | null): ChangeAttemptsRequest {
  return maxAttempts === null ? { unlimitedAttempts: true } : { maxAttempts };
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
  /** Tiene varias correctas: hay que marcarlas todas (RF-09). */
  multipleCorrect: boolean;
}

/** El alumno se identifica al empezar (DE-04); la entrega queda a su nombre. */
export interface StartAttemptRequest {
  studentName: string;
  studentClub: string | null;
  studentEmail: string | null;
}

export interface ExamAttemptResponse {
  attemptToken: string;
  questions: PublicQuestionResponse[];
  /** Qué intento es, de cuántos tiene el alumno (RF-52). */
  attemptNumber: number;
  /** null = sin límite. */
  maxAttempts: number | null;
}
