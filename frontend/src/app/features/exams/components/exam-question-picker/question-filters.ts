import { GRADES, QuestionResponse, QuestionTypeFilter } from '../../../../core/models/question.models';
import { FULL_GRADE_RANGE, GradeRange } from '../../../../shared/components/grade-range/grade-range.component';

/** Filtros del selector de preguntas que se recuerdan entre exámenes (la búsqueda no). */
export interface SavedQuestionFilters {
  /** Tags marcados; ninguno = todas las preguntas. */
  selectedTagIds: number[];
  /** Índices de GRADES: [desde, hasta], ambos incluidos. */
  gradeRange: GradeRange;
  typeFilter: QuestionTypeFilter;
}

export interface QuestionFilters extends SavedQuestionFilters {
  search: string;
}

export const DEFAULT_FILTERS: SavedQuestionFilters = {
  selectedTagIds: [],
  gradeRange: FULL_GRADE_RANGE,
  typeFilter: ''
};

// v3: la dificultad pasó a grado (RF-43) y se añadió el tipo (RF-44); lo guardado antes no vale
const STORAGE_KEY = 'fest_exam_picker_filters_v3';

/**
 * Filtro de tags del banco y del selector: sin nada marcado pasan todas; si no, las que
 * tienen alguno de los tags marcados.
 */
export function matchesTags(question: QuestionResponse, selectedTagIds: ReadonlySet<number>): boolean {
  return selectedTagIds.size === 0 || question.tags.some(tag => selectedTagIds.has(tag.id));
}

/** El grado de la pregunta está entre los dos extremos de la barra, ambos incluidos. */
export function matchesGrade(question: QuestionResponse, [from, to]: GradeRange): boolean {
  const index = GRADES.indexOf(question.grade);
  return index >= from && index <= to;
}

/** Todas (''), las de un tipo, o las que no tienen tipo ('NONE'). */
export function matchesType(question: QuestionResponse, filter: QuestionTypeFilter): boolean {
  if (!filter) return true;
  return filter === 'NONE' ? question.type === null : question.type === filter;
}

/** Una pregunta se ve si cumple la búsqueda, el grado, el tipo y el filtro de tags. */
export function matchesFilters(question: QuestionResponse, filters: QuestionFilters): boolean {
  const text = filters.search.toLowerCase().trim();
  if (text && !question.text.toLowerCase().includes(text)) return false;
  return matchesGrade(question, filters.gradeRange)
    && matchesType(question, filters.typeFilter)
    && matchesTags(question, new Set(filters.selectedTagIds));
}

/** Cuántas preguntas tienen cada tag (id → número). */
export function countByTag(questions: QuestionResponse[]): Map<number, number> {
  const counts = new Map<number, number>();
  for (const question of questions) {
    for (const tag of question.tags) counts.set(tag.id, (counts.get(tag.id) ?? 0) + 1);
  }
  return counts;
}

/** Hasta n elementos distintos elegidos al azar (Fisher-Yates parcial). */
export function pickRandom<T>(items: readonly T[], n: number, random: () => number = Math.random): T[] {
  const pool = [...items];
  const count = Math.min(n, pool.length);
  for (let i = 0; i < count; i++) {
    const j = i + Math.floor(random() * (pool.length - i));
    [pool[i], pool[j]] = [pool[j], pool[i]];
  }
  return pool.slice(0, count);
}

/** Filtros guardados en este navegador; si no hay o no se pueden leer, los de por defecto. */
export function loadSavedFilters(): SavedQuestionFilters {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') as Partial<SavedQuestionFilters> | null;
    return { ...DEFAULT_FILTERS, ...saved };
  } catch {
    return DEFAULT_FILTERS;
  }
}

export function saveFilters(filters: SavedQuestionFilters): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(filters));
  } catch {
    // Sin almacenamiento (modo privado, cuota llena…): los filtros simplemente no se recuerdan
  }
}
