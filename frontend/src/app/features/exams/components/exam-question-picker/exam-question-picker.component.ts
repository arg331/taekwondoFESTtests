import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import {
  QUESTION_TYPE_LABEL,
  QuestionResponse,
  QuestionTypeFilter
} from '../../../../core/models/question.models';
import { GradeBadgeComponent } from '../../../../shared/components/grade-badge/grade-badge.component';
import { GradeRange, GradeRangeComponent } from '../../../../shared/components/grade-range/grade-range.component';
import { TagResponse } from '../../../../core/models/tag.models';
import { EXAM_MAX_QUESTIONS } from '../../../../core/models/exam.models';
import {
  QuestionFormDialogComponent,
  QuestionFormDialogData
} from '../../../questions/components/question-form-dialog/question-form-dialog.component';
import {
  QuestionFilters,
  countByTag,
  loadSavedFilters,
  matchesFilters,
  pickRandom,
  saveFilters
} from './question-filters';
import { TagFilterComponent } from '../../../../shared/components/tag-filter/tag-filter.component';

/**
 * Selector de preguntas de un borrador: filtros por texto, tags, tipo y rango de grados,
 * vista previa, carrito con lo elegido, relleno al azar y alta de preguntas sin salir del examen.
 * selectedIds es bidireccional: [(selectedIds)].
 */
@Component({
  selector: 'app-exam-question-picker',
  imports: [
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatButtonToggleModule,
    GradeBadgeComponent,
    GradeRangeComponent,
    TagFilterComponent
  ],
  templateUrl: './exam-question-picker.component.html',
  styleUrl: './exam-question-picker.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamQuestionPickerComponent {
  private dialog = inject(MatDialog);

  questions = input.required<QuestionResponse[]>();
  tags = input<TagResponse[]>([]);
  /** Objetivo de preguntas: hasta dónde rellena «Completar al azar». Se publican las elegidas (RF-46). */
  targetCount = input(0);
  selectedIds = model.required<number[]>();
  /** Pregunta creada desde el selector: el padre la añade a su lista del banco. */
  questionCreated = output<QuestionResponse>();

  readonly typeLabel = QUESTION_TYPE_LABEL;

  private readonly saved = loadSavedFilters();
  search = signal('');
  selectedTagIds = signal<number[]>(this.saved.selectedTagIds);
  gradeRange = signal<GradeRange>(this.saved.gradeRange);
  typeFilter = signal<QuestionTypeFilter>(this.saved.typeFilter);

  expandedId = signal<number | null>(null);
  randomNote = signal<string | null>(null);

  private filters = computed<QuestionFilters>(() => ({
    search: this.search(),
    selectedTagIds: this.selectedTagIds(),
    gradeRange: this.gradeRange(),
    typeFilter: this.typeFilter()
  }));

  filtered = computed(() => this.questions().filter(q => matchesFilters(q, this.filters())));
  tagCounts = computed(() => countByTag(this.questions()));
  /** Hay tags marcados: la lista está filtrada por ellos. */
  filteringByTags = computed(() => this.selectedTagIds().length > 0);

  selectedQuestions = computed(() => {
    const byId = new Map(this.questions().map(q => [q.id, q]));
    return this.selectedIds()
      .map(id => byId.get(id))
      .filter((q): q is QuestionResponse => q !== undefined);
  });
  readonly maxQuestions = EXAM_MAX_QUESTIONS;
  overMax = computed(() => this.selectedIds().length > EXAM_MAX_QUESTIONS);
  missing = computed(() => Math.max(0, this.targetCount() - this.selectedIds().length));
  candidates = computed(() => this.filtered().filter(q => !this.selectedIds().includes(q.id)));

  constructor() {
    // Los filtros de tags, grado y tipo se recuerdan en este navegador
    effect(() => saveFilters({
      selectedTagIds: this.selectedTagIds(),
      gradeRange: this.gradeRange(),
      typeFilter: this.typeFilter()
    }));
  }

  // ── Selección ─────────────────────────────────────
  isSelected(id: number): boolean {
    return this.selectedIds().includes(id);
  }

  toggle(id: number): void {
    this.randomNote.set(null);
    this.selectedIds.update(ids => ids.includes(id) ? ids.filter(q => q !== id) : [...ids, id]);
  }

  remove(id: number): void {
    this.randomNote.set(null);
    this.selectedIds.update(ids => ids.filter(q => q !== id));
  }

  clear(): void {
    this.randomNote.set(null);
    this.selectedIds.set([]);
  }

  /** Rellena los huecos que faltan con preguntas visibles con los filtros actuales. */
  completeRandomly(): void {
    const missing = this.missing();
    const picked = pickRandom(this.candidates(), missing);
    this.selectedIds.update(ids => [...ids, ...picked.map(q => q.id)]);
    this.randomNote.set(picked.length < missing
      ? `Solo había ${picked.length} pregunta(s) disponibles con estos filtros; faltan ${missing - picked.length}.`
      : null);
  }

  // ── Vista previa y alta ───────────────────────────
  togglePreview(id: number, event: Event): void {
    event.stopPropagation();
    this.expandedId.update(current => current === id ? null : id);
  }

  openNewQuestion(): void {
    this.dialog
      .open<QuestionFormDialogComponent, QuestionFormDialogData, QuestionResponse>(QuestionFormDialogComponent, {
        data: { tags: this.tags() },
        width: '760px',
        maxWidth: '95vw',
        maxHeight: '90vh'
      })
      .afterClosed()
      .subscribe(question => {
        if (!question) return;
        this.questionCreated.emit(question);
        this.selectedIds.update(ids => [...ids, question.id]);
      });
  }
}
