import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TeacherResultsComponent } from './teacher-results.component';
import { ExamResponse } from '../../../../core/models/exam.models';
import { ResultResponse } from '../../../../core/models/result.models';

const API = 'http://localhost:8080/api';

const EXAM: ExamResponse = {
  id: 7, title: 'Examen de Árbitros 2026', ownerId: 1, status: 'PUBLISHED', visibility: 'PRIVATE', accessMode: 'OPEN',
  config: { numberOfQuestions: 5, timeLimitMinutes: null, showScore: true, showAnswers: true, randomizeOptions: false, randomizeQuestionOrder: false },
  questionIds: [1, 2, 3, 4, 5], generationTags: [], code: 'EXM-1234ABCD', createdAt: '2026-10-12T10:00:00', expiresAt: null, maxAttempts: 1,
  sharedBy: null
};

function result(id: number, studentName: string, registered: boolean, score = 100): ResultResponse {
  return {
    id, examId: 7, examTitle: EXAM.title, studentName, studentClub: 'Club Badajoz', studentEmail: null, registered,
    studentUserId: registered ? 100 + id : null,
    studentKey: registered ? `u:${studentName}` : `n:${studentName.toLowerCase()}`,
    scoreVisible: true, answers: [], correctAnswers: score / 20, totalQuestions: 5, score, passed: score >= 70,
    timeSpentSeconds: 125, completedAt: `2026-10-12T11:0${id}:00`, autoSubmitted: false
  };
}

/**
 * Como llegan del servidor: la entrega más reciente primero. Ana (anónima) lo hizo dos veces. El último
 * intento de Ana y el de Carla se corrigieron solos al caducar (RF-24).
 */
const RESULTS = [{ ...result(5, 'ANA', false, 40), autoSubmitted: true }, { ...result(4, 'Carla', false), autoSubmitted: true }, result(3, 'Zoe', true), result(2, 'Ana', false),
  result(1, 'Bruno', true)];

