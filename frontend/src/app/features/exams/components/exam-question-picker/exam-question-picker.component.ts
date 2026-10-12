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
  /** Número de preguntas que pide el examen. */
  requiredCount = input(0);
  selectedIds = model.required<number[]>();
  /** Pregunta creada desde el selector: el padre la añade a su lista del banco. */
  questionCreated = output<QuestionResponse>();

  readonly difficulties = DIFFICULTIES;
  readonly diffLabel = DIFFICULTY_LABEL;
  readonly diffColor = DIFFICULTY_COLOR;

  private readonly saved = loadSavedFilters();
  search = signal('');
  hiddenTagIds = signal<number[]>(this.saved.hiddenTagIds);
  showUntagged = signal(this.saved.showUntagged);
  difficultyRange = signal<[number, number]>(this.saved.difficultyRange);

  expandedId = signal<number | null>(null);
  randomNote = signal<string | null>(null);

  private filters = computed<QuestionFilters>(() => ({
    search: this.search(),
    hiddenTagIds: this.hiddenTagIds(),
    showUntagged: this.showUntagged(),
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
  overSelected = computed(() => this.selectedIds().length > this.requiredCount());
  missing = computed(() => Math.max(0, this.requiredCount() - this.selectedIds().length));
  candidates = computed(() => this.filtered().filter(q => !this.selectedIds().includes(q.id)));

  /** Etiqueta que muestra la barra de dificultad mientras se arrastra. */
  readonly difficultyLabel = (level: number): string => DIFFICULTY_LABEL[DIFFICULTIES[level]];

  constructor() {
    // Los filtros de tags y dificultad se recuerdan en este navegador
    effect(() => saveFilters({
      hiddenTagIds: this.hiddenTagIds(),
      showUntagged: this.showUntagged(),
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
  isTagVisible(tagId: number): boolean {
    return !this.hiddenTagIds().includes(tagId);
  }

  setTagVisible(tagId: number, visible: boolean): void {
    this.hiddenTagIds.update(ids => visible ? ids.filter(id => id !== tagId) : [...ids, tagId]);
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
