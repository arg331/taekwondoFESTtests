import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ResultService } from '../../../../core/services/result.service';
import { QuestionStatsResponse } from '../../../../core/models/result.models';

/** Una pregunta con su número en el examen («P3»), para encontrarla aunque la lista se reordene. */
interface QuestionRow extends QuestionStatsResponse {
  number: number;
}

/** Sin entregas va al final: detrás de la que tenga un 100 %. */
const NO_RATE = 101;

/**
 * Acierto por pregunta de un examen (RF-28): de la menos a la más acertada, para detectar preguntas
 * mal planteadas. Al pinchar una se ve cuántos marcaron cada opción y cuántos la dejaron en blanco.
 */
@Component({
  selector: 'app-question-stats',
  imports: [MatIconModule, MatProgressSpinnerModule],
  templateUrl: './question-stats.component.html',
  styleUrl: './question-stats.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionStatsComponent {
  private resultSvc = inject(ResultService);

  examId = input.required<number>();

  readonly stats = rxResource({
    params: () => this.examId(),
    stream: ({ params: examId }) => this.resultSvc.getQuestionStats(examId)
  });

  rows = computed<QuestionRow[]>(() => {
    if (!this.stats.hasValue()) return [];
    return this.stats.value()
      .map((question, i) => ({ ...question, number: i + 1 }))
      .sort((a, b) => (a.successRate ?? NO_RATE) - (b.successRate ?? NO_RATE) || a.number - b.number);
  });

  /** Pregunta desplegada; solo una a la vez. */
  expandedId = signal<number | null>(null);

  toggle(questionId: number): void {
    this.expandedId.update(current => current === questionId ? null : questionId);
  }

  isCorrect(row: QuestionRow, option: number): boolean {
    return row.correctAnswers.includes(option);
  }

  percent(count: number, total: number): number {
    return total ? Math.round(count * 100 / total) : 0;
  }

  letter(option: number): string {
    return String.fromCharCode('A'.charCodeAt(0) + option);
  }
}
