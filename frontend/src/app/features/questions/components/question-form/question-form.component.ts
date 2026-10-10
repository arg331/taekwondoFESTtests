import { ChangeDetectionStrategy, Component, computed, effect, inject, input, linkedSignal, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { QuestionService } from '../../../../core/services/question.service';
import {
  DIFFICULTIES,
  DIFFICULTY_LABEL,
  Difficulty,
  MAX_OPTIONS,
  MIN_OPTIONS,
  QuestionRequest,
  QuestionResponse
} from '../../../../core/models/question.models';
import { TagResponse } from '../../../../core/models/tag.models';

/**
 * Crear (question = null) o editar una pregunta. Las opciones vacías se
 * descartan al guardar y la respuesta correcta se reindexa en consecuencia.
 */
@Component({
  selector: 'app-question-form',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './question-form.component.html',
  styleUrl: './question-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionFormComponent {
  private fb = inject(FormBuilder);
  private questionSvc = inject(QuestionService);

  question = input<QuestionResponse | null>(null);
  tags = input<TagResponse[]>([]);
  saved = output<QuestionResponse>();
  cancelled = output<void>();

  readonly difficulties = DIFFICULTIES;
  readonly diffLabel = DIFFICULTY_LABEL;
  readonly minOptions = MIN_OPTIONS;

  saving = signal(false);
  /** Se limpia sola al cambiar de pregunta. */
  error = linkedSignal<QuestionResponse | null, string | null>({ source: this.question, computation: () => null });

  form = this.fb.nonNullable.group({
    text:          ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]],
    options:       this.fb.nonNullable.array(Array.from({ length: MAX_OPTIONS }, () => '')),
    correctAnswer: [0],
    explanation:   ['', [Validators.required, Validators.maxLength(2000)]],
    difficulty:    ['FACIL' as Difficulty],
    tagIds:        [[] as number[]]
  });

  private formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  filledOptions = computed(() => (this.formValue().options ?? []).filter(o => o?.trim()).length);
  correctIsEmpty = computed(() => {
    const v = this.formValue();
    return !v.options?.[v.correctAnswer ?? 0]?.trim();
  });
  optionsError = computed(() => {
    if (this.filledOptions() < MIN_OPTIONS) return `Necesitas al menos ${MIN_OPTIONS} opciones con texto.`;
    if (this.correctIsEmpty()) return 'La opción marcada como correcta está vacía.';
    return null;
  });

  constructor() {
    // Rellena el formulario cada vez que cambia la pregunta (o lo vacía si es nueva)
    effect(() => {
      const q = this.question();
      this.form.reset({
        text: q?.text ?? '',
        options: Array.from({ length: MAX_OPTIONS }, (_, i) => q?.options[i] ?? ''),
        correctAnswer: q?.correctAnswer ?? 0,
        explanation: q?.explanation ?? '',
        difficulty: q?.difficulty ?? 'FACIL',
        tagIds: q?.tags.map(t => t.id) ?? []
      });
    });
  }

  save(): void {
    if (this.form.invalid || this.optionsError() || this.saving()) return;
    const v = this.form.getRawValue();

    // Quita las opciones vacías; la correcta pasa a ser su posición entre las que quedan
    const kept = v.options.map((text, index) => ({ text: text.trim(), index })).filter(o => o.text);
    const request: QuestionRequest = {
      text: v.text.trim(),
      options: kept.map(o => o.text),
      correctAnswer: kept.findIndex(o => o.index === v.correctAnswer),
      explanation: v.explanation.trim(),
      difficulty: v.difficulty,
      tagIds: v.tagIds
    };

    const q = this.question();
    this.saving.set(true);
    this.error.set(null);
    (q ? this.questionSvc.edit(q.id, request) : this.questionSvc.create(request)).subscribe({
      next: saved => {
        this.saving.set(false);
        this.saved.emit(saved);
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo guardar la pregunta');
      }
    });
  }
}
