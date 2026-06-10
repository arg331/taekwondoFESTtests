import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ExamService } from '../../core/services/exam.service';
import { QuestionService } from '../../core/services/question.service';
import { TagService } from '../../core/services/tag.service';
import { ExamResponse, ExamStatus } from '../../core/models/exam.models';
import { QuestionResponse, Difficulty } from '../../core/models/question.models';
import { TagResponse } from '../../core/models/tag.models';

@Component({
  selector: 'app-exams',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDividerModule,
    MatTabsModule,
    MatSlideToggleModule
  ],
  templateUrl: './exams.component.html',
  styleUrl: './exams.component.scss'
})
export class ExamsComponent implements OnInit {
  private examSvc     = inject(ExamService);
  private questionSvc = inject(QuestionService);
  private tagSvc      = inject(TagService);
  private fb          = inject(FormBuilder);
  private route       = inject(ActivatedRoute);
  router              = inject(Router);

  loading       = signal(true);
  saving        = signal(false);
  exams         = signal<ExamResponse[]>([]);
  questions     = signal<QuestionResponse[]>([]);
  tags          = signal<TagResponse[]>([]);
  showForm      = signal(false);
  showRandomForm = signal(false);
  selectedExam  = signal<ExamResponse | null>(null);
  showQr        = signal<ExamResponse | null>(null);
  editingConfig = signal(false);
  publishError  = signal<string | null>(null);

  pickerSearch = signal('');
  pickerDiff   = signal<Difficulty | ''>('');

  difficulties: Difficulty[] = ['FACIL', 'MEDIO', 'DIFICIL'];
  diffLabel: Record<Difficulty, string> = {
    FACIL: 'Fácil', MEDIO: 'Medio', DIFICIL: 'Difícil'
  };

  filteredQuestions = computed(() => {
    let list = this.questions();
    const search = this.pickerSearch().toLowerCase().trim();
    if (search) list = list.filter(q => q.text.toLowerCase().includes(search));
    if (this.pickerDiff()) list = list.filter(q => q.difficulty === this.pickerDiff());
    return list;
  });

  canPublish = computed(() => {
    const exam = this.selectedExam();
    if (!exam) return false;
    const n = this.selectedQuestionIds().length;
    return n >= 5 && n === exam.config.numberOfQuestions;
  });

  publishBlockReason = computed(() => {
    const exam = this.selectedExam();
    if (!exam) return '';
    const n = this.selectedQuestionIds().length;
    if (n < 5) return `Necesitas al menos 5 preguntas (tienes ${n})`;
    if (n !== exam.config.numberOfQuestions)
      return `Tienes ${n} preguntas seleccionadas pero el examen requiere ${exam.config.numberOfQuestions}. Ajusta la configuración o selecciona más preguntas.`;
    return '';
  });

  statusLabel: Record<ExamStatus, string> = {
    DRAFT: 'Borrador', PUBLISHED: 'Publicado', EXPIRED: 'Expirado'
  };
  statusColor: Record<ExamStatus, string> = {
    DRAFT: '#ff9800', PUBLISHED: '#4caf50', EXPIRED: '#9e9e9e'
  };
  statusIcon: Record<ExamStatus, string> = {
    DRAFT: 'edit_note', PUBLISHED: 'check_circle', EXPIRED: 'schedule'
  };

  draftForm = this.fb.group({
    title:                  ['', [Validators.required, Validators.minLength(3)]],
    numberOfQuestions:      [10, [Validators.required, Validators.min(5), Validators.max(50)]],
    timeLimitMinutes:       [60],
    showScore:              [true],
    randomizeOptions:       [false],
    randomizeQuestionOrder: [false]
  });

  randomForm = this.fb.group({
    title:                  ['', [Validators.required, Validators.minLength(3)]],
    numberOfQuestions:      [10, [Validators.required, Validators.min(5), Validators.max(50)]],
    timeLimitMinutes:       [60],
    showScore:              [true],
    randomizeOptions:       [true],
    randomizeQuestionOrder: [true],
    requiredAnyOfTagIds:    [[] as number[]]
  });

  publishForm = this.fb.group({
    visibility: ['PUBLIC'],
    expiresAt:  [null as string | null]
  });

