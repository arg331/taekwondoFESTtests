import { DIFFICULTIES, QuestionResponse } from '../../../../core/models/question.models';

/** Filtros del selector de preguntas que se recuerdan entre exámenes (la búsqueda no). */
export interface SavedQuestionFilters {
  /** Tags desmarcados: así un tag nuevo aparece marcado por defecto. */
  hiddenTagIds: number[];
  showUntagged: boolean;
  /** Índices de DIFFICULTIES: [desde, hasta], ambos incluidos. */
  difficultyRange: [number, number];
}

export interface QuestionFilters extends SavedQuestionFilters {
  search: string;
}

export const DEFAULT_FILTERS: SavedQuestionFilters = {
  hiddenTagIds: [],
  showUntagged: true,
  difficultyRange: [0, DIFFICULTIES.length - 1]
};

const STORAGE_KEY = 'fest_exam_picker_filters';

/**
 * Una pregunta se ve si cumple la búsqueda, su dificultad está en el rango y tiene
 * algún tag marcado (o, si no tiene tags, si «Sin tag» está marcado).
 */
export function matchesFilters(question: QuestionResponse, filters: QuestionFilters): boolean {
  const text = filters.search.toLowerCase().trim();
  if (text && !question.text.toLowerCase().includes(text)) return false;

  const level = DIFFICULTIES.indexOf(question.difficulty);
  const [from, to] = filters.difficultyRange;
  if (level < from || level > to) return false;

  if (question.tags.length === 0) return filters.showUntagged;
  return question.tags.some(tag => !filters.hiddenTagIds.includes(tag.id));
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
