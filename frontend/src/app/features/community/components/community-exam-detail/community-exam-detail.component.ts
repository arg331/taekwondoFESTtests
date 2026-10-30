import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommunityService } from '../../../../core/services/community.service';
import { CommunityExamResponse, CopyQuestionsResponse } from '../../../../core/models/community.models';

const MESSAGE_DURATION_MS = 5000;
const OPTION_LETTERS = 'ABCDEFGHIJKLMNOPQRST';

/**
 * Un examen de prueba de otro profesor: sus preguntas tal como se publicaron, con la solución, para elegir cuáles
 * copiar a mi banco (DE-12). Al abrir un examen están todas elegidas.
 */
@Component({
  selector: 'app-community-exam-detail',
  imports: [MatButtonModule, MatCheckboxModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './community-exam-detail.component.html',
  styleUrl: './community-exam-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CommunityExamDetailComponent {
  private communitySvc = inject(CommunityService);
  private snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly exam = input.required<CommunityExamResponse>();
  readonly letter = (index: number) => OPTION_LETTERS[index] ?? String(index + 1);

  private examId = computed(() => this.exam().id);
  readonly questions = rxResource({
    params: () => this.examId(),
    stream: ({ params: id }) => this.communitySvc.questions(id)
  });
  /** Las elegidas para copiar: todas al cargar las preguntas de un examen. */
  selected = linkedSignal<Set<number>>(() => new Set((this.questions.value() ?? []).map(q => q.questionId)));
  copying = signal(false);

  isSelected(questionId: number): boolean {
    return this.selected().has(questionId);
  }

  toggle(questionId: number): void {
    this.selected.update(current => {
      const next = new Set(current);
      if (!next.delete(questionId)) next.add(questionId);
      return next;
    });
  }

  selectAll(): void {
    this.selected.set(new Set((this.questions.value() ?? []).map(q => q.questionId)));
  }

  selectNone(): void {
    this.selected.set(new Set());
  }

  copy(): void {
    // En el orden del examen, no en el que se marcaron
    const ids = (this.questions.value() ?? []).map(q => q.questionId).filter(id => this.isSelected(id));
    this.copying.set(true);
    this.communitySvc.copyQuestions(this.examId(), ids).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => {
        this.copying.set(false);
        this.snackBar.open(copyMessage(result), undefined, { duration: MESSAGE_DURATION_MS });
      },
      error: err => {
        this.copying.set(false);
        this.snackBar.open(err?.error?.message ?? 'No se pudieron copiar las preguntas', undefined,
          { duration: MESSAGE_DURATION_MS });
      }
    });
  }
}

/** «2 preguntas copiadas a tu banco; 1 ya la tenías». */
export function copyMessage({ copied, alreadyInBank }: CopyQuestionsResponse): string {
  const copiedText = copied === 0 ? 'No se ha copiado ninguna pregunta'
    : copied === 1 ? '1 pregunta copiada a tu banco' : `${copied} preguntas copiadas a tu banco`;
  if (alreadyInBank === 0) return copiedText;
  return `${copiedText}; ${alreadyInBank} ya ${alreadyInBank === 1 ? 'la tenías' : 'las tenías'}`;
}
