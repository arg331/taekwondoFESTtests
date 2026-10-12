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
  DIFFICULTIES,
  DIFFICULTY_LABEL,
  Difficulty,
  QuestionResponse
} from '../../core/models/question.models';
import { TagResponse } from '../../core/models/tag.models';
import { QuestionFormComponent } from './components/question-form/question-form.component';
import { QuestionCardComponent, QuestionCardVariant } from './components/question-card/question-card.component';
import { TagManagerComponent } from './components/tag-manager/tag-manager.component';
import { countByTag, matchesTags } from '../exams/components/exam-question-picker/question-filters';

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

  readonly difficulties = DIFFICULTIES;
  readonly diffLabel = DIFFICULTY_LABEL;

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
  filterDiff   = signal<Difficulty | ''>('');
  /** Tags marcados. Ninguno = todas las preguntas; si no, las que tengan alguno de ellos. */
  selectedTagIds = signal<ReadonlySet<number>>(new Set());
  /** «Sin tag» marcado: suma las preguntas que no tienen tags. */
  untaggedSelected = signal(false);

  tagCounts     = computed(() => countByTag(this.questions()));
  untaggedCount = computed(() => this.questions().filter(q => q.tags.length === 0).length);
  hasFilters    = computed(() =>
    !!this.filterText().trim() || !!this.filterDiff() || this.selectedTagIds().size > 0 || this.untaggedSelected());

  filtered = computed(() => {
    const text = this.filterText().toLowerCase().trim();
    const diff = this.filterDiff();
    const selected = this.selectedTagIds();
    const untagged = this.untaggedSelected();
    return this.questions().filter(q =>
      (!text || q.text.toLowerCase().includes(text)) &&
      (!diff || q.difficulty === diff) &&
      matchesTags(q, selected, untagged));
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

  isTagSelected(tagId: number): boolean {
    return this.selectedTagIds().has(tagId);
  }

  setTagSelected(tagId: number, selected: boolean): void {
    const ids = new Set(this.selectedTagIds());
    if (selected) ids.add(tagId); else ids.delete(tagId);
    this.selectedTagIds.set(ids);
  }

  clearFilters(): void {
    this.filterText.set('');
    this.filterDiff.set('');
    this.selectedTagIds.set(new Set());
    this.untaggedSelected.set(false);
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
