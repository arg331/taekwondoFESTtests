import { ChangeDetectionStrategy, Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExamService } from '../../core/services/exam.service';
import { QuestionService } from '../../core/services/question.service';
import { TagService } from '../../core/services/tag.service';
import { ExamResponse } from '../../core/models/exam.models';
import { QuestionResponse } from '../../core/models/question.models';
import { TagResponse } from '../../core/models/tag.models';
import { DraftMode, ExamDraftFormComponent } from './components/exam-draft-form/exam-draft-form.component';
import { ExamListComponent } from './components/exam-list/exam-list.component';
import { ExamDetailComponent } from './components/exam-detail/exam-detail.component';

/**
 * Página "Mis exámenes": lista a la izquierda, detalle del seleccionado a la derecha.
 * Los subcomponentes hacen las llamadas y devuelven el examen actualizado.
 */
@Component({
  selector: 'app-exams',
  imports: [
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    ExamDraftFormComponent,
    ExamListComponent,
    ExamDetailComponent
  ],
  templateUrl: './exams.component.html',
  styleUrl: './exams.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamsComponent implements OnInit {
  private examSvc     = inject(ExamService);
  private questionSvc = inject(QuestionService);
  private tagSvc      = inject(TagService);
  private route       = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  loading      = signal(true);
  exams        = signal<ExamResponse[]>([]);
  questions    = signal<QuestionResponse[]>([]);
  tags         = signal<TagResponse[]>([]);
  draftMode    = signal<DraftMode | null>(null);
  selectedExam = signal<ExamResponse | null>(null);

  ngOnInit(): void {
    this.questionSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(q => this.questions.set(q));
    this.tagSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(t => this.tags.set(t));
    this.examSvc.getMine().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: exams => {
        this.exams.set(exams);
        this.loading.set(false);
        // Desde el dashboard se puede llegar con ?select=<id>
        const selectId = Number(this.route.snapshot.queryParamMap.get('select'));
        const exam = exams.find(e => e.id === selectId);
        if (exam) this.select(exam);
      },
      error: () => this.loading.set(false)
    });
  }

  openDraftForm(mode: DraftMode): void {
    this.draftMode.set(mode);
    this.selectedExam.set(null);
  }

  select(exam: ExamResponse): void {
    this.draftMode.set(null);
    this.selectedExam.set(exam);
  }

  onCreated(exam: ExamResponse): void {
    this.exams.update(list => [exam, ...list]);
    this.select(exam);
  }

  onChanged(updated: ExamResponse): void {
    this.exams.update(list => list.map(e => e.id === updated.id ? updated : e));
    this.selectedExam.set(updated);
  }

  onDeleted(id: number): void {
    this.exams.update(list => list.filter(e => e.id !== id));
    this.selectedExam.set(null);
  }
}
