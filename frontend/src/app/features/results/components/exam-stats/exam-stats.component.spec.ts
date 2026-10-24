import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExamStatsComponent } from './exam-stats.component';
import { ExamStatisticsResponse } from '../../../../core/models/result.models';

const STATS: ExamStatisticsResponse = {
  examId: 7, totalAttempts: 5, totalStudents: 4, averageScore: 76.4, passedCount: 3, failedCount: 1,
  highestScore: 100, lowestScore: 40
};

/** Estadísticas de un examen: por alumno, con su mejor nota (RF-26, RF-54). */
describe('ExamStatsComponent', () => {
  let fixture: ComponentFixture<ExamStatsComponent>;
  let el: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ExamStatsComponent] });
    fixture = TestBed.createComponent(ExamStatsComponent);
    fixture.componentRef.setInput('stats', STATS);
    fixture.detectChanges();
    el = fixture.nativeElement;
  });

  function stat(label: string): string | undefined {
    return Array.from(el.querySelectorAll('.stat-mini'))
      .find(s => s.querySelector('.stat-l')?.textContent?.trim() === label)?.querySelector('.stat-n')?.textContent?.trim();
  }

  it('cuenta alumnos y, aparte, entregas', () => {
    expect(stat('Alumnos')).toBe('4');
    expect(stat('Entregas')).toBe('5');
  });

  it('el porcentaje de aprobados es sobre los alumnos, no sobre las entregas', () => {
    expect(el.querySelector('.pie-legend')?.textContent).toContain('Aprobados: 3 (75%)');
    expect(el.querySelector('.pie-legend')?.textContent).toContain('Suspensos: 1 (25%)');
  });
});
