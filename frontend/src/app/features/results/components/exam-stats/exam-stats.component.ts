import { Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { ExamStatisticsResponse } from '../../../../core/models/result.models';

/**
 * Tarjetas de estadísticas y tarta de aprobados/suspensos de un examen.
 */
@Component({
  selector: 'app-exam-stats',
  imports: [DecimalPipe, MatCardModule],
  templateUrl: './exam-stats.component.html',
  styleUrl: './exam-stats.component.scss'
})
export class ExamStatsComponent {
  stats = input.required<ExamStatisticsResponse>();

  passedPct = computed(() => {
    const s = this.stats();
    return s.totalAttempts ? s.passedCount / s.totalAttempts : 0;
  });

  /** Sector de aprobados, empezando arriba y en sentido horario (círculo de radio 1). */
  passedPath = computed(() => this.sector(0, this.passedPct()));
  failedPath = computed(() => this.sector(this.passedPct(), 1));

  private sector(from: number, to: number): string {
    const point = (fraction: number) => {
      const angle = fraction * 2 * Math.PI - Math.PI / 2;
      return `${Math.cos(angle)} ${Math.sin(angle)}`;
    };
    const largeArc = to - from > 0.5 ? 1 : 0;
    return `M 0 0 L ${point(from)} A 1 1 0 ${largeArc} 1 ${point(to)} Z`;
  }
}
