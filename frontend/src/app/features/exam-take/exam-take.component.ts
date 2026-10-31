import { ChangeDetectionStrategy, Component, inject, signal, computed, OnInit, OnDestroy, input, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, concatMap, map, of } from 'rxjs';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExamService } from '../../core/services/exam.service';
import { ResultService } from '../../core/services/result.service';
import { ConfirmService } from '../../core/services/confirm.service';
import { AuthService } from '../../core/services/auth.service';
import { ExamAttemptResponse, PublicExamResponse, PublicQuestionResponse, SaveAnswerRequest } from '../../core/models/exam.models';
import { ResultResponse } from '../../core/models/result.models';
import { ExamAccessCardComponent, StudentInfo } from './components/exam-access-card/exam-access-card.component';
import { ExamResultCardComponent } from './components/exam-result-card/exam-result-card.component';

type Phase = 'loading' | 'error' | 'access' | 'exam' | 'result';


/** El intento en curso, guardado en el navegador para recuperarlo si se recarga la página (RF-24). */
interface StoredAttempt {
  attemptToken: string;
  student: StudentInfo;
}

const STORED_ATTEMPT_PREFIX = 'fest_attempt_';

/**
 * Flujo del alumno desde el QR: portada → preguntas → resultado.
 * El servidor baraja preguntas/opciones y controla el tiempo con el token del intento. Cada respuesta
 * se guarda al contestarla y el navegador recuerda el intento: recargar no pierde el examen (RF-24).
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
  styleUrl: './exam-take.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamTakeComponent implements OnInit, OnDestroy {
  private examSvc   = inject(ExamService);
  private resultSvc = inject(ResultService);
  private confirmSvc = inject(ConfirmService);
  private auth      = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  /** Parámetro de ruta :code, en mayúsculas: los códigos se guardan así y alguien puede teclearlo en minúsculas. */
  code = input.required<string, string>({ transform: (value: string) => value.trim().toUpperCase() });

  user = this.auth.currentUser;

  phase       = signal<Phase>('loading');
  errorMsg    = signal('');
  exam        = signal<PublicExamResponse | null>(null);
  questions   = signal<PublicQuestionResponse[]>([]);
  result      = signal<ResultResponse | null>(null);
  starting    = signal(false);
  startError  = signal('');
  submitting  = signal(false);
  submitError = signal<string | null>(null);
  /** La última respuesta no se pudo guardar: se avisa, y se enviará igualmente al entregar. */
  saveFailed  = signal(false);

  /** Respuestas por guardar, de una en una y en orden: lo último guardado es lo último que marcó. */
  private readonly saves = new Subject<SaveAnswerRequest>();

  private student: StudentInfo | null = null;
  private attemptToken = '';
  /** El intento en curso: cuál es y de cuántos (null = sin límite). */
  private attempt = signal<{ number: number; max: number | null } | null>(null);
  /** «Intento 2 de 3», o «Intento 4» sin límite; con un solo intento no se muestra (RF-52). */
  attemptLabel = computed(() => {
    const a = this.attempt();
    if (!a || a.max === 1) return null;
    return a.max === null ? `Intento ${a.number}` : `Intento ${a.number} de ${a.max}`;
  });
  /** Intentos que le quedan después del actual: null = sin límite, 0 = ninguno. */
  remainingAttempts = computed(() => {
    const a = this.attempt();
    if (!a) return 0;
    return a.max === null ? null : Math.max(0, a.max - a.number);
  });

  currentIndex = signal(0);
  /** questionId → índice original de la opción elegida. */
  answers      = signal<Record<number, number[]>>({});

  timeLeft = signal(0);
  private deadline = 0;
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  currentQuestion = computed(() => this.questions()[this.currentIndex()]);
  answeredCount   = computed(() => Object.values(this.answers()).filter(chosen => chosen.length > 0).length);
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

  constructor() {
    this.saves.pipe(
      concatMap(request => this.examSvc.saveAnswer(this.code(), request).pipe(
        map(() => true),
        catchError(() => of(false))
      )),
      takeUntilDestroyed()
    ).subscribe(saved => {
      if (this.phase() === 'exam') this.saveFailed.set(!saved);
    });
  }

  ngOnInit(): void {
    this.examSvc.getByCode(this.code()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: exam => {
        this.exam.set(exam);
        const stored = this.storedAttempt();
        if (stored) {
          this.resume(stored);
        } else {
          this.phase.set('access');
        }
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
    this.startError.set('');
    this.examSvc.startAttempt(this.code(), student).subscribe({
      next: attempt => {
        this.starting.set(false);
        this.enterExam(attempt);
      },
      error: err => {
        this.starting.set(false);
        const message = err?.error?.message ?? 'Error al cargar las preguntas.';
        // Desde la portada, lo que el servidor rechaza (p. ej. un nombre con un intento en curso, DE-21)
        // se avisa en el formulario para poder corregirlo
        if (this.phase() === 'access' && err?.status === 409) {
          this.startError.set(message);
        } else {
          this.fail(message);
        }
      }
    });
  }

  /** Tras recargar: sigue el intento donde lo dejó. Si ya no vale (entregado o caducado), vuelve a la portada. */
  private resume(stored: StoredAttempt): void {
    this.student = stored.student;
    this.examSvc.resumeAttempt(this.code(), stored.attemptToken).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: attempt => this.enterExam(attempt),
      error: () => {
        this.forgetAttempt();
        this.phase.set('access');
      }
    });
  }

  /** Muestra el intento, nuevo o recuperado, con lo ya contestado y el tiempo que le queda. */
  private enterExam(attempt: ExamAttemptResponse): void {
    this.attemptToken = attempt.attemptToken;
    this.rememberAttempt();
    this.attempt.set({ number: attempt.attemptNumber, max: attempt.maxAttempts });
    this.questions.set(attempt.questions);
    this.answers.set(attempt.answers ?? {});
    this.currentIndex.set(0);
    this.saveFailed.set(false);
    this.clearTimer();
    this.startTimer(attempt.secondsLeft);
    this.phase.set('exam');
  }

  /** «Repetir examen»: otro intento con los mismos datos, sin volver a la portada. */
  repeat(): void {
    if (this.student) this.startExam(this.student);
  }

  // ── Examen ────────────────────────────────────────
  /**
   * Con varias correctas (RF-09) cada opción se marca o desmarca; con una sola, elegir otra sustituye
   * a la anterior.
   */
  selectAnswer(question: PublicQuestionResponse, optionIndex: number): void {
    const chosen = this.answers()[question.id] ?? [];
    const next = !question.multipleCorrect ? [optionIndex]
      : chosen.includes(optionIndex) ? chosen.filter(i => i !== optionIndex)
      : [...chosen, optionIndex].sort((x, y) => x - y);
    this.answers.update(a => ({ ...a, [question.id]: next }));
    this.saves.next({ attemptToken: this.attemptToken, questionId: question.id, chosenOptions: next });
  }

  /** «A», «B»…: una pregunta puede tener hasta 20 opciones (RF-09). */
  letter(position: number): string {
    return String.fromCharCode('A'.charCodeAt(0) + position);
  }

  isChosen(questionId: number, optionIndex: number): boolean {
    return (this.answers()[questionId] ?? []).includes(optionIndex);
  }

  isAnswered(questionId: number): boolean {
    return (this.answers()[questionId] ?? []).length > 0;
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
    this.confirmSvc.ask({ title: 'Enviar examen', message, confirmLabel: 'Enviar', cancelLabel: 'Seguir con el examen' })
      .subscribe(ok => { if (ok) this.submit(); });
  }

  private submit(): void {
    if (this.submitting()) return;
    this.submitting.set(true);
    this.submitError.set(null);

    this.resultSvc.submit({
      examCode: this.code(),
      attemptToken: this.attemptToken,
      answers: this.questions().map(q => ({
        questionId: q.id,
        chosenOptions: this.answers()[q.id] ?? []
      }))
    }).subscribe({
      next: result => {
        this.clearTimer();
        this.forgetAttempt();
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
  /** El tiempo que queda lo dice el servidor: al recuperar el intento, el reloj sigue donde iba. */
  private startTimer(secondsLeft: number | null): void {
    if (secondsLeft === null) return;
    // Se calcula contra una hora fija para no acumular desfase si la pestaña se ralentiza
    this.deadline = Date.now() + secondsLeft * 1000;
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

  // ── Intento guardado en el navegador (RF-24) ──────
  private storageKey(): string {
    return STORED_ATTEMPT_PREFIX + this.code();
  }

  private storedAttempt(): StoredAttempt | null {
    try {
      const stored = JSON.parse(localStorage.getItem(this.storageKey()) ?? 'null') as StoredAttempt | null;
      return typeof stored?.attemptToken === 'string' && stored.student ? stored : null;
    } catch {
      return null;
    }
  }

  private rememberAttempt(): void {
    if (!this.student) return;
    try {
      localStorage.setItem(this.storageKey(),
        JSON.stringify({ attemptToken: this.attemptToken, student: this.student } satisfies StoredAttempt));
    } catch {
      // Sin almacenamiento solo se pierde poder recuperarlo: las respuestas siguen guardándose en el servidor
    }
  }

  private forgetAttempt(): void {
    try {
      localStorage.removeItem(this.storageKey());
    } catch {
      // Nada que olvidar
    }
  }

  private fail(message: string): void {
    this.errorMsg.set(message);
    this.phase.set('error');
  }
}
