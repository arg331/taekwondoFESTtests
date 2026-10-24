import { Injectable, signal } from '@angular/core';

export const QR_DEFAULT_COLOR = '#000000';

/** Paleta para distinguir QR en el aula: colores con nombre fácil de decir y que se leen bien sobre blanco. */
export const QR_COLOR_PRESETS: readonly { name: string; value: string }[] = [
  { name: 'Negro', value: '#000000' },
  { name: 'Rojo', value: '#c62828' },
  { name: 'Azul', value: '#0d47a1' },
  { name: 'Verde', value: '#1b5e20' },
  { name: 'Morado', value: '#6a1b9a' },
  { name: 'Marrón', value: '#5d4037' }
];

const STORAGE_KEY = 'qr-colors';
const HEX_COLOR = /^#[0-9a-f]{6}$/i;
/** Contraste mínimo con el fondo blanco para que los lectores distingan los módulos del QR. */
const MIN_SCAN_CONTRAST = 4.5;
/** Exámenes cuyo color se recuerda; los más antiguos se olvidan. */
const MAX_REMEMBERED = 200;

type ColorsByExam = Record<string, string>;

/**
 * Color del QR de cada examen (RF-50), para que en el aula se sepa cuál escanear
 * («escanead el rojo»). Se guarda en este navegador, por código de examen.
 */
@Injectable({ providedIn: 'root' })
export class QrColorService {
  private readonly colors = signal<ColorsByExam>(loadColors());

  /** Reactivo: dentro de un computed o un resource, se recalcula al cambiar el color. */
  colorOf(examCode: string): string {
    return this.colors()[examCode] ?? QR_DEFAULT_COLOR;
  }

  setColor(examCode: string, color: string): void {
    if (!HEX_COLOR.test(color)) return;
    // Se quita y se vuelve a añadir para que cuente como el más reciente
    const others = Object.entries(this.colors()).filter(([code]) => code !== examCode);
    const updated = forgetOldest(Object.fromEntries([...others, [examCode, color.toLowerCase()]]));
    this.colors.set(updated);
    saveColors(updated);
  }

  /** Al publicar: si el examen no tiene color, el de la paleta menos usado por los otros exámenes abiertos. */
  assignDistinct(examCode: string, openExamCodes: readonly string[]): void {
    if (this.colors()[examCode]) return;
    const used = openExamCodes.filter(code => code !== examCode).map(code => this.colorOf(code));
    const timesUsed = (color: string) => used.filter(c => c === color).length;
    const leastUsed = QR_COLOR_PRESETS.reduce((best, p) => (timesUsed(p.value) < timesUsed(best.value) ? p : best));
    this.setColor(examCode, leastUsed.value);
  }
}

/** Relación de contraste WCAG con el blanco por debajo de lo que leen bien los móviles. */
export function isLowContrast(hex: string): boolean {
  const [r, g, b] = [1, 3, 5].map(i => toLinear(parseInt(hex.slice(i, i + 2), 16) / 255));
  const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  return 1.05 / (luminance + 0.05) < MIN_SCAN_CONTRAST;
}

function toLinear(channel: number): number {
  return channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
}

function forgetOldest(colors: ColorsByExam): ColorsByExam {
  const entries = Object.entries(colors);
  return entries.length > MAX_REMEMBERED ? Object.fromEntries(entries.slice(-MAX_REMEMBERED)) : colors;
}

function loadColors(): ColorsByExam {
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}');
    if (typeof parsed !== 'object' || parsed === null) return {};
    return Object.fromEntries(Object.entries(parsed).filter(([, color]) => typeof color === 'string' && HEX_COLOR.test(color)));
  } catch {
    // Almacenamiento bloqueado o datos corruptos: se empieza sin colores
    return {};
  }
}

function saveColors(colors: ColorsByExam): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(colors));
  } catch {
    // Sin almacenamiento (modo privado, bloqueado): los colores valen solo para esta visita
  }
}
