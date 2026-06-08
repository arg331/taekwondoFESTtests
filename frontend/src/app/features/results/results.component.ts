import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../core/services/auth.service';
import { ExamService } from '../../core/services/exam.service';
import { ResultService } from '../../core/services/result.service';
import { ExamResponse } from '../../core/models/exam.models';
import { ResultResponse, ExamStatisticsResponse } from '../../core/models/result.models';

@Component({
  selector: 'app-results',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule
  ],
  templateUrl: './results.component.html',
  styleUrl: './results.component.scss'
})
export class ResultsComponent implements OnInit {
  auth       = inject(AuthService);
  examSvc    = inject(ExamService);
  resultSvc  = inject(ResultService);

  // ── Estado admin ───────────────────────────────────
  loadingExams  = signal(true);
  exams         = signal<ExamResponse[]>([]);
  selectedExam  = signal<ExamResponse | null>(null);
  loadingResults = signal(false);
  results       = signal<ResultResponse[]>([]);
  stats         = signal<ExamStatisticsResponse | null>(null);

  publishedExams = computed(() =>
    this.exams().filter(e => e.status === 'PUBLISHED' || e.status === 'EXPIRED')
  );

  // ── Estado student ────────────────────────────────
  loadingMine  = signal(true);
  myAttempts   = signal<ResultResponse[]>([]);

  // ── Tabla ─────────────────────────────────────────
  displayedColumns = ['studentName', 'studentClub', 'score', 'passed', 'time', 'date'];

  ngOnInit(): void {
    if (this.auth.isAdmin()) {
      this.examSvc.getMine().subscribe({
        next: e => { this.exams.set(e); this.loadingExams.set(false); },
        error: () => this.loadingExams.set(false)
      });
    } else {
      this.resultSvc.getMyAttempts().subscribe({
        next: r => { this.myAttempts.set(r); this.loadingMine.set(false); },
        error: () => this.loadingMine.set(false)
      });
    }
  }

  selectExam(exam: ExamResponse): void {
    this.selectedExam.set(exam);
    this.loadingResults.set(true);
    this.results.set([]);
    this.stats.set(null);

    this.resultSvc.getByExam(exam.id).subscribe({
      next: r => { this.results.set(r); this.loadingResults.set(false); },
      error: () => this.loadingResults.set(false)
    });
    this.resultSvc.getStatistics(exam.id).subscribe({
      next: s => this.stats.set(s)
    });
  }

  // ── Gráfica tarta SVG ─────────────────────────────
  get pieAprobados(): string {
    const s = this.stats();
    if (!s || s.totalAttempts === 0) return '';
    const pct = s.passedCount / s.totalAttempts;
    const angle = pct * 2 * Math.PI;
    const x = Math.cos(angle - Math.PI / 2);
    const y = Math.sin(angle - Math.PI / 2);
    const large = pct > 0.5 ? 1 : 0;
    // Arco desde arriba (0,-1) hasta (x,y)
    return `M 0 0 L 0 -1 A 1 1 0 ${large} 1 ${x} ${y} Z`;
  }

  get pieSuspensos(): string {
    const s = this.stats();
    if (!s || s.totalAttempts === 0) return '';
    const pct = s.passedCount / s.totalAttempts;
    const angle = pct * 2 * Math.PI;
    const x = Math.cos(angle - Math.PI / 2);
    const y = Math.sin(angle - Math.PI / 2);
    const large = pct < 0.5 ? 1 : 0;
    return `M 0 0 L ${x} ${y} A 1 1 0 ${large} 1 0 -1 Z`;
  }

  // ── Helpers ───────────────────────────────────────
  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}m ${s}s`;
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('es-ES', {
      day: '2-digit', month: '2-digit', year: '2-digit',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
