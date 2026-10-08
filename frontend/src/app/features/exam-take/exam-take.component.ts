import { Component, inject, signal, computed, OnInit, OnDestroy, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExamService } from '../../core/services/exam.service';
import { ResultService } from '../../core/services/result.service';
import { AuthService } from '../../core/services/auth.service';
import { ExamResponse, PublicQuestionResponse } from '../../core/models/exam.models';
import { ResultResponse } from '../../core/models/result.models';
import { ExamAccessCardComponent, StudentInfo } from './components/exam-access-card/exam-access-card.component';
import { ExamResultCardComponent } from './components/exam-result-card/exam-result-card.component';

type Phase = 'loading' | 'error' | 'access' | 'exam' | 'result';

const OPTION_LETTERS = ['A', 'B', 'C', 'D'];

/**
 * Flujo del alumno desde el QR: portada → preguntas → resultado.
 * El servidor baraja preguntas/opciones y controla el tiempo con el token del intento.
 */
@Component({
  selector: 'app-exam-take',
  imports: [
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    ExamAccessCardComponent,
    ExamResultCardComponent
  ],
  templateUrl: './exam-take.component.html',
  styleUrl: './exam-take.component.scss'
})
export class ExamTakeComponent implements OnInit, OnDestroy {
  private examSvc   = inject(ExamService);
  private resultSvc = inject(ResultService);
  private auth      = inject(AuthService);

  /** Parámetro de ruta :code. */
  code = input.required<string>();

  readonly letters = OPTION_LETTERS;
  user = this.auth.currentUser;

  phase       = signal<Phase>('loading');
  errorMsg    = signal('');
  exam        = signal<ExamResponse | null>(null);
  questions   = signal<PublicQuestionResponse[]>([]);
  result      = signal<ResultResponse | null>(null);
  starting    = signal(false);
  submitting  = signal(false);
  submitError = signal<string | null>(null);

  private student: StudentInfo | null = null;
  private attemptToken = '';

  currentIndex = signal(0);
  /** questionId → índice original de la opción elegida. */
  answers      = signal<Record<number, number>>({});

  timeLeft = signal(0);
  private deadline = 0;
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  currentQuestion = computed(() => this.questions()[this.currentIndex()]);
  answeredCount   = computed(() => Object.keys(this.answers()).length);
  progress        = computed(() => {
    const total = this.questions().length;
    return total ? Math.round((this.answeredCount() / total) * 100) : 0;
  });
  allAnswered     = computed(() =>
    this.questions().length > 0 && this.answeredCount() === this.questions().length);
  timerDisplay    = computed(() => {
    const t = this.timeLeft();
    return `${Math.floor(t / 60).toString().padStart(2, '0')}:${(t % 60).toString().padStart(2, '0')}`;
  });
  timerWarning    = computed(() => this.timeLeft() > 0 && this.timeLeft() <= 60);

  ngOnInit(): void {
    this.examSvc.getByCode(this.code()).subscribe({
      next: exam => {
        this.exam.set(exam);
        this.phase.set('access');
      },
      error: err => this.fail(err?.error?.message ?? 'Examen no encontrado o no disponible.')
    });
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  // ── Portada ───────────────────────────────────────
  startExam(student: StudentInfo): void {
    this.student = student;
    this.starting.set(true);
    this.examSvc.startAttempt(this.code()).subscribe({
      next: attempt => {
        this.attemptToken = attempt.attemptToken;
        this.questions.set(attempt.questions);
        this.answers.set({});
        this.currentIndex.set(0);
        this.starting.set(false);
        this.startTimer();
        this.phase.set('exam');
      },
      error: err => {
        this.starting.set(false);
        this.fail(err?.error?.message ?? 'Error al cargar las preguntas.');
      }
    });
  }

  // ── Examen ────────────────────────────────────────
  selectAnswer(questionId: number, optionIndex: number): void {
    this.answers.update(a => ({ ...a, [questionId]: optionIndex }));
  }

  isChosen(questionId: number, optionIndex: number): boolean {
    return this.answers()[questionId] === optionIndex;
  }

  isAnswered(questionId: number): boolean {
    return this.answers()[questionId] !== undefined;
  }

  goTo(index: number): void {
    if (index >= 0 && index < this.questions().length) {
      this.currentIndex.set(index);
    }
  }

  /** Envío manual: pide confirmación. Al agotarse el tiempo se envía sin preguntar. */
  confirmSubmit(): void {
    const pending = this.questions().length - this.answeredCount();
    const message = pending > 0
      ? `Tienes ${pending} pregunta(s) sin responder. ¿Enviar el examen igualmente?`
      : '¿Enviar el examen? No podrás modificar las respuestas.';
    if (confirm(message)) this.submit();
  }

  private submit(): void {
    if (this.submitting() || !this.student) return;
    this.submitting.set(true);
    this.submitError.set(null);

    this.resultSvc.submit({
      examCode: this.code(),
      attemptToken: this.attemptToken,
      ...this.student,
      answers: this.questions().map(q => ({
        questionId: q.id,
        chosenOption: this.answers()[q.id] ?? null
      }))
    }).subscribe({
      next: result => {
        this.clearTimer();
        this.result.set(result);
        this.submitting.set(false);
        this.phase.set('result');
      },
      error: err => {
        this.submitting.set(false);
        this.submitError.set(err?.error?.message ?? 'Error al enviar el examen. Inténtalo de nuevo.');
      }
    });
  }

  // ── Temporizador ──────────────────────────────────
  private startTimer(): void {
    const limit = this.exam()!.config.timeLimitMinutes;
    if (!limit) return;
    // Se calcula contra una hora fija para no acumular desfase si la pestaña se ralentiza
    this.deadline = Date.now() + limit * 60_000;
    this.tick();
    this.timerInterval = setInterval(() => this.tick(), 1000);
  }

  private tick(): void {
    const left = Math.max(0, Math.ceil((this.deadline - Date.now()) / 1000));
    this.timeLeft.set(left);
    if (left === 0) {
      this.clearTimer();
      this.submit();
    }
  }

  private clearTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  private fail(message: string): void {
    this.errorMsg.set(message);
    this.phase.set('error');
  }
}
