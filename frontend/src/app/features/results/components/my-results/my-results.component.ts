import { ChangeDetectionStrategy, Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { ResultService } from '../../../../core/services/result.service';
import { ResultResponse } from '../../../../core/models/result.models';
import { formatDuration } from '../../../../shared/utils/format';

/**
 * Vista del alumno: sus intentos. La nota solo aparece si el examen la muestra.
 */
@Component({
  selector: 'app-my-results',
  imports: [DatePipe, MatIconModule, MatProgressSpinnerModule, MatTableModule],
  templateUrl: './my-results.component.html',
  styleUrl: './my-results.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MyResultsComponent implements OnInit {
  private resultSvc = inject(ResultService);
  private readonly destroyRef = inject(DestroyRef);

  readonly formatDuration = formatDuration;
  readonly columns = ['exam', 'correct', 'score', 'time', 'date'];

  loading = signal(true);
  attempts = signal<ResultResponse[]>([]);

  ngOnInit(): void {
    this.resultSvc.getMyAttempts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: r => { this.attempts.set(r); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
