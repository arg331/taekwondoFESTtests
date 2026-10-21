import { ChangeDetectionStrategy, Component, computed, effect, inject, input, linkedSignal, output, resource, signal, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { rxResource } from '@angular/core/rxjs-interop';
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
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ExamService } from '../../../../core/services/exam.service';
import { ShareService } from '../../../../core/services/share.service';
import { QR_PANEL_SIZE_PX, QrCodeService } from '../../../../core/services/qr-code.service';
import { QrColorService } from '../../../../core/services/qr-color.service';
import {
  EXAM_MAX_QUESTIONS,
  EXAM_MIN_ATTEMPTS,
  attemptsRequest,
  EXAM_MIN_QUESTIONS,
  EXAM_STATUS_COLOR,
  EXAM_STATUS_LABEL,
  ExamResponse,
  VISIBILITY_COLOR,
  VISIBILITY_ICON,
  VISIBILITY_LABEL,
  Visibility,
  RESULT_DISPLAYS,
  RESULT_DISPLAY_LABEL,
  RESULT_DISPLAY_SUMMARY,
  ResultDisplay,
  resultDisplayFlags,
  resultDisplayOf
} from '../../../../core/models/exam.models';
import { QuestionResponse } from '../../../../core/models/question.models';
import { TagResponse } from '../../../../core/models/tag.models';
import { ExamQuestionPickerComponent } from '../exam-question-picker/exam-question-picker.component';
import { AttemptsSliderComponent } from '../attempts-slider/attempts-slider.component';
import { ExamQrDialogComponent, ExamQrDialogData } from '../exam-qr-dialog/exam-qr-dialog.component';
import { ExamShareComponent } from '../exam-share/exam-share.component';

/** Cuánto se ve un aviso breve (enlace copiado, etc.). */
const NOTICE_DURATION_MS = 3000;

