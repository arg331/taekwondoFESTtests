import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule, FormArray } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatRadioModule } from '@angular/material/radio';
import { MatBadgeModule } from '@angular/material/badge';
import { QuestionService } from '../../core/services/question.service';
import { TagService } from '../../core/services/tag.service';
import { QuestionResponse, Difficulty } from '../../core/models/question.models';
import { TagResponse } from '../../core/models/tag.models';

@Component({
  selector: 'app-questions',
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
    MatChipsModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDividerModule,
    MatRadioModule,
    MatBadgeModule
  ],
  templateUrl: './questions.component.html',
  styleUrl: './questions.component.scss'
})
export class QuestionsComponent implements OnInit {
  private questionSvc = inject(QuestionService);
  private tagSvc      = inject(TagService);
  private fb          = inject(FormBuilder);

  loading     = signal(true);
  saving      = signal(false);
  questions   = signal<QuestionResponse[]>([]);
  tags        = signal<TagResponse[]>([]);
  showForm    = signal(false);
  editingId   = signal<number | null>(null);
  filterText  = signal('');
  filterDiff  = signal<Difficulty | ''>('');
  filterTagId = signal<number | ''>('');

  difficulties: Difficulty[] = ['FACIL', 'MEDIO', 'DIFICIL'];
  diffLabel: Record<Difficulty, string> = {
    FACIL: 'Fácil', MEDIO: 'Medio', DIFICIL: 'Difícil'
  };
  diffColor: Record<Difficulty, string> = {
    FACIL: '#4caf50', MEDIO: '#ff9800', DIFICIL: '#f44336'
  };

  filtered = computed(() => {
    let list = this.questions();
    if (this.filterText()) {
      const t = this.filterText().toLowerCase();
      list = list.filter(q => q.text.toLowerCase().includes(t));
    }
    if (this.filterDiff()) {
      list = list.filter(q => q.difficulty === this.filterDiff());
    }
    if (this.filterTagId()) {
      list = list.filter(q => q.tags.some(t => t.id === this.filterTagId()));
    }
    return list;
  });

  form = this.fb.group({
    text:          ['', [Validators.required, Validators.minLength(10)]],
    options:       this.fb.array([
      this.fb.control('', Validators.required),
      this.fb.control('', Validators.required),
      this.fb.control('', Validators.required),
      this.fb.control('', Validators.required),
    ]),
    correctAnswer: [0, Validators.required],
    explanation:   ['', Validators.required],
    difficulty:    ['FACIL' as Difficulty, Validators.required],
    tagIds:        [[] as number[]]
  });

  get optionsArray() {
    return this.form.get('options') as FormArray;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.tagSvc.getAll().subscribe(tags => this.tags.set(tags));
    this.questionSvc.getAll().subscribe({
      next: q => { this.questions.set(q); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({
      text: '', options: ['', '', '', ''],
      correctAnswer: 0, explanation: '',
      difficulty: 'FACIL', tagIds: []
    });
    this.showForm.set(true);
  }

  openEdit(q: QuestionResponse): void {
    this.editingId.set(q.id);
    this.form.setValue({
      text: q.text,
      options: [...q.options],
      correctAnswer: q.correctAnswer,
      explanation: q.explanation,
      difficulty: q.difficulty,
      tagIds: q.tags.map(t => t.id)
    });
    this.showForm.set(true);
  }

  cancel(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  save(): void {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);

    const value = this.form.value as any;
    const request = {
      text: value.text,
      options: value.options,
      correctAnswer: Number(value.correctAnswer),
      explanation: value.explanation,
      difficulty: value.difficulty,
      tagIds: value.tagIds ?? []
    };

    const op = this.editingId()
      ? this.questionSvc.edit(this.editingId()!, request)
      : this.questionSvc.create(request);

    op.subscribe({
      next: (q) => {
        if (this.editingId()) {
          this.questions.update(list => list.map(x => x.id === q.id ? q : x));
        } else {
          this.questions.update(list => [q, ...list]);
        }
        this.saving.set(false);
        this.showForm.set(false);
        this.editingId.set(null);
      },
      error: () => this.saving.set(false)
    });
  }

  delete(q: QuestionResponse): void {
    if (!confirm(`¿Eliminar la pregunta "${q.text.substring(0, 50)}..."?`)) return;
    this.questionSvc.delete(q.id).subscribe({
      next: () => this.questions.update(list => list.filter(x => x.id !== q.id))
    });
  }
}
