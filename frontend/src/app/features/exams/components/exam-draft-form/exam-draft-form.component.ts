import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
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
import { ExamDraftRequest, ExamResponse } from '../../../../core/models/exam.models';
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
  styleUrl: './exam-draft-form.component.scss'
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

  form = this.fb.nonNullable.group({
    title:                  ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    numberOfQuestions:      [10, [Validators.required, Validators.min(5), Validators.max(50)]],
    timeLimitMinutes:       [60, [Validators.min(0), Validators.max(180)]],
    showScore:              [true],
    randomizeOptions:       [false],
    randomizeQuestionOrder: [false],
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

    const v = this.form.getRawValue();
    const request: ExamDraftRequest = {
      ...v,
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
