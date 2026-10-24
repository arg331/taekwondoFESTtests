import { ChangeDetectionStrategy, Component, computed, effect, inject, input, linkedSignal, output, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { QuestionService } from '../../../../core/services/question.service';
import {
  GRADES,
  Grade,
  MAX_OPTIONS,
  MIN_OPTIONS,
  QUESTION_TYPES,
  QUESTION_TYPE_LABEL,
  QuestionRequest,
  QuestionResponse,
  QuestionType
} from '../../../../core/models/question.models';
import { TagResponse } from '../../../../core/models/tag.models';
import { GradeBadgeComponent } from '../../../../shared/components/grade-badge/grade-badge.component';

/** Valor de «Sin grado» en el desplegable: con null, el desplegable se mostraría vacío. */
const NO_GRADE = '';

/**
 * Crear (question = null) o editar una pregunta. Siempre hay una casilla de opción libre al
 * final: al escribir en ella aparece otra (RF-09). Las opciones vacías se descartan al guardar
 * y la respuesta correcta se reindexa en consecuencia.
 */
@Component({
  selector: 'app-question-form',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    GradeBadgeComponent
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

  readonly grades = GRADES;
  readonly noGrade = NO_GRADE;
  readonly questionTypes = QUESTION_TYPES;
  readonly typeLabel = QUESTION_TYPE_LABEL;
  readonly minOptions = MIN_OPTIONS;

  saving = signal(false);
  /** Se limpia sola al cambiar de pregunta. */
  error = linkedSignal<QuestionResponse | null, string | null>({ source: this.question, computation: () => null });

  form = this.fb.nonNullable.group({
    text:          ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]],
    options:       this.fb.nonNullable.array(Array.from({ length: MIN_OPTIONS }, () => '')),
    /** Índices de las correctas: una o varias (RF-09). Una pregunta nueva no trae ninguna. */
    correctAnswers: [[] as number[]],
    explanation:   ['', [Validators.maxLength(2000)]],
    // Opcional (RF-43): una pregunta nueva parte sin grado
    grade:         [NO_GRADE as Grade | typeof NO_GRADE],
    /** '' = sin tipo. */
    type:          ['' as QuestionType | ''],
    tagIds:        [[] as number[]]
  });

  private formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  filledOptions = computed(() => (this.formValue().options ?? []).filter(o => o?.trim()).length);
  noCorrect = computed(() => (this.formValue().correctAnswers ?? []).length === 0);
  correctIsEmpty = computed(() => {
    const v = this.formValue();
    return (v.correctAnswers ?? []).some(i => !v.options?.[i]?.trim());
  });
  optionsError = computed(() => {
    if (this.filledOptions() < MIN_OPTIONS) return `Necesitas al menos ${MIN_OPTIONS} opciones con texto.`;
    if (this.noCorrect()) return 'Marca al menos una opción correcta.';
    if (this.correctIsEmpty()) return 'La opción marcada como correcta está vacía.';
    return null;
  });
  /** Opciones que se pueden quitar: todas menos la casilla libre del final, si quedan más de las mínimas. */
  canRemoveOptions = computed(() => (this.formValue().options?.length ?? 0) - 1 > MIN_OPTIONS);

  constructor() {
    // Rellena el formulario cada vez que cambia la pregunta (o lo vacía si es nueva)
    effect(() => {
      const q = this.question();
      const options = q ? [...q.options] : Array.from({ length: MIN_OPTIONS - 1 }, () => '');
      this.form.controls.options.clear({ emitEvent: false });
      [...options, ''].forEach(o => this.form.controls.options.push(this.fb.nonNullable.control(o), { emitEvent: false }));
      this.form.reset({
        text: q?.text ?? '',
        options: [...options, ''],
        correctAnswers: q ? [...q.correctAnswers] : [],
        explanation: q?.explanation ?? '',
        grade: q?.grade ?? NO_GRADE,
        type: q?.type ?? '',
        tagIds: q?.tags.map(t => t.id) ?? []
      });
    });
    // Al escribir en la última casilla aparece otra vacía
    this.form.controls.options.valueChanges.pipe(takeUntilDestroyed()).subscribe(options => {
      if (options.at(-1)?.trim() && options.length < MAX_OPTIONS) {
        this.form.controls.options.push(this.fb.nonNullable.control(''));
      }
    });
  }

  isTrailing(index: number): boolean {
    return index === this.form.controls.options.length - 1;
  }

  isCorrect(index: number): boolean {
    return this.form.controls.correctAnswers.value.includes(index);
  }

  setCorrect(index: number, correct: boolean): void {
    const current = this.form.controls.correctAnswers.value.filter(i => i !== index);
    this.form.controls.correctAnswers.setValue(correct ? [...current, index].sort((a, b) => a - b) : current);
  }

  removeOption(index: number): void {
    if (!this.canRemoveOptions() || this.isTrailing(index)) return;
    const correct = this.form.controls.correctAnswers.value;
    this.form.controls.options.removeAt(index);
    // Las correctas siguen apuntando a las mismas opciones; si se quita una correcta, deja de serlo
    this.form.controls.correctAnswers.setValue(correct.filter(i => i !== index).map(i => i > index ? i - 1 : i));
  }

  save(): void {
    if (this.form.invalid || this.optionsError() || this.saving()) return;
    const v = this.form.getRawValue();

    // Quita las opciones vacías; las correctas pasan a ser su posición entre las que quedan
    const kept = v.options.map((text, index) => ({ text: text.trim(), index })).filter(o => o.text);
    const request: QuestionRequest = {
      text: v.text.trim(),
      options: kept.map(o => o.text),
      correctAnswers: v.correctAnswers.map(i => kept.findIndex(o => o.index === i)).filter(i => i >= 0),
      explanation: v.explanation.trim(),
      grade: v.grade === NO_GRADE ? null : v.grade,
      type: v.type || null,
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
