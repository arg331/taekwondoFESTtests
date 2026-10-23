import { ChangeDetectionStrategy, Component, inject, signal, computed, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { QuestionService } from '../../core/services/question.service';
import { TagService } from '../../core/services/tag.service';
import { ConfirmService } from '../../core/services/confirm.service';
import { ShareService } from '../../core/services/share.service';
import {
  QuestionTypeFilter,
  QuestionResponse
} from '../../core/models/question.models';
import { TagResponse } from '../../core/models/tag.models';
import { QuestionFormComponent } from './components/question-form/question-form.component';
import { QuestionCardComponent, QuestionCardVariant } from './components/question-card/question-card.component';
import { TagManagerComponent } from './components/tag-manager/tag-manager.component';
import { matchesGrade, matchesType } from '../exams/components/exam-question-picker/question-filters';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { FULL_GRADE_RANGE, GradeRange, GradeRangeComponent } from '../../shared/components/grade-range/grade-range.component';
import { TagFilterComponent } from '../../shared/components/tag-filter/tag-filter.component';

type QuestionsView = 'cards' | 'list';
/** Qué preguntas se ven (RF-57): las mías, las que me han compartido, todas o las que eliminé. */
type QuestionsSource = 'mine' | 'shared' | 'all' | 'deleted';
const VIEW_STORAGE_KEY = 'questions-view';

/**
 * Banco de preguntas del profesor: filtros, listado, alta/edición y tags. Además consulta, sin poder
 * cambiarlas, las preguntas de los exámenes que le han compartido (RF-57).
 */
@Component({
  selector: 'app-questions',
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonToggleModule,
    GradeRangeComponent,
    TagFilterComponent,
    MatProgressSpinnerModule,
    QuestionFormComponent,
    QuestionCardComponent,
    TagManagerComponent
  ],
  templateUrl: './questions.component.html',
  styleUrl: './questions.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionsComponent implements OnInit {
  private questionSvc = inject(QuestionService);
  private tagSvc      = inject(TagService);
  private confirmSvc = inject(ConfirmService);
  private shareSvc    = inject(ShareService);
  private readonly destroyRef = inject(DestroyRef);


  loading     = signal(true);
  questions   = signal<QuestionResponse[]>([]);
  /** Las de los exámenes que me han compartido: solo se consultan. */
  sharedQuestions = signal<QuestionResponse[]>([]);
  /** Fuera del banco: los exámenes publicados las siguen usando. Solo se recuperan. */
  deletedQuestions = signal<QuestionResponse[]>([]);
  source      = signal<QuestionsSource>('mine');
  /** Las preguntas de la vista elegida, antes de filtrar. */
  pool        = computed(() => {
    switch (this.source()) {
      case 'mine': return this.questions();
      case 'shared': return this.sharedQuestions();
      case 'all': return [...this.questions(), ...this.sharedQuestions()];
      case 'deleted': return this.deletedQuestions();
    }
  });
  tags        = signal<TagResponse[]>([]);
  /** undefined = formulario cerrado; null = nueva pregunta; objeto = editando. */
  editing     = signal<QuestionResponse | null | undefined>(undefined);
  error       = signal<string | null>(null);
  /** Tarjetas cuadradas o lista simple; se recuerda en el navegador. */
  view        = signal<QuestionsView>(loadView());
  cardVariant = computed<QuestionCardVariant>(() => (this.view() === 'list' ? 'row' : 'card'));

  filterText   = signal('');
  gradeRange   = signal<GradeRange>(FULL_GRADE_RANGE);
  typeFilter   = signal<QuestionTypeFilter>('');
  /** Tags marcados. Ninguno = todas las preguntas; si no, las que tengan alguno de ellos. */
  selectedTagIds = signal<number[]>([]);

  /**
   * Los tags se cuentan y se filtran por nombre: los de las preguntas compartidas son de otro profesor,
   * así que su «Combate» cuenta como el mío.
   */
  private tagIdByName = computed(() => new Map(this.tags().map(t => [tagKey(t.name), t.id])));
  tagCounts     = computed(() => {
    const counts = new Map<number, number>();
    for (const question of this.pool()) {
      for (const tag of question.tags) {
        const id = this.tagIdByName().get(tagKey(tag.name));
        if (id !== undefined) counts.set(id, (counts.get(id) ?? 0) + 1);
      }
    }
    return counts;
  });
  hasFilters    = computed(() =>
    !!this.filterText().trim() || !!this.typeFilter() || this.selectedTagIds().length > 0 ||
    this.gradeRange()[0] !== FULL_GRADE_RANGE[0] || this.gradeRange()[1] !== FULL_GRADE_RANGE[1]);

  filtered = computed(() => {
    const text = this.filterText().toLowerCase().trim();
    const grades = this.gradeRange();
    const type = this.typeFilter();
    const selectedIds = new Set(this.selectedTagIds());
    const selected = new Set(this.tags().filter(t => selectedIds.has(t.id)).map(t => tagKey(t.name)));
    return this.pool().filter(q =>
      (!text || q.text.toLowerCase().includes(text)) &&
      matchesGrade(q, grades) &&
      matchesType(q, type) &&
      (selected.size === 0 || q.tags.some(tag => selected.has(tagKey(tag.name)))));
  });

  ngOnInit(): void {
    this.tagSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(tags => this.tags.set(tags));
    this.questionSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: q => { this.questions.set(q); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
    this.shareSvc.questions().pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: q => this.sharedQuestions.set(q), error: () => this.sharedQuestions.set([]) });
    this.questionSvc.getDeleted().pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: q => this.deletedQuestions.set(q), error: () => this.deletedQuestions.set([]) });
  }

  onSaved(saved: QuestionResponse): void {
    const exists = this.questions().some(q => q.id === saved.id);
    this.questions.update(list => exists ? list.map(q => q.id === saved.id ? saved : q) : [saved, ...list]);
    this.editing.set(undefined);
  }

  /** Un tag renombrado o con otro color cambia en sus preguntas sin recargar (RF-08). */
  onTagUpdated(tag: TagResponse): void {
    this.questions.update(list => list.map(q => q.tags.some(t => t.id === tag.id)
      ? { ...q, tags: q.tags.map(t => t.id === tag.id ? tag : t) }
      : q));
  }

  /** Un tag borrado desaparece de sus preguntas y del filtro (RF-08). */
  onTagRemoved(tagId: number): void {
    this.questions.update(list => list.map(q => q.tags.some(t => t.id === tagId)
      ? { ...q, tags: q.tags.filter(t => t.id !== tagId) }
      : q));
    this.selectedTagIds.update(ids => ids.filter(id => id !== tagId));
  }

  clearFilters(): void {
    this.filterText.set('');
    this.gradeRange.set(FULL_GRADE_RANGE);
    this.typeFilter.set('');
    this.selectedTagIds.set([]);
  }

  setView(view: QuestionsView): void {
    this.view.set(view);
    try {
      localStorage.setItem(VIEW_STORAGE_KEY, view);
    } catch {
      // Sin almacenamiento: la vista vale solo para esta visita
    }
  }

  /** Sale del banco a «Preguntas eliminadas»; los exámenes publicados que la usan no cambian. */
  delete(question: QuestionResponse): void {
    this.confirmSvc.ask({
      title: 'Eliminar pregunta', confirmLabel: 'Eliminar',
      message: `¿Eliminar «${question.text.substring(0, 50)}…»? Podrás recuperarla en «Preguntas eliminadas».`
    }).subscribe(ok => {
      if (!ok) return;
      this.error.set(null);
      this.questionSvc.delete(question.id).subscribe({
        next: () => {
          this.questions.update(list => list.filter(q => q.id !== question.id));
          this.deletedQuestions.update(list => [{ ...question, deletedAt: new Date().toISOString() }, ...list]);
        },
        error: err => this.error.set(err?.error?.message ?? 'No se pudo eliminar la pregunta')
      });
    });
  }

  /** Vuelve al banco una pregunta eliminada. */
  restore(question: QuestionResponse): void {
    this.error.set(null);
    this.questionSvc.restore(question.id).subscribe({
      next: restored => {
        this.deletedQuestions.update(list => list.filter(q => q.id !== question.id));
        this.questions.update(list => [restored, ...list]);
      },
      error: err => this.error.set(err?.error?.message ?? 'No se pudo recuperar la pregunta')
    });
  }
}

function tagKey(name: string): string {
  return name.trim().toLocaleLowerCase('es');
}

function loadView(): QuestionsView {
  try {
    return localStorage.getItem(VIEW_STORAGE_KEY) === 'list' ? 'list' : 'cards';
  } catch {
    return 'cards';
  }
}
