import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { QUESTION_TYPE_LABEL, QuestionResponse } from '../../../../core/models/question.models';
import { GradeBadgeComponent } from '../../../../shared/components/grade-badge/grade-badge.component';

/** Tarjeta cuadrada con todo a la vista, o fila de lista que despliega opciones y explicación. */
export type QuestionCardVariant = 'card' | 'row';

@Component({
  selector: 'app-question-card',
  imports: [NgTemplateOutlet, MatCardModule, MatButtonModule, MatIconModule, MatTooltipModule, GradeBadgeComponent],
  templateUrl: './question-card.component.html',
  styleUrl: './question-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionCardComponent {
  question = input.required<QuestionResponse>();
  variant = input<QuestionCardVariant>('card');
  edit = output<void>();
  remove = output<void>();
  /** Solo en las eliminadas: vuelve al banco. */
  restore = output<void>();

  /** Solo en la fila: si se ven las opciones y la explicación. */
  expanded = signal(false);

  readonly typeLabel = QUESTION_TYPE_LABEL;
}
