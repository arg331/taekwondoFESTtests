import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { ExamService } from '../../../../core/services/exam.service';
import { ResultService } from '../../../../core/services/result.service';
import { EXAM_STATUS_COLOR, EXAM_STATUS_LABEL, ExamResponse } from '../../../../core/models/exam.models';
import { ExamStatisticsResponse, ResultResponse } from '../../../../core/models/result.models';
import { formatDuration } from '../../../../shared/utils/format';
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
    ExamStatsComponent
  ],
  templateUrl: './teacher-results.component.html',
  styleUrl: './teacher-results.component.scss'
})
export class TeacherResultsComponent implements OnInit {
  private examSvc = inject(ExamService);
  private resultSvc = inject(ResultService);

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

  publishedExams = computed(() => this.exams().filter(e => e.status !== 'DRAFT'));

  ngOnInit(): void {
    this.examSvc.getMine().subscribe({
      next: exams => { this.exams.set(exams); this.loadingExams.set(false); },
      error: () => this.loadingExams.set(false)
    });
  }

  select(exam: ExamResponse): void {
    this.selectedExam.set(exam);
    this.loadingResults.set(true);
    this.results.set([]);
    this.stats.set(null);

    this.resultSvc.getByExam(exam.id).subscribe({
      next: r => { this.results.set(r); this.loadingResults.set(false); },
      error: () => this.loadingResults.set(false)
    });
    this.resultSvc.getStatistics(exam.id).subscribe(s => this.stats.set(s));
  }
}
