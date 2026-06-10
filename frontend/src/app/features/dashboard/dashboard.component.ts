import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../core/services/auth.service';
import { ExamService } from '../../core/services/exam.service';
import { ScraperService } from '../../core/services/scraper.service';
import { ExamResponse } from '../../core/models/exam.models';
import { CursoArbitrajeResponse } from '../../core/models/scraper.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  auth       = inject(AuthService);
  examSvc    = inject(ExamService);
  scraperSvc = inject(ScraperService);
  router     = inject(Router);

  loading        = signal(false);
  exams          = signal<ExamResponse[]>([]);
  publicExams    = signal<ExamResponse[]>([]);
  loadingPublic  = signal(false);
  cursos         = signal<CursoArbitrajeResponse[]>([]);
  loadingCursos  = signal(false);

  drafts    = computed(() => this.exams().filter(e => e.status === 'DRAFT'));
  published = computed(() => this.exams().filter(e => e.status === 'PUBLISHED'));
  expired   = computed(() => this.exams().filter(e => e.status === 'EXPIRED'));

  ngOnInit(): void {
    if (this.auth.isAdmin()) {
      this.loading.set(true);
      this.examSvc.getMine().subscribe({
        next: exams => { this.exams.set(exams); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
      this.loadingCursos.set(true);
      this.scraperSvc.getCursosArbitraje().subscribe({
        next: cursos => { this.cursos.set(cursos); this.loadingCursos.set(false); },
        error: () => this.loadingCursos.set(false)
      });
    } else {
      this.loadingPublic.set(true);
      this.examSvc.getPublic().subscribe({
        next: exams => { this.publicExams.set(exams); this.loadingPublic.set(false); },
        error: () => this.loadingPublic.set(false)
      });
    }
  }

  getStatusColor(status: string): string {
    return { DRAFT: 'accent', PUBLISHED: 'primary', EXPIRED: 'warn' }[status] ?? 'primary';
  }

  getStatusLabel(status: string): string {
    return { DRAFT: 'Borrador', PUBLISHED: 'Publicado', EXPIRED: 'Expirado' }[status] ?? status;
  }

  goToExam(id: number): void {
    this.router.navigate(['/exams'], { queryParams: { select: id } });
  }

  goToExamTake(code: string): void {
    this.router.navigate(['/exam', code]);
  }

  isCursoError(curso: CursoArbitrajeResponse): boolean {
    return curso.mes === 'Error' || curso.mes === 'Info';
  }
}
