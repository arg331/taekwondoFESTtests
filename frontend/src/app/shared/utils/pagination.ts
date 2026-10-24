/** Elementos por página en los listados (RNF-09). */
export const PAGE_SIZE = 20;

/** La página que se puede enseñar: si la lista se ha quedado más corta, la última que queda. */
export function clampPage(page: number, length: number, size = PAGE_SIZE): number {
  return Math.min(page, Math.max(0, Math.ceil(length / size) - 1));
}

/** Los elementos de esa página (desde 0). */
export function pageItems<T>(items: readonly T[], page: number, size = PAGE_SIZE): T[] {
  return items.slice(page * size, (page + 1) * size);
}
