const CODE_PREFIX = 'EXM-';
/** Formato de los códigos que genera el backend: EXM- y 8 caracteres hexadecimales en mayúsculas. */
const CODE_PATTERN = /^EXM-[0-9A-F]{8}$/;

/**
 * Convierte lo que escribe o pega el alumno en un código de examen válido, o null si no lo es.
 * Acepta minúsculas, espacios, el código sin el prefijo EXM- y el enlace completo del examen.
 */
export function normalizeExamCode(input: string): string | null {
  const lastSegment = input.trim().split('/').filter(Boolean).pop() ?? '';
  const compact = lastSegment.replace(/\s+/g, '').toUpperCase();
  const withoutPrefix = compact.startsWith(CODE_PREFIX)
    ? compact.slice(CODE_PREFIX.length)
    : compact.replace(/^EXM/, '');
  const code = CODE_PREFIX + withoutPrefix;
  return CODE_PATTERN.test(code) ? code : null;
}
