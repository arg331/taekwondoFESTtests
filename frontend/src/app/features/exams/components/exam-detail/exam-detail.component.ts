import { Component, computed, inject, input, linkedSignal, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, of, switchMap } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { ExamService } from '../../../../core/services/exam.service';
import {
  EXAM_STATUS_COLOR,
  EXAM_STATUS_LABEL,
  ExamResponse,
  Visibility
} from '../../../../core/models/exam.models';
import { QuestionResponse } from '../../../../core/models/question.models';
import { ExamQuestionPickerComponent } from '../exam-question-picker/exam-question-picker.component';

/**
 * Panel de un examen: configuración, preguntas, publicación y ciclo de vida.
 * En borrador todo es editable; publicado o cerrado solo se gestionan
 * plazo, visibilidad y estado.
 */
@Component({
  selector: 'app-exam-detail',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDividerModule,
    ExamQuestionPickerComponent
  ],
  templateUrl: './exam-detail.component.html',
  styleUrl: './exam-detail.component.scss'
})
export class ExamDetailComponent {
  private fb = inject(FormBuilder);
  private examSvc = inject(ExamService);

  exam = input.required<ExamResponse>();
  questions = input<QuestionResponse[]>([]);
  changed = output<ExamResponse>();
  deleted = output<number>();

  readonly statusLabel = EXAM_STATUS_LABEL;
  readonly statusColor = EXAM_STATUS_COLOR;

  busy = signal(false);
  error = signal<string | null>(null);
  editingConfig = signal(false);
  showQr = signal(false);

  /** Selección de preguntas en edición; se reinicia al cambiar de examen. */
  selectedIds = linkedSignal(() => [...this.exam().questionIds]);
  questionsDirty = computed(() => {
    const saved = this.exam().questionIds;
    const current = this.selectedIds();
    return saved.length !== current.length || saved.some((id, i) => id !== current[i]);
  });
  assignedQuestions = computed(() => {
    const byId = new Map(this.questions().map(q => [q.id, q]));
    return this.exam().questionIds
      .map(id => byId.get(id))
      .filter((q): q is QuestionResponse => q !== undefined);
  });

  publishBlockReason = computed(() => {
    const n = this.selectedIds().length;
    const required = this.exam().config.numberOfQuestions;
    return n === required ? '' : `Tienes ${n} preguntas seleccionadas y el examen requiere ${required}.`;
  });

  examUrl = computed(() => `${window.location.origin}/exam/${this.exam().code}`);
  qrUrl = computed(() =>
    `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(this.examUrl())}`);

  configForm = this.fb.nonNullable.group({
    title:                  ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    numberOfQuestions:      [10, [Validators.required, Validators.min(5), Validators.max(50)]],
    timeLimitMinutes:       [0, [Validators.min(0), Validators.max(180)]],
    showScore:              [true],
    randomizeOptions:       [false],
    randomizeQuestionOrder: [false]
  });

  publishForm = this.fb.nonNullable.group({
    visibility: ['PUBLIC' as Visibility],
    accessMode: ['OPEN' as 'OPEN' | 'REGISTERED_ONLY'],
    expiresAt:  ['']
  });

  expirationForm = this.fb.nonNullable.group({ expiresAt: [''] });

  // ── Configuración ─────────────────────────────────
  openEditConfig(): void {
    const { title, config } = this.exam();
    this.configForm.setValue({
      title,
      numberOfQuestions: config.numberOfQuestions,
      timeLimitMinutes: config.timeLimitMinutes ?? 0,
      showScore: config.showScore,
      randomizeOptions: config.randomizeOptions,
      randomizeQuestionOrder: config.randomizeQuestionOrder
    });
    this.editingConfig.set(true);
  }

  saveConfig(): void {
    if (this.configForm.invalid) return;
    const exam = this.exam();
    const { title, ...config } = this.configForm.getRawValue();
    const newConfig = { ...config, timeLimitMinutes: config.timeLimitMinutes || null };

    const rename$: Observable<ExamResponse> = title.trim() !== exam.title
      ? this.examSvc.rename(exam.id, { newTitle: title.trim() })
      : of(exam);
    const configChanged = JSON.stringify(newConfig) !== JSON.stringify(exam.config);

    this.run(rename$.pipe(
      switchMap(updated => configChanged ? this.examSvc.changeConfig(exam.id, newConfig) : of(updated))
    ), () => this.editingConfig.set(false));
  }

  // ── Preguntas ─────────────────────────────────────
  saveQuestions(): void {
    this.run(this.examSvc.updateQuestions(this.exam().id, { questionIds: this.selectedIds() }));
  }

  // ── Ciclo de vida ─────────────────────────────────
  publish(): void {
    if (this.publishBlockReason()) return;
    const exam = this.exam();
    const v = this.publishForm.getRawValue();
    // Si hay cambios de preguntas sin guardar, se guardan antes de publicar
    const save$: Observable<unknown> = this.questionsDirty()
      ? this.examSvc.updateQuestions(exam.id, { questionIds: this.selectedIds() })
      : of(null);

    this.run(save$.pipe(switchMap(() => this.examSvc.publish(exam.id, {
      visibility: v.visibility,
      accessMode: v.accessMode,
      expiresAt: v.expiresAt || null
    }))), () => this.showQr.set(true));
  }

  close(): void {
    if (!confirm('¿Cerrar este examen? Los estudiantes no podrán entregarlo.')) return;
    this.run(this.examSvc.close(this.exam().id));
  }

  reopen(): void {
    const expiresAt = this.expirationForm.getRawValue().expiresAt || null;
    this.run(this.examSvc.reopen(this.exam().id, { newExpiresAt: expiresAt }));
  }

  changeExpiration(): void {
    const expiresAt = this.expirationForm.getRawValue().expiresAt || null;
    this.run(this.examSvc.changeExpiration(this.exam().id, { newExpiresAt: expiresAt }));
  }

  toggleVisibility(): void {
    const newVisibility: Visibility = this.exam().visibility === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC';
    this.run(this.examSvc.changeVisibility(this.exam().id, { newVisibility }));
  }

  deleteDraft(): void {
    const exam = this.exam();
    if (!confirm(`¿Eliminar el borrador "${exam.title}"?`)) return;
    this.busy.set(true);
    this.examSvc.deleteDraft(exam.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.deleted.emit(exam.id);
      },
      error: err => this.handleError(err)
    });
  }

  copy(text: string): void {
    navigator.clipboard.writeText(text);
  }

  /** Ejecuta una acción que devuelve el examen actualizado y lo propaga al padre. */
  private run(action$: Observable<ExamResponse>, onSuccess?: () => void): void {
    this.busy.set(true);
    this.error.set(null);
    action$.subscribe({
      next: updated => {
        this.busy.set(false);
        this.changed.emit(updated);
        onSuccess?.();
      },
      error: err => this.handleError(err)
    });
  }

  private handleError(err: { error?: { message?: string } }): void {
    this.busy.set(false);
    this.error.set(err?.error?.message ?? 'No se pudo completar la acción');
  }
}