/**
 * Panel de un examen: configuración, preguntas, publicación y ciclo de vida.
 * En borrador todo es editable; publicado o cerrado solo se gestionan
 * plazo, visibilidad y estado. Un examen que me han compartido (RF-51) solo se consulta y se proyecta.
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
    ExamQuestionPickerComponent,
    AttemptsSliderComponent,
    ExamShareComponent
  ],
  templateUrl: './exam-detail.component.html',
  styleUrl: './exam-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamDetailComponent {
  private fb = inject(FormBuilder);
  private examSvc = inject(ExamService);
  private shareSvc = inject(ShareService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private qrCode = inject(QrCodeService);
  private qrColor = inject(QrColorService);

  exam = input.required<ExamResponse>();
  questions = input<QuestionResponse[]>([]);
  /** Las de los exámenes que me han compartido: las de un borrador compartido se ven aquí (RF-57). */
  sharedQuestions = input<QuestionResponse[]>([]);
  /** Tags del profesor, para filtrar y para crear preguntas desde el selector. */
  tags = input<TagResponse[]>([]);
  /** Códigos de los otros exámenes abiertos: al publicar, el QR toma un color distinto (RF-50). */
  openExamCodes = input<string[]>([]);
  changed = output<ExamResponse>();
  deleted = output<number>();
  /** Copia creada como borrador (RF-18): el padre la añade a la lista y la abre. */
  copied = output<ExamResponse>();
  /** Pregunta creada desde el selector: el padre la añade al banco que muestra. */
  questionCreated = output<QuestionResponse>();

  readonly statusLabel = EXAM_STATUS_LABEL;
  readonly statusColor = EXAM_STATUS_COLOR;
  readonly visibilityLabel = VISIBILITY_LABEL;
  readonly visibilityIcon = VISIBILITY_ICON;
  readonly visibilityColor = VISIBILITY_COLOR;

  /**
   * El panel se reutiliza al elegir otro examen de la lista: todo su estado se reinicia
   * cuando cambia el examen (su id), no cuando llega el mismo examen actualizado.
   */
  private readonly examId = computed(() => this.exam().id);

  busy = signal(false);
  error = linkedSignal<number, string | null>({ source: this.examId, computation: () => null });
  /** Me lo han compartido (RF-51): puedo consultarlo y proyectarlo, no cambiarlo. */
  readOnly = computed(() => !!this.exam().sharedBy);
  /** Las preguntas elegidas de un borrador compartido, en su orden. */
  sharedDraftQuestions = computed(() => {
    const byId = new Map(this.sharedQuestions().map(q => [q.id, q]));
    return this.exam().questionIds.map(id => byId.get(id)).filter((q): q is QuestionResponse => !!q);
  });
  /** Panel para compartirlo con otros profesores; se carga al abrirlo. */
  showShare = linkedSignal({ source: this.examId, computation: () => false });
  editingConfig = linkedSignal({ source: this.examId, computation: () => false });
  showQr = linkedSignal({ source: this.examId, computation: () => false });

  /** Selección de preguntas en edición; se reinicia al cambiar de examen. */
  selectedIds = linkedSignal(() => [...this.exam().questionIds]);
  questionsDirty = computed(() => {
    const saved = this.exam().questionIds;
    const current = this.selectedIds();
    return saved.length !== current.length || saved.some((id, i) => id !== current[i]);
  });
  /** Ya publicado: sus preguntas tal como las ven los alumnos, no como estén hoy en el banco (DE-03). */
  publishedQuestions = rxResource({
    params: () => (this.exam().status === 'DRAFT' ? undefined : this.exam().id),
    stream: ({ params: id }) => this.examSvc.publishedQuestions(id)
  });

  /** Se publican las preguntas elegidas, coincidan o no con el objetivo (RF-46). */
  publishBlockReason = computed(() => {
    const n = this.selectedIds().length;
    if (n < EXAM_MIN_QUESTIONS) return `Selecciona al menos ${EXAM_MIN_QUESTIONS} preguntas para publicar (tienes ${n}).`;
    if (n > EXAM_MAX_QUESTIONS) return `Un examen admite como máximo ${EXAM_MAX_QUESTIONS} preguntas (tienes ${n}).`;
    return '';
  });

  /** Botón para pasar de examen de prueba a oficial y al revés, diciendo qué cambia de verdad (RF-49). */
  visibilityToggleLabel = computed(() =>
    this.exam().visibility === 'PUBLIC' ? 'Pasar a oficial (privado)' : 'Pasar a examen de prueba (público)');

  examUrl = computed(() => `${window.location.origin}/exam/${this.exam().code}`);
  /** Cerrado a mano o fuera de plazo: quien escanee el QR no podrá entregar. */
  closedForSubmissions = computed(() => {
    const { status, expiresAt } = this.exam();
    return status === 'EXPIRED' || (status === 'PUBLISHED' && !!expiresAt && new Date(expiresAt) <= new Date());
  });
  /** QR del panel: se genera en el navegador, solo mientras se muestra y con el color del examen (RF-50). */
  qrImage = resource({
    params: () => (this.showQr() ? { url: this.examUrl(), color: this.qrColor.colorOf(this.exam().code ?? '') } : undefined),
    loader: ({ params: { url, color } }) => this.qrCode.toDataUrl(url, QR_PANEL_SIZE_PX, color)
  });

  configForm = this.fb.nonNullable.group({
    title:                  ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    numberOfQuestions:      [10, [Validators.required, Validators.min(EXAM_MIN_QUESTIONS), Validators.max(EXAM_MAX_QUESTIONS)]],
    timeLimitMinutes:       [0, [Validators.min(0), Validators.max(180)]],
    resultDisplay:          ['SCORE_AND_ANSWERS' as ResultDisplay],
    randomizeOptions:       [false],
    randomizeQuestionOrder: [false]
  });

  readonly resultDisplays = RESULT_DISPLAYS;
  readonly resultDisplayLabel = RESULT_DISPLAY_LABEL;
  /** Qué ve el alumno al entregar, para el resumen (RF-53). */
  resultDisplaySummary = computed(() => RESULT_DISPLAY_SUMMARY[resultDisplayOf(this.exam().config)]);

  publishForm = this.fb.nonNullable.group({
    // Por defecto, oficial: solo entra quien tenga el código
    visibility: ['PRIVATE' as Visibility],
    accessMode: ['OPEN' as 'OPEN' | 'REGISTERED_ONLY'],
    // Vacío = no caduca (RF-48)
    expiresAt:  ['']
  });
  /** Intentos por alumno al publicar (RF-52); null = sin límite. Vuelve a 1 al cambiar de examen. */
  publishAttempts = linkedSignal<number, number | null>({ source: this.examId, computation: () => EXAM_MIN_ATTEMPTS });
  /** Intentos de un examen publicado mientras se eligen otros. */
  attemptsDraft = linkedSignal(() => this.exam().maxAttempts);

  expirationForm = this.fb.nonNullable.group({ expiresAt: [''] });

  constructor() {
    // Los formularios reactivos no son signals: se vacían al cambiar de examen
    effect(() => {
      this.examId();
      untracked(() => this.publishForm.reset());
    });
    // «Cambiar plazo» parte de la fecha vigente; al reabrir, la anterior ya pasó y se deja vacía
    effect(() => {
      const { status, expiresAt } = this.exam();
      const current = status === 'PUBLISHED' && expiresAt ? toDateTimeLocal(expiresAt) : '';
      untracked(() => this.expirationForm.reset({ expiresAt: current }));
    });
  }

  // ── Configuración ─────────────────────────────────
  openEditConfig(): void {
    const { title, config } = this.exam();
    this.configForm.setValue({
      title,
      numberOfQuestions: config.numberOfQuestions,
      timeLimitMinutes: config.timeLimitMinutes ?? 0,
      resultDisplay: resultDisplayOf(config),
      randomizeOptions: config.randomizeOptions,
      randomizeQuestionOrder: config.randomizeQuestionOrder
    });
    this.editingConfig.set(true);
  }

  saveConfig(): void {
    if (this.configForm.invalid) return;
    const exam = this.exam();
    const { title, resultDisplay, ...config } = this.configForm.getRawValue();
    const newConfig = { ...config, ...resultDisplayFlags(resultDisplay), timeLimitMinutes: config.timeLimitMinutes || null };

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
    if (this.publishBlockReason() || this.publishForm.invalid) return;
    const exam = this.exam();
    const v = this.publishForm.getRawValue();
    // Si hay cambios de preguntas sin guardar, se guardan antes de publicar
    const save$: Observable<unknown> = this.questionsDirty()
      ? this.examSvc.updateQuestions(exam.id, { questionIds: this.selectedIds() })
      : of(null);

    this.run(save$.pipe(switchMap(() => this.examSvc.publish(exam.id, {
      visibility: v.visibility,
      accessMode: v.accessMode,
      expiresAt: v.expiresAt || null,
      ...attemptsRequest(this.publishAttempts())
    }))), published => {
      if (published.code) this.qrColor.assignDistinct(published.code, this.openExamCodes());
      this.showQr.set(true);
    });
  }

  /** Subir o bajar los intentos de un examen publicado (RF-52). */
  changeAttempts(): void {
    this.run(this.examSvc.changeAttempts(this.exam().id, attemptsRequest(this.attemptsDraft())));
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

  /** «Dejar de ver» un examen que me compartieron (RF-51): sale de mi lista. */
  leave(): void {
    const exam = this.exam();
    if (!confirm(`¿Dejar de ver «${exam.title}»? Ya no tendrás acceso a sus resultados.`)) return;
    this.busy.set(true);
    this.shareSvc.leave(exam.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.deleted.emit(exam.id);
      },
      error: err => this.handleError(err)
    });
  }

  /** Copia el examen, en cualquier estado, como borrador nuevo para ampliarlo (RF-18). */
  copyExam(): void {
    this.busy.set(true);
    this.error.set(null);
    this.examSvc.copy(this.exam().id).subscribe({
      next: copy => {
        this.busy.set(false);
        this.copied.emit(copy);
        this.notify('Copia creada como borrador');
      },
      error: err => this.handleError(err)
    });
  }

  /** QR grande para proyectar en el aula; no se proyecta un examen que no admite entregas. */
  project(): void {
    if (this.closedForSubmissions()) return;
    const { title, code } = this.exam();
    this.dialog.open<ExamQrDialogComponent, ExamQrDialogData>(ExamQrDialogComponent, {
      data: { title, code: code ?? '', url: this.examUrl() },
      width: '100vw',
      height: '100vh',
      maxWidth: '100vw',
      maxHeight: '100vh'
    });
  }

  /** Copia el enlace. Si el navegador no lo permite (p. ej. http con una IP), lo muestra para copiarlo a mano. */
  copy(text: string): void {
    const clipboard: Clipboard | undefined = navigator.clipboard;
    if (!clipboard) {
      this.copyFailed();
      return;
    }
    clipboard.writeText(text).then(() => this.notify('Enlace copiado'), () => this.copyFailed());
  }

  private copyFailed(): void {
    this.showQr.set(true);
    this.notify('No se pudo copiar el enlace: cópialo a mano desde el panel');
  }

  private notify(message: string): void {
    this.snackBar.open(message, undefined, { duration: NOTICE_DURATION_MS });
  }

  /** Ejecuta una acción que devuelve el examen actualizado y lo propaga al padre. */
  private run(action$: Observable<ExamResponse>, onSuccess?: (updated: ExamResponse) => void): void {
    this.busy.set(true);
    this.error.set(null);
    action$.subscribe({
      next: updated => {
        this.busy.set(false);
        this.changed.emit(updated);
        onSuccess?.(updated);
      },
      error: err => this.handleError(err)
    });
  }

  private handleError(err: { error?: { message?: string } }): void {
    this.busy.set(false);
    this.error.set(err?.error?.message ?? 'No se pudo completar la acción');
  }
}

/** '2026-12-31T10:00:00' → '2026-12-31T10:00', el formato de un <input type="datetime-local">. */
function toDateTimeLocal(dateTime: string): string {
  return dateTime.slice(0, 16);
}
