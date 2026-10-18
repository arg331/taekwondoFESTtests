import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { ExamService } from '../../../../core/services/exam.service';
import { ResultService } from '../../../../core/services/result.service';
import { EXAM_STATUS_COLOR, EXAM_STATUS_LABEL, ExamResponse } from '../../../../core/models/exam.models';
import { ExamStatisticsResponse, ResultResponse } from '../../../../core/models/result.models';
import { formatDuration } from '../../../../shared/utils/format';
import { fileSlug, saveBlob } from '../../../../shared/utils/download';
import { ExamStatsComponent } from '../exam-stats/exam-stats.component';

/**
 * Vista del profesor: elige uno de sus exámenes publicados y ve estadísticas y participantes.
 */
@Component({
  selector: 'app-teacher-results',
  imports: [
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatTableModule,
    MatButtonModule,
    RouterLink,
    ExamStatsComponent
  ],
  templateUrl: './teacher-results.component.html',
  styleUrl: './teacher-results.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TeacherResultsComponent implements OnInit {
  private examSvc = inject(ExamService);
  private resultSvc = inject(ResultService);
  private snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly statusLabel = EXAM_STATUS_LABEL;
  readonly statusColor = EXAM_STATUS_COLOR;
  readonly formatDuration = formatDuration;
  readonly columns = ['studentName', 'studentClub', 'score', 'passed', 'time', 'date'];

  loadingExams = signal(true);
  exams = signal<ExamResponse[]>([]);
  selectedExam = signal<ExamResponse | null>(null);
  loadingResults = signal(false);
  results = signal<ResultResponse[]>([]);
  stats = signal<ExamStatisticsResponse | null>(null);
  exporting = signal(false);

  publishedExams = computed(() => this.exams().filter(e => e.status !== 'DRAFT'));
  /** Primero los usuarios registrados y, dentro de cada grupo, por nombre. */
  sortedResults = computed(() => [...this.results()].sort((a, b) =>
    Number(b.registered) - Number(a.registered) || a.studentName.localeCompare(b.studentName, 'es')));

  ngOnInit(): void {
    this.examSvc.getMine().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: exams => { this.exams.set(exams); this.loadingExams.set(false); },
      error: () => this.loadingExams.set(false)
    });
  }

  select(exam: ExamResponse): void {
    this.selectedExam.set(exam);
    this.loadingResults.set(true);
    this.results.set([]);
    this.stats.set(null);

    this.resultSvc.getByExam(exam.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: r => { this.results.set(r); this.loadingResults.set(false); },
      error: () => this.loadingResults.set(false)
    });
    this.resultSvc.getStatistics(exam.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(s => this.stats.set(s));
  }

  /** Descarga el Excel de las entregas del examen elegido, ordenadas por club (RF-27). */
  exportExcel(): void {
    const exam = this.selectedExam();
    if (!exam || this.exporting()) return;
    this.exporting.set(true);
    this.resultSvc.exportExcel(exam.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: blob => {
        this.exporting.set(false);
        saveBlob(blob, `resultados-${fileSlug(exam.title)}.xlsx`);
      },
      error: () => {
        this.exporting.set(false);
        this.snackBar.open('No se pudo exportar el Excel. Inténtalo de nuevo.', undefined, { duration: 4000 });
      }
    });
  }
}
