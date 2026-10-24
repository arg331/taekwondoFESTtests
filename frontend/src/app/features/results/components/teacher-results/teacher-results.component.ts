import { ChangeDetectionStrategy, Component, OnInit, computed, inject, linkedSignal, signal, DestroyRef } from '@angular/core';
import { MatPaginatorIntl, MatPaginatorModule } from '@angular/material/paginator';
import { PaginatorIntlEs } from '../../../../shared/paginator-intl-es';
import { PAGE_SIZE, clampPage, pageItems } from '../../../../shared/utils/pagination';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
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
import { QuestionStatsComponent } from '../question-stats/question-stats.component';

/** Un alumno en los resultados: su mejor entrega y todas, de la más reciente a la más antigua. */
interface StudentResults {
  key: string;
  best: ResultResponse;
  attempts: ResultResponse[];
}

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
    MatButtonModule,
    MatPaginatorModule,
    RouterLink,
    ExamStatsComponent,
    QuestionStatsComponent
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginatorIntlEs }],
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

  loadingExams = signal(true);
  exams = signal<ExamResponse[]>([]);
  selectedExam = signal<ExamResponse | null>(null);
  loadingResults = signal(false);
  results = signal<ResultResponse[]>([]);
  stats = signal<ExamStatisticsResponse | null>(null);
  exporting = signal(false);

  publishedExams = computed(() => this.exams().filter(e => e.status !== 'DRAFT'));
  /**
   * Cada alumno una vez, con su mejor nota y sus intentos del más reciente al más antiguo (RF-54).
   * Primero los usuarios registrados y, dentro de cada grupo, por nombre.
   */
  students = computed<StudentResults[]>(() => {
    const byStudent = new Map<string, ResultResponse[]>();
    for (const r of this.results()) byStudent.set(r.studentKey, [...(byStudent.get(r.studentKey) ?? []), r]);
    return [...byStudent.entries()]
      .map(([key, attempts]) => {
        const sorted = [...attempts].sort((a, b) => b.completedAt.localeCompare(a.completedAt));
        const best = sorted.reduce((top, r) => (r.score ?? 0) > (top.score ?? 0) ? r : top);
        return { key, best, attempts: sorted };
      })
      .sort((a, b) => Number(b.best.registered) - Number(a.best.registered)
        || a.best.studentName.localeCompare(b.best.studentName, 'es'));
  });
  readonly pageSize = PAGE_SIZE;
  /**
   * Página de participantes, desde 0 (RNF-09). Se pagina aquí porque cada alumno agrupa varias entregas:
   * paginando las entregas en el servidor, un alumno podría quedar partido. Otro examen vuelve a la primera.
   */
  page = linkedSignal({ source: this.selectedExam, computation: () => 0 });
  currentPage = computed(() => clampPage(this.page(), this.students().length));
  visibleStudents = computed(() => pageItems(this.students(), this.currentPage()));
  /** Alumno con los intentos desplegados; se cierra al cambiar de examen. */
  expandedKey = signal<string | null>(null);

  toggleAttempts(key: string): void {
    this.expandedKey.update(current => current === key ? null : key);
  }

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
    this.expandedKey.set(null);
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
