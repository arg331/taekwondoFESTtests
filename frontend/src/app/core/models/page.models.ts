/** Una página de un listado (RNF-09): sus elementos, cuál es (desde 0), su tamaño y el total. */
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}