  configForm = this.fb.group({
    title:                  ['', [Validators.required, Validators.minLength(3)]],
    numberOfQuestions:      [10, [Validators.required, Validators.min(5), Validators.max(50)]],
    timeLimitMinutes:       [null as number | null],
    showScore:              [true],
    randomizeOptions:       [false],
    randomizeQuestionOrder: [false]
  });

  selectedQuestionIds = signal<number[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.questionSvc.getAll().subscribe(q => this.questions.set(q));
    this.tagSvc.getAll().subscribe(t => this.tags.set(t));
    this.examSvc.getMine().subscribe({
      next: e => {
        this.exams.set(e);
        this.loading.set(false);
        const selectId = this.route.snapshot.queryParamMap.get('select');
        if (selectId) {
          const exam = e.find(ex => ex.id === Number(selectId));
          if (exam) this.selectExam(exam);
        }
      },
      error: () => this.loading.set(false)
    });
  }

  // ── Formulario manual ─────────────────────────────
  openCreateForm(): void {
    this.draftForm.reset({
      title: '', numberOfQuestions: 10,
      timeLimitMinutes: 60, showScore: true,
      randomizeOptions: false, randomizeQuestionOrder: false
    });
    this.showForm.set(true);
    this.showRandomForm.set(false);
    this.selectedExam.set(null);
  }

  cancelForm(): void {
    this.showForm.set(false);
  }

  createDraft(): void {
    if (this.draftForm.invalid || this.saving()) return;
    this.saving.set(true);
    const v = this.draftForm.value as any;
    this.examSvc.createDraft({
      title: v.title,
      numberOfQuestions: v.numberOfQuestions,
      timeLimitMinutes: v.timeLimitMinutes || null,
      showScore: v.showScore,
      randomizeOptions: v.randomizeOptions,
      randomizeQuestionOrder: v.randomizeQuestionOrder
    }).subscribe({
      next: exam => {
        this.exams.update(list => [exam, ...list]);
        this.saving.set(false);
        this.showForm.set(false);
        this.selectExam(exam);
      },
      error: () => this.saving.set(false)
    });
  }

  // ── Formulario aleatorio ──────────────────────────
  openRandomForm(): void {
    this.randomForm.reset({
      title: '', numberOfQuestions: 10,
      timeLimitMinutes: 60, showScore: true,
      randomizeOptions: true, randomizeQuestionOrder: true,
      requiredAnyOfTagIds: []
    });
    this.showRandomForm.set(true);
    this.showForm.set(false);
    this.selectedExam.set(null);
  }

  cancelRandomForm(): void {
    this.showRandomForm.set(false);
  }

  createRandom(): void {
    if (this.randomForm.invalid || this.saving()) return;
    this.saving.set(true);
    const v = this.randomForm.value as any;
    this.examSvc.preGenerate({
      title: v.title,
      numberOfQuestions: v.numberOfQuestions,
      timeLimitMinutes: v.timeLimitMinutes || null,
      showScore: v.showScore,
      randomizeOptions: v.randomizeOptions,
      randomizeQuestionOrder: v.randomizeQuestionOrder,
      requiredAnyOfTagIds: v.requiredAnyOfTagIds ?? []
    }).subscribe({
      next: exam => {
        this.exams.update(list => [exam, ...list]);
        this.saving.set(false);
        this.showRandomForm.set(false);
        this.selectExam(exam);
      },
      error: (err) => {
        this.saving.set(false);
        alert(err?.error?.message || 'No hay suficientes preguntas con los filtros seleccionados');
      }
    });
  }

  // ── Selección de examen ───────────────────────────
  selectExam(exam: ExamResponse): void {
    this.selectedExam.set(exam);
    this.selectedQuestionIds.set([...exam.questionIds]);
    this.showQr.set(null);
    this.editingConfig.set(false);
    this.publishError.set(null);
    this.pickerSearch.set('');
    this.pickerDiff.set('');
  }

  closeDetail(): void {
    this.selectedExam.set(null);
    this.showQr.set(null);
    this.editingConfig.set(false);
  }

  // ── Editar config ──────────────────────────────────
  openEditConfig(): void {
    const exam = this.selectedExam()!;
    this.configForm.setValue({
      title:                  exam.title,
      numberOfQuestions:      exam.config.numberOfQuestions,
      timeLimitMinutes:       exam.config.timeLimitMinutes ?? null,
      showScore:              exam.config.showScore,
      randomizeOptions:       exam.config.randomizeOptions,
      randomizeQuestionOrder: exam.config.randomizeQuestionOrder
    });
    this.editingConfig.set(true);
  }

