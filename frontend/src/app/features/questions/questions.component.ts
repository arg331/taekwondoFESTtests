import { ChangeDetectionStrategy, Component, inject, signal, computed, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
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
import { QuestionCardComponent } from './components/question-card/question-card.component';
import { TagManagerComponent } from './components/tag-manager/tag-manager.component';

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

  filterText  = signal('');
  filterDiff  = signal<Difficulty | ''>('');
  filterTagId = signal<number | ''>('');

  filtered = computed(() => {
    const text = this.filterText().toLowerCase().trim();
    const diff = this.filterDiff();
    const tagId = this.filterTagId();
    return this.questions().filter(q =>
      (!text || q.text.toLowerCase().includes(text)) &&
      (!diff || q.difficulty === diff) &&
      (!tagId || q.tags.some(t => t.id === tagId)));
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
