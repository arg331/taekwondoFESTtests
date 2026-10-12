import { DIFFICULTIES, QuestionResponse } from '../../../../core/models/question.models';

/** Filtros del selector de preguntas que se recuerdan entre exámenes (la búsqueda no). */
export interface SavedQuestionFilters {
  /** Tags marcados; ninguno = todas las preguntas. */
  selectedTagIds: number[];
  /** «Sin tag» marcado: suma las preguntas sin tags. */
  untaggedSelected: boolean;
  /** Índices de DIFFICULTIES: [desde, hasta], ambos incluidos. */
  difficultyRange: [number, number];
}

export interface QuestionFilters extends SavedQuestionFilters {
  search: string;
}

export const DEFAULT_FILTERS: SavedQuestionFilters = {
  selectedTagIds: [],
  untaggedSelected: false,
  difficultyRange: [0, DIFFICULTIES.length - 1]
};

// v2: los tags pasaron de «desmarcados» a «marcados»; lo guardado antes no vale
const STORAGE_KEY = 'fest_exam_picker_filters_v2';

/**
 * Filtro de tags del banco y del selector: sin nada marcado pasan todas; si no, las que
 * tienen alguno de los tags marcados (y, con «Sin tag» marcado, las que no tienen tags).
 */
export function matchesTags(question: QuestionResponse, selectedTagIds: ReadonlySet<number>, untaggedSelected: boolean): boolean {
  if (selectedTagIds.size === 0 && !untaggedSelected) return true;
  if (question.tags.length === 0) return untaggedSelected;
  return question.tags.some(tag => selectedTagIds.has(tag.id));
}

/** Una pregunta se ve si cumple la búsqueda, su dificultad está en el rango y pasa el filtro de tags. */
export function matchesFilters(question: QuestionResponse, filters: QuestionFilters): boolean {
  const text = filters.search.toLowerCase().trim();
  if (text && !question.text.toLowerCase().includes(text)) return false;

  const level = DIFFICULTIES.indexOf(question.difficulty);
  const [from, to] = filters.difficultyRange;
  if (level < from || level > to) return false;

  return matchesTags(question, new Set(filters.selectedTagIds), filters.untaggedSelected);
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
