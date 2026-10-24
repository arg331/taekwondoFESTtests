import { ChangeDetectionStrategy, Component, inject, signal, computed, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../core/services/auth.service';
import { ExamCodeEntryComponent } from '../../shared/components/exam-code-entry/exam-code-entry.component';
import { ShareInvitationsComponent } from './components/share-invitations/share-invitations.component';
import { ExamService } from '../../core/services/exam.service';
import { EXAM_STATUS_LABEL, ExamResponse, ExamStatus, VISIBILITY_COLOR, VISIBILITY_ICON, VISIBILITY_LABEL } from '../../core/models/exam.models';
import { APP_NAME } from '../../core/app-brand';

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
    MatDividerModule,
    ExamCodeEntryComponent,
    ShareInvitationsComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
  readonly appName = APP_NAME;
  readonly visibilityLabel = VISIBILITY_LABEL;
  readonly visibilityIcon = VISIBILITY_ICON;
  readonly visibilityColor = VISIBILITY_COLOR;
  auth       = inject(AuthService);
  examSvc    = inject(ExamService);
  router     = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  loading        = signal(false);
  exams          = signal<ExamResponse[]>([]);
  publicExams    = signal<ExamResponse[]>([]);
  loadingPublic  = signal(false);

  drafts    = computed(() => this.exams().filter(e => e.status === 'DRAFT'));
  published = computed(() => this.exams().filter(e => e.status === 'PUBLISHED'));
  expired   = computed(() => this.exams().filter(e => e.status === 'EXPIRED'));

  ngOnInit(): void {
    if (this.auth.canTeach()) {
      this.loading.set(true);
      this.examSvc.getMine().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        // Solo los propios: los compartidos conmigo (RF-51) están en «Exámenes» y «Resultados»
        next: exams => { this.exams.set(exams.filter(e => !e.sharedBy)); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
    } else {
      this.loadingPublic.set(true);
      this.examSvc.getPublic().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: exams => { this.publicExams.set(exams); this.loadingPublic.set(false); },
        error: () => this.loadingPublic.set(false)
      });
    }
  }

  getStatusColor(status: ExamStatus): string {
    return { DRAFT: 'accent', PUBLISHED: 'primary', EXPIRED: 'warn' }[status];
  }

  getStatusLabel(status: ExamStatus): string {
    return EXAM_STATUS_LABEL[status];
  }

  goToExam(id: number): void {
    this.router.navigate(['/exams'], { queryParams: { select: id } });
  }

  goToExamTake(code: string): void {
    this.router.navigate(['/exam', code]);
  }
}