  cancelEditConfig(): void {
    this.editingConfig.set(false);
  }

  saveConfig(): void {
    const exam = this.selectedExam();
    if (!exam || this.configForm.invalid || this.saving()) return;
    this.saving.set(true);
    const v = this.configForm.value as any;

    const titleChanged = v.title !== exam.title;
    const configChanged =
      v.numberOfQuestions !== exam.config.numberOfQuestions ||
      (v.timeLimitMinutes || null) !== exam.config.timeLimitMinutes ||
      v.showScore !== exam.config.showScore ||
      v.randomizeOptions !== exam.config.randomizeOptions ||
      v.randomizeQuestionOrder !== exam.config.randomizeQuestionOrder;

    let pending = (titleChanged ? 1 : 0) + (configChanged ? 1 : 0);
    if (pending === 0) { this.saving.set(false); this.editingConfig.set(false); return; }

    const done = (updated: ExamResponse) => {
      pending--;
      this.exams.update(list => list.map(e => e.id === updated.id ? updated : e));
      this.selectedExam.set(updated);
      if (pending === 0) { this.saving.set(false); this.editingConfig.set(false); }
    };

    if (titleChanged) {
      this.examSvc.rename(exam.id, { newTitle: v.title }).subscribe({
        next: done, error: () => this.saving.set(false)
      });
    }
    if (configChanged) {
      this.examSvc.changeConfig(exam.id, {
        numberOfQuestions:      v.numberOfQuestions,
        timeLimitMinutes:       v.timeLimitMinutes || null,
        showScore:              v.showScore,
        randomizeOptions:       v.randomizeOptions,
        randomizeQuestionOrder: v.randomizeQuestionOrder
      }).subscribe({ next: done, error: () => this.saving.set(false) });
    }
  }

  // ── Preguntas ──────────────────────────────────────
  toggleQuestion(id: number): void {
    const current = this.selectedQuestionIds();
    if (current.includes(id)) {
      this.selectedQuestionIds.set(current.filter(q => q !== id));
    } else {
      this.selectedQuestionIds.set([...current, id]);
    }
  }

  isSelected(id: number): boolean {
    return this.selectedQuestionIds().includes(id);
  }

  saveQuestions(): void {
    const exam = this.selectedExam();
    if (!exam) return;
    this.examSvc.updateQuestions(exam.id, {
      questionIds: this.selectedQuestionIds()
    }).subscribe({
      next: updated => {
        this.exams.update(list => list.map(e => e.id === updated.id ? updated : e));
        this.selectedExam.set(updated);
        this.publishError.set(null);
      }
    });
  }

  // ── Estado ────────────────────────────────────────
  publish(exam: ExamResponse): void {
    if (!this.canPublish()) return;
    this.publishError.set(null);
    const v = this.publishForm.value as any;
    this.examSvc.publish(exam.id, {
      visibility: v.visibility,
      expiresAt: v.expiresAt || null
    }).subscribe({
      next: updated => {
        this.exams.update(list => list.map(e => e.id === updated.id ? updated : e));
        this.selectedExam.set(updated);
        this.showQr.set(updated);
      },
      error: (err) => {
        this.publishError.set(err?.error?.message || 'Error al publicar el examen');
      }
    });
  }

  close(exam: ExamResponse): void {
    if (!confirm('¿Cerrar este examen? Los estudiantes no podrán acceder.')) return;
    this.examSvc.close(exam.id).subscribe({
      next: updated => {
        this.exams.update(list => list.map(e => e.id === updated.id ? updated : e));
        this.selectedExam.set(updated);
      }
    });
  }

  deleteDraft(exam: ExamResponse): void {
    if (!confirm(`¿Eliminar el borrador "${exam.title}"?`)) return;
    this.examSvc.deleteDraft(exam.id).subscribe({
      next: () => {
        this.exams.update(list => list.filter(e => e.id !== exam.id));
        if (this.selectedExam()?.id === exam.id) this.selectedExam.set(null);
      }
    });
  }

  getQrUrl(code: string): string {
    return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${code}&bgcolor=ffffff&color=000000`;
  }

  copyCode(code: string): void {
    navigator.clipboard.writeText(code);
  }
}
