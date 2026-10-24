import { ChangeDetectionStrategy, Component, computed, model } from '@angular/core';
import { MatSliderModule } from '@angular/material/slider';
import { EXAM_MAX_ATTEMPTS } from '../../../../core/models/exam.models';

/** Posición de la barra que significa «sin límite»: justo después del máximo con número. */
const UNLIMITED_POSITION = EXAM_MAX_ATTEMPTS + 1;

/**
 * Intentos por alumno (RF-52) con una barra de cinco posiciones: 1, 2, 3, 4 o sin límite (∞).
 * value es el número de intentos, o null = sin límite.
 */
@Component({
  selector: 'app-attempts-slider',
  imports: [MatSliderModule],
  templateUrl: './attempts-slider.component.html',
  styleUrl: './attempts-slider.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttemptsSliderComponent {
  value = model<number | null>(1);

  readonly maxPosition = UNLIMITED_POSITION;
  readonly marks = [...Array.from({ length: EXAM_MAX_ATTEMPTS }, (_, i) => String(i + 1)), '∞'];
  position = computed(() => this.value() ?? UNLIMITED_POSITION);

  readonly display = (position: number): string => position === UNLIMITED_POSITION ? '∞' : String(position);

  setPosition(position: number): void {
    this.value.set(position === UNLIMITED_POSITION ? null : position);
  }
}
