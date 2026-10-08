import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { ResultResponse } from '../../../../core/models/result.models';
import { PublicQuestionResponse } from '../../../../core/models/exam.models';

interface AnswerRow {
  number: number;
  text: string;
  chosen: string | null;
  correctOption: string;
  correct: boolean;
}

/**
 * Resultado al terminar. Si el examen oculta la nota, solo confirma la entrega.
 */
@Component({
  selector: 'app-exam-result-card',
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatDividerModule],
  templateUrl: './exam-result-card.component.html',
  styleUrl: './exam-result-card.component.scss'
})
export class ExamResultCardComponent {
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
        const optionText = (index: number | null) =>
          index === null ? null : question?.options.find(o => o.index === index)?.text ?? '—';
        return {
          number: position + 1,
          text: question?.text ?? '',
          chosen: optionText(answer.studentAnswer),
          correctOption: optionText(answer.correctAnswer) ?? '—',
          correct: answer.correct
        };
      })
      .sort((a, b) => a.number - b.number);
  });
}
