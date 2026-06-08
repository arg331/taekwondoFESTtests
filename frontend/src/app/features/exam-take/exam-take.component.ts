import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { ExamService } from '../../core/services/exam.service';
import { ExamResponse, PublicQuestionResponse } from '../../core/models/exam.models';
import { ResultResponse } from '../../core/models/result.models';

type Phase = 'loading' | 'error' | 'access' | 'exam' | 'result';

@Component({
  selector: 'app-exam-take',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatChipsModule
  ],
  templateUrl: './exam-take.component.html',
  styleUrl: './exam-take.component.scss'
})
export class ExamTakeComponent implements OnInit, OnDestroy {
  private route      = inject(ActivatedRoute);
  private router     = inject(Router);
  private examSvc    = inject(ExamService);
  private fb         = inject(FormBuilder);

  // ── Estado general ────────────────────────────────
  phase      = signal<Phase>('loading');
  errorMsg   = signal('');
  code       = signal('');
  exam       = signal<ExamResponse | null>(null);
  questions  = signal<PublicQuestionResponse[]>([]);
  result     = signal<ResultResponse | null>(null);

  // ── Fase acceso ───────────────────────────────────
  accessForm = this.fb.group({
    studentName:  ['', [Validators.required, Validators.minLength(2)]],
    studentClub:  [''],
    studentEmail: ['', Validators.email]
  });

  // ── Fase examen ───────────────────────────────────
  currentIndex  = signal(0);
  answers       = signal<Record<number, number>>({});  // questionId -> chosenOption
  submitting    = signal(false);
  startTime     = 0;

  // Temporizador
  timeLeft      = signal(0);
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  currentQuestion = computed(() => this.questions()[this.currentIndex()]);
  progress        = computed(() => {
    const total = this.questions().length;
    return total ? Math.round((Object.keys(this.answers()).length / total) * 100) : 0;
  });
  answeredCount   = computed(() => Object.keys(this.answers()).length);
  hasAnswer       = computed(() =>
    this.currentQuestion() !== undefined &&
    this.answers()[this.currentQuestion().id] !== undefined
  );
  allAnswered     = computed(() =>
    this.questions().length > 0 &&
    Object.keys(this.answers()).length === this.questions().length
  );

  // ── Fase resultado ────────────────────────────────
  get passedLabel(): string {
    return this.result()?.passed ? 'APROBADO' : 'SUSPENSO';
  }
  get passedColor(): string {
    return this.result()?.passed ? '#4caf50' : '#f44336';
  }

  ngOnInit(): void {
    const c = this.route.snapshot.paramMap.get('code') ?? '';
    this.code.set(c);
    this.loadExam(c);
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  private loadExam(code: string): void {
    this.phase.set('loading');
    this.examSvc.getByCode(code).subscribe({
      next: exam => {
        this.exam.set(exam);
        this.phase.set('access');
      },
      error: err => {
        this.errorMsg.set(err.error?.message ?? 'Examen no encontrado o no disponible.');
        this.phase.set('error');
      }
    });
  }

  // ── Acciones fase acceso ──────────────────────────
  startExam(): void {
    if (this.accessForm.invalid) return;
    this.phase.set('loading');
    this.examSvc.getQuestionsByCode(this.code()).subscribe({
      next: qs => {
        // Aleatorizar si el examen lo requiere
        const shuffled = this.exam()!.config.randomizeQuestionOrder
          ? [...qs].sort(() => Math.random() - 0.5)
          : qs;
        this.questions.set(shuffled);
        this.answers.set({});
        this.currentIndex.set(0);
        this.startTime = Date.now();
        this.initTimer();
        this.phase.set('exam');
      },
      error: () => {
        this.errorMsg.set('Error al cargar las preguntas.');
        this.phase.set('error');
      }
    });
  }

  // ── Acciones fase examen ──────────────────────────
  selectAnswer(questionId: number, optionIndex: number): void {
    this.answers.update(a => ({ ...a, [questionId]: optionIndex }));
  }

  getAnswer(questionId: number): number | undefined {
    return this.answers()[questionId];
  }

  goTo(index: number): void {
    if (index >= 0 && index < this.questions().length) {
      this.currentIndex.set(index);
    }
  }

  submitExam(): void {
    if (this.submitting()) return;
    if (!confirm('¿Enviar el examen? No podrás modificar las respuestas.')) return;
    this.submitting.set(true);
    this.clearTimer();

    const timeSpent = Math.round((Date.now() - this.startTime) / 1000);
    const v = this.accessForm.value as any;

    const answersPayload = this.questions().map(q => ({
      questionId: q.id,
      chosenOption: this.answers()[q.id] ?? 0
    }));

    this.examSvc.submitExam({
      examCode: this.code(),
      studentName: v.studentName,
      studentClub: v.studentClub || null,
      studentEmail: v.studentEmail || null,
      answers: answersPayload,
      timeSpentSeconds: timeSpent
    }).subscribe({
      next: result => {
        this.result.set(result);
        this.phase.set('result');
        this.submitting.set(false);
      },
      error: err => {
        this.errorMsg.set(err.error?.message ?? 'Error al enviar el examen.');
        this.submitting.set(false);
      }
    });
  }

  // ── Temporizador ──────────────────────────────────
  private initTimer(): void {
    const limit = this.exam()!.config.timeLimitMinutes;
    if (!limit) return;
    this.timeLeft.set(limit * 60);
    this.timerInterval = setInterval(() => {
      const left = this.timeLeft() - 1;
      if (left <= 0) {
        this.timeLeft.set(0);
        this.clearTimer();
        this.submitExam();
      } else {
        this.timeLeft.set(left);
      }
    }, 1000);
  }

  private clearTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  get timerDisplay(): string {
    const t = this.timeLeft();
    const m = Math.floor(t / 60).toString().padStart(2, '0');
    const s = (t % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }

  get timerWarning(): boolean {
    return this.timeLeft() > 0 && this.timeLeft() <= 60;
  }
}
