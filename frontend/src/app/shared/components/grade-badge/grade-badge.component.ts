import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { GRADE_LABEL, Grade, gradeBackground, gradeTextColor } from '../../../core/models/question.models';

/** Distintivo con el cinturón de una pregunta (RF-43); sin grado no muestra nada. */
@Component({
  selector: 'app-grade-badge',
  template: `@if (grade(); as g) {<span class="grade-badge" [style.background]="background()" [style.color]="textColor()">{{ label() }}</span>}`,
  styles: [`
    .grade-badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 12px;
      border: 1px solid rgba(0, 0, 0, 0.25);
      font-size: 0.75rem;
      font-weight: 600;
      letter-spacing: 0.03em;
      white-space: nowrap;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GradeBadgeComponent {
  grade = input.required<Grade | null>();

  label = computed(() => this.grade() ? GRADE_LABEL[this.grade()!] : '');
  background = computed(() => this.grade() ? gradeBackground(this.grade()!) : '');
  textColor = computed(() => this.grade() ? gradeTextColor(this.grade()!) : '');
}