/** Resultados del profesor: participantes, exportar a Excel (RF-27) e ir al examen. */
describe('TeacherResultsComponent', () => {
  let fixture: ComponentFixture<TeacherResultsComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;
  let downloads: string[];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [TeacherResultsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TeacherResultsComponent);
    el = fixture.nativeElement;
    downloads = [];
    Object.defineProperty(URL, 'createObjectURL', { value: vi.fn(() => 'blob:resultados'), configurable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: vi.fn(), configurable: true });
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      downloads.push(this.download);
    });
    await stable();
    http.expectOne(`${API}/exams/mine`).flush([EXAM]);
    await stable();
    (el.querySelector('.exam-item') as HTMLElement).click();
    http.expectOne(`${API}/results/exam/7`).flush(RESULTS);
    http.expectOne(`${API}/results/exam/7/statistics`).flush(null);
    // Debajo, el acierto por pregunta (RF-28): su resource tiene la app ocupada hasta que responde
    fixture.detectChanges();
    fixture.detectChanges();
    http.expectOne(`${API}/results/exam/7/questions`).flush([]);
    await stable();
  });

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  describe('participantes por páginas de 20 (RNF-09)', () => {
    function names(): string[] {
      return Array.from(el.querySelectorAll('tr.student-row .student-name')).map(n => n.textContent?.trim() ?? '');
    }

    it('con pocos no hay paginador', () => {
      expect(el.querySelector('mat-paginator')).toBeNull();
    });

    it('con más de 20 alumnos, 20 por página; el total sigue contando a todos', async () => {
      const many = Array.from({ length: 25 }, (_, i) => ({
        ...result(i + 1, `Alumno ${String(i + 1).padStart(2, '0')}`, true),
        completedAt: `2026-10-12T11:${String(i).padStart(2, '0')}:00`
      }));
      fixture.componentInstance.results.set(many);
      await stable();

      expect(names()).toHaveLength(20);
      expect(el.textContent).toContain('Participantes: 25 alumnos');
      expect(el.querySelector('.mat-mdc-paginator-range-label')?.textContent?.trim()).toBe('1 – 20 de 25');

      (el.querySelector('.mat-mdc-paginator-navigation-next') as HTMLButtonElement).click();
      await stable();
      expect(names()).toEqual(['Alumno 21', 'Alumno 22', 'Alumno 23', 'Alumno 24', 'Alumno 25']);
    });
  });

  it('marca quién es usuario registrado y quién no; primero los registrados y, dentro, por nombre', () => {
    const rows = Array.from(el.querySelectorAll('.results-table tr.student-row'));
    const names = rows.map(r => r.querySelector('.student-name')?.textContent?.trim());
    const tags = rows.map(r => r.querySelector('.registration-tag'));

    expect(names).toEqual(['Bruno', 'Zoe', 'Ana', 'Carla']);
    expect(tags.map(tag => tag?.textContent?.trim())).toEqual(['Registrado', 'Registrado', 'No registrado', 'No registrado']);
    expect(tags.map(tag => tag?.classList.contains('registered'))).toEqual([true, true, false, false]);
  });

  it('debajo de los participantes, el acierto por pregunta del examen (RF-28)', () => {
    expect(el.querySelector('app-question-stats')).not.toBeNull();
  });

  describe('cada alumno una vez, con sus intentos desplegables (RF-54)', () => {
    function row(name: string): HTMLElement {
      return Array.from(el.querySelectorAll('.results-table tr.student-row'))
        .find(r => r.querySelector('.student-name')?.textContent?.trim() === name) as HTMLElement;
    }

    it('muestra su mejor nota y cuántos intentos lleva', () => {
      expect(el.querySelector('.participants-header')?.textContent).toContain('4 alumnos · 5 entregas');
      expect(row('Ana').querySelector('.score-badge')?.textContent?.trim()).toBe('100');
      expect(row('Ana').textContent).toContain('2 intentos');
      expect(row('Bruno').textContent).not.toContain('intentos');
    });

    it('el nombre de un alumno registrado lleva a todos sus resultados; el de un anónimo, no (RF-55)', () => {
      expect(row('Bruno').querySelector('a.student-name')?.getAttribute('href')).toBe('/results/user/101');
      expect(row('Zoe').querySelector('a.student-name')?.getAttribute('href')).toBe('/results/user/103');
      expect(row('Ana').querySelector('a.student-name')).toBeNull();
    });

    it('al desplegarlo se ven todos sus intentos, el más reciente primero', async () => {
      expect(el.querySelectorAll('.attempt-row').length).toBe(0);

      (row('Ana').querySelector('.attempts-toggle') as HTMLButtonElement).click();
      await stable();

      const attempts = Array.from(el.querySelectorAll('.attempt-row'));
      expect(attempts.length).toBe(2);
      expect(attempts.map(a => a.querySelector('.score-badge')?.textContent?.trim())).toEqual(['40', '100']);
    });

    it('las entregas automáticas llevan su etiqueta, en la fila del alumno y en sus intentos (RF-24)', async () => {
      expect(row('Carla').querySelector('.auto-tag')?.textContent?.trim()).toBe('Entrega automática');
      // La mejor nota de Ana la entregó ella
      expect(row('Ana').querySelector('.auto-tag')).toBeNull();

      (row('Ana').querySelector('.attempts-toggle') as HTMLButtonElement).click();
      await stable();

      const tags = Array.from(el.querySelectorAll('.attempt-row'))
        .map(a => a.querySelector('.auto-tag')?.textContent?.trim() ?? null);
      expect(tags).toEqual(['Entrega automática', null]);
    });
  });

  it('«Ir al examen» lleva a ese examen en Exámenes', () => {
    const link = Array.from(el.querySelectorAll('a')).find(a => a.textContent?.includes('Ir al examen'));

    expect(link?.getAttribute('href')).toBe('/exams?select=7');
  });

  function exportButton(): HTMLButtonElement {
    return Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes('Exportar a Excel')) as HTMLButtonElement;
  }

  it('descarga el Excel con el nombre del examen', async () => {
    exportButton().click();
    http.expectOne(`${API}/results/exam/7/export`).flush(new Blob(['xlsx']));
    await stable();

    expect(downloads).toEqual(['resultados-examen-de-arbitros-2026.xlsx']);
  });

  it('si falla la descarga lo dice', async () => {
    exportButton().click();
    http.expectOne(`${API}/results/exam/7/export`).flush(new Blob(), { status: 500, statusText: 'Error' });
    await stable();

    expect(downloads).toEqual([]);
    expect(document.body.textContent).toContain('No se pudo exportar');
  });
});
