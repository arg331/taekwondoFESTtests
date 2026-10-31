import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExamService } from '../../../../core/services/exam.service';
import {
  EXAM_MAX_QUESTIONS,
  EXAM_MIN_QUESTIONS,
  EXAM_DEFAULT_PASSING_SCORE,
  EXAM_MIN_PASSING_SCORE,
  EXAM_MAX_PASSING_SCORE,
  ExamDraftRequest,
  ExamResponse,
  RESULT_DISPLAYS,
  RESULT_DISPLAY_LABEL,
  ResultDisplay,
  resultDisplayFlags
} from '../../../../core/models/exam.models';
import { TagResponse } from '../../../../core/models/tag.models';

export type DraftMode = 'manual' | 'random';

/**
 * Crear un borrador: vacío (manual) o con preguntas aleatorias del banco (random).
 */
@Component({
  selector: 'app-exam-draft-form',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './exam-draft-form.component.html',
  styleUrl: './exam-draft-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamDraftFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private examSvc = inject(ExamService);

  mode = input.required<DraftMode>();
  tags = input<TagResponse[]>([]);
  created = output<ExamResponse>();
  cancelled = output<void>();

  saving = signal(false);
  error = signal<string | null>(null);
  isRandom = computed(() => this.mode() === 'random');
  readonly resultDisplays = RESULT_DISPLAYS;
  readonly resultDisplayLabel = RESULT_DISPLAY_LABEL;

  form = this.fb.nonNullable.group({
    title:                  ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    numberOfQuestions:      [10, [Validators.required, Validators.min(EXAM_MIN_QUESTIONS), Validators.max(EXAM_MAX_QUESTIONS)]],
    timeLimitMinutes:       [60, [Validators.min(0), Validators.max(180)]],
    resultDisplay:          ['SCORE_AND_ANSWERS' as ResultDisplay],
    randomizeOptions:       [false],
    randomizeQuestionOrder: [false],
    passingScore:           [EXAM_DEFAULT_PASSING_SCORE,
                             [Validators.required, Validators.min(EXAM_MIN_PASSING_SCORE), Validators.max(EXAM_MAX_PASSING_SCORE)]],
    requiredAnyOfTagIds:    [[] as number[]]
  });

  ngOnInit(): void {
    if (this.isRandom()) {
      this.form.patchValue({ randomizeOptions: true, randomizeQuestionOrder: true });
    }
  }

  submit(): void {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);
    this.error.set(null);

    const { resultDisplay, ...v } = this.form.getRawValue();
    const request: ExamDraftRequest = {
      ...v,
      ...resultDisplayFlags(resultDisplay),
      title: v.title.trim(),
      timeLimitMinutes: v.timeLimitMinutes || null
    };
    const call = this.isRandom() ? this.examSvc.preGenerate(request) : this.examSvc.createDraft(request);

    call.subscribe({
      next: exam => {
        this.saving.set(false);
        this.created.emit(exam);
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo crear el examen');
      }
    });
  }
}
