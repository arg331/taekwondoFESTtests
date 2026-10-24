import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { ResultResponse } from '../../../../core/models/result.models';
import { PublicQuestionResponse } from '../../../../core/models/exam.models';
import { APP_NAME } from '../../../../core/app-brand';

interface AnswerRow {
  number: number;
  text: string;
  chosen: string | null;
  /** null cuando el alumno no recibe la correcta (RF-53). */
  correctOption: string | null;
  correct: boolean;
}

/**
 * Resultado al terminar. Si el examen oculta la nota, solo confirma la entrega.
 */
@Component({
  selector: 'app-exam-result-card',
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatDividerModule],
  templateUrl: './exam-result-card.component.html',
  styleUrl: './exam-result-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamResultCardComponent {
  readonly appName = APP_NAME;
  result = input.required<ResultResponse>();
  /** Preguntas en el orden en que las vio el alumno. */
  questions = input.required<PublicQuestionResponse[]>();

  passedColor = computed(() => (this.result().passed ? '#4caf50' : '#f44336'));

  /** Detalle con el texto de cada opción (las opciones pudieron mostrarse barajadas). */
  rows = computed<AnswerRow[]>(() => {
    const shown = this.questions();
    return (this.result().answers ?? [])
      .map(answer => {
        const position = shown.findIndex(q => q.id === answer.questionId);
        const question = shown[position];
        // Varias opciones se muestran separadas por comas; ninguna, null
        const optionTexts = (indexes: number[] | null) => !indexes?.length ? null : indexes
          .map(index => question?.options.find(o => o.index === index)?.text ?? '—').join(', ');
        return {
          number: position + 1,
          text: question?.text ?? '',
          chosen: optionTexts(answer.studentAnswers),
          // El alumno nunca recibe la correcta (RF-53): entonces no se muestra
          correctOption: optionTexts(answer.correctAnswers),
          correct: answer.correct
        };
      })
      .sort((a, b) => a.number - b.number);
  });
}
