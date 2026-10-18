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
import {
  QuestionTypeFilter,
  QuestionResponse
} from '../../core/models/question.models';
import { TagResponse } from '../../core/models/tag.models';
import { QuestionFormComponent } from './components/question-form/question-form.component';
import { QuestionCardComponent, QuestionCardVariant } from './components/question-card/question-card.component';
import { TagManagerComponent } from './components/tag-manager/tag-manager.component';
import { countByTag, matchesGrade, matchesTags, matchesType } from '../exams/components/exam-question-picker/question-filters';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { FULL_GRADE_RANGE, GradeRange, GradeRangeComponent } from '../../shared/components/grade-range/grade-range.component';
import { TagFilterComponent } from '../../shared/components/tag-filter/tag-filter.component';

type QuestionsView = 'cards' | 'list';
const VIEW_STORAGE_KEY = 'questions-view';

/** Banco de preguntas del profesor: filtros, listado, alta/edición y tags. */
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
  private readonly destroyRef = inject(DestroyRef);


  loading     = signal(true);
  questions   = signal<QuestionResponse[]>([]);
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

  tagCounts     = computed(() => countByTag(this.questions()));
  hasFilters    = computed(() =>
    !!this.filterText().trim() || !!this.typeFilter() || this.selectedTagIds().length > 0 ||
    this.gradeRange()[0] !== FULL_GRADE_RANGE[0] || this.gradeRange()[1] !== FULL_GRADE_RANGE[1]);

  filtered = computed(() => {
    const text = this.filterText().toLowerCase().trim();
    const grades = this.gradeRange();
    const type = this.typeFilter();
    const selected = new Set(this.selectedTagIds());
    return this.questions().filter(q =>
      (!text || q.text.toLowerCase().includes(text)) &&
      matchesGrade(q, grades) &&
      matchesType(q, type) &&
      matchesTags(q, selected));
  });

  ngOnInit(): void {
    this.tagSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(tags => this.tags.set(tags));
    this.questionSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: q => { this.questions.set(q); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  onSaved(saved: QuestionResponse): void {
    const exists = this.questions().some(q => q.id === saved.id);
    this.questions.update(list => exists ? list.map(q => q.id === saved.id ? saved : q) : [saved, ...list]);
    this.editing.set(undefined);
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

  delete(question: QuestionResponse): void {
    if (!confirm(`¿Eliminar la pregunta "${question.text.substring(0, 50)}..."?`)) return;
    this.error.set(null);
    this.questionSvc.delete(question.id).subscribe({
      next: () => this.questions.update(list => list.filter(q => q.id !== question.id)),
      // p. ej. la pregunta está en un examen ya publicado
      error: err => this.error.set(err?.error?.message ?? 'No se pudo eliminar la pregunta')
    });
  }
}

function loadView(): QuestionsView {
  try {
    return localStorage.getItem(VIEW_STORAGE_KEY) === 'list' ? 'list' : 'cards';
  } catch {
    return 'cards';
  }
}
