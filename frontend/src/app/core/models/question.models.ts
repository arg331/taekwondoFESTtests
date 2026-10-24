import { TagResponse } from './tag.models';

/** Grado de una pregunta: el cinturón mínimo desde el que es apropiada (RF-43). */
export type Grade =
  | 'BLANCO' | 'BLANCO_AMARILLO' | 'AMARILLO' | 'AMARILLO_VERDE' | 'VERDE' | 'VERDE_AZUL'
  | 'AZUL' | 'AZUL_ROJO' | 'ROJO' | 'ROJO_NEGRO' | 'NEGRO';

/** Los 11 grados en orden; las posiciones pares son cinturones enteros y las impares, medios. */
export const GRADES: readonly Grade[] = [
  'BLANCO', 'BLANCO_AMARILLO', 'AMARILLO', 'AMARILLO_VERDE', 'VERDE', 'VERDE_AZUL',
  'AZUL', 'AZUL_ROJO', 'ROJO', 'ROJO_NEGRO', 'NEGRO'
];

export const GRADE_LABEL: Record<Grade, string> = {
  BLANCO: 'Blanco',
  BLANCO_AMARILLO: 'Blanco-amarillo',
  AMARILLO: 'Amarillo',
  AMARILLO_VERDE: 'Amarillo-verde',
  VERDE: 'Verde',
  VERDE_AZUL: 'Verde-azul',
  AZUL: 'Azul',
  AZUL_ROJO: 'Azul-rojo',
  ROJO: 'Rojo',
  ROJO_NEGRO: 'Rojo-negro',
  NEGRO: 'Negro'
};

export function isWholeBelt(gradeIndex: number): boolean {
  return gradeIndex % 2 === 0;
}

/** Color del cinturón entero de cada posición par de GRADES. */
const BELT_COLORS = ['#fafafa', '#fbc02d', '#2e7d32', '#1565c0', '#c62828', '#212121'];

/** Fondo del distintivo: el cinturón; los medios llevan la punta del color siguiente. */
export function gradeBackground(grade: Grade): string {
  const index = GRADES.indexOf(grade);
  const belt = BELT_COLORS[Math.floor(index / 2)];
  if (isWholeBelt(index)) return belt;
  const tip = BELT_COLORS[Math.floor(index / 2) + 1];
  return `linear-gradient(90deg, ${belt} 0 70%, ${tip} 70% 100%)`;
}

/** Texto oscuro sobre los cinturones claros (blanco y amarillo). */
export function gradeTextColor(grade: Grade): string {
  return GRADES.indexOf(grade) < GRADES.indexOf('VERDE') ? '#333' : '#fff';
}

/** Tipo de una pregunta (RF-44): C, D o ninguno (null). */
export type QuestionType = 'C' | 'D';

export const QUESTION_TYPES: readonly QuestionType[] = ['C', 'D'];

export const QUESTION_TYPE_LABEL: Record<QuestionType, string> = { C: 'Tipo C', D: 'Tipo D' };

/** Filtro por tipo: todos (''), uno de los tipos, o las que no tienen tipo. */
export type QuestionTypeFilter = '' | QuestionType | 'NONE';

export const MIN_OPTIONS = 2;
/** Sin máximo para el profesor (RF-09); el servidor no admite más de 20. */
export const MAX_OPTIONS = 20;

export interface QuestionResponse {
  id: number;
  text: string;
  options: string[];
  /** Una o varias, de menor a mayor (RF-09). */
  correctAnswers: number[];
  explanation: string | null;
  /** Cinturón mínimo; opcional (null = sin grado). */
  grade: Grade | null;
  type: QuestionType | null;
  tags: TagResponse[];
  createdAt: string;
  updatedAt: string;
  /** Quién me la ha compartido (RF-57): solo puedo consultarla. null en las propias. */
  sharedBy: string | null;
  /** En cuántos exámenes míos está, borradores incluidos. null en las compartidas. */
  usedInExams: number | null;
  /** Cuándo la eliminé del banco; null si está en él. */
  deletedAt: string | null;
}

/** Cuerpo para crear y para editar una pregunta. */
export interface QuestionRequest {
  text: string;
  options: string[];
  /** Una o varias, de menor a mayor (RF-09). */
  correctAnswers: number[];
  explanation: string;
  /** Cinturón mínimo; opcional (null = sin grado). */
  grade: Grade | null;
  type: QuestionType | null;
  tagIds: number[];
}
