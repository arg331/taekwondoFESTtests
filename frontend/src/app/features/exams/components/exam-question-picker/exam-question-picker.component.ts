import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSliderModule } from '@angular/material/slider';
import {
  DIFFICULTIES,
  DIFFICULTY_COLOR,
  DIFFICULTY_LABEL,
  QuestionResponse
} from '../../../../core/models/question.models';
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

/**
 * Selector de preguntas de un borrador: filtros por texto, tags y rango de dificultad,
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
    MatSliderModule
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

  readonly difficulties = DIFFICULTIES;
  readonly diffLabel = DIFFICULTY_LABEL;
  readonly diffColor = DIFFICULTY_COLOR;

  private readonly saved = loadSavedFilters();
  search = signal('');
  selectedTagIds = signal<number[]>(this.saved.selectedTagIds);
  untaggedSelected = signal(this.saved.untaggedSelected);
  difficultyRange = signal<[number, number]>(this.saved.difficultyRange);

  expandedId = signal<number | null>(null);
  randomNote = signal<string | null>(null);

  private filters = computed<QuestionFilters>(() => ({
    search: this.search(),
    selectedTagIds: this.selectedTagIds(),
    untaggedSelected: this.untaggedSelected(),
    difficultyRange: this.difficultyRange()
  }));

  filtered = computed(() => this.questions().filter(q => matchesFilters(q, this.filters())));
  tagCounts = computed(() => countByTag(this.questions()));
  untaggedCount = computed(() => this.questions().filter(q => q.tags.length === 0).length);

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

  /** Etiqueta que muestra la barra de dificultad mientras se arrastra. */
  readonly difficultyLabel = (level: number): string => DIFFICULTY_LABEL[DIFFICULTIES[level]];

  constructor() {
    // Los filtros de tags y dificultad se recuerdan en este navegador
    effect(() => saveFilters({
      selectedTagIds: this.selectedTagIds(),
      untaggedSelected: this.untaggedSelected(),
      difficultyRange: this.difficultyRange()
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

  // ── Filtros ───────────────────────────────────────
  isTagSelected(tagId: number): boolean {
    return this.selectedTagIds().includes(tagId);
  }

  setTagSelected(tagId: number, selected: boolean): void {
    this.selectedTagIds.update(ids => selected ? [...ids, tagId] : ids.filter(id => id !== tagId));
  }

  setDifficultyFrom(level: number): void {
    this.difficultyRange.update(([, to]) => [level, to]);
  }

  setDifficultyTo(level: number): void {
    this.difficultyRange.update(([from]) => [from, level]);
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
