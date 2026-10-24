import { ChangeDetectionStrategy, Component, model } from '@angular/core';
import { MatSliderModule } from '@angular/material/slider';
import { GRADES, GRADE_LABEL, isWholeBelt } from '../../../core/models/question.models';

/** Índices de GRADES: [desde, hasta], ambos incluidos. */
export type GradeRange = [number, number];

export const FULL_GRADE_RANGE: GradeRange = [0, GRADES.length - 1];

/**
 * Barra de dos extremos para filtrar por grado (RF-43). Debajo, una regla con una marca
 * en cada cinturón y medio cinturón, y el nombre solo en los cinturones enteros.
 * El rango cambia al soltar un extremo, no mientras se arrastra.
 */
@Component({
  selector: 'app-grade-range',
  imports: [MatSliderModule],
  templateUrl: './grade-range.component.html',
  styleUrl: './grade-range.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GradeRangeComponent {
  range = model<GradeRange>(FULL_GRADE_RANGE);

  readonly max = GRADES.length - 1;
  readonly ticks = GRADES.map((grade, index) => ({
    grade,
    whole: isWholeBelt(index),
    label: isWholeBelt(index) ? GRADE_LABEL[grade] : null,
    left: `${(index / (GRADES.length - 1)) * 100}%`
  }));

  /** Etiqueta del extremo mientras se arrastra. */
  readonly gradeLabel = (index: number): string => GRADE_LABEL[GRADES[index]];

  setFrom(index: number): void {
    this.range.update(([, to]) => [index, to]);
  }

  setTo(index: number): void {
    this.range.update(([from]) => [from, index]);
  }
}
