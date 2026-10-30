export const environment = {
  production: true,
  /** El backend, en el mismo dominio que la app, detrás del servidor web. */
  apiUrl: '/api',
  /**
   * Dirección pública de la app para el QR y el enlace del examen (DE-16), p. ej. 'https://examenes.ejemplo.es'.
   * Vacía = la del navegador del profesor, que vale si solo se entra por el dominio público.
   */
  publicUrl: ''
};
