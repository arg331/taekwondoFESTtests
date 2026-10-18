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
  config: { numberOfQuestions: 5, timeLimitMinutes: null, showScore: true, randomizeOptions: false, randomizeQuestionOrder: false },
  questionIds: [1, 2, 3, 4, 5], generationTags: [], code: 'EXM-1234ABCD', createdAt: '2026-10-12T10:00:00', expiresAt: null, maxAttempts: 1
};

function result(id: number, studentName: string, registered: boolean): ResultResponse {
  return {
    id, examId: 7, examTitle: EXAM.title, studentName, studentClub: 'Club Badajoz', studentEmail: null, registered,
    scoreVisible: true, answers: [], correctAnswers: 5, totalQuestions: 5, score: 100, passed: true,
    timeSpentSeconds: 125, completedAt: `2026-10-12T11:0${id}:00`
  };
}

/** Como llegan del servidor: la entrega más reciente primero. */
const RESULTS = [result(4, 'Carla', false), result(3, 'Zoe', true), result(2, 'Ana', false), result(1, 'Bruno', true)];

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

  it('marca quién es usuario registrado y quién no; primero los registrados y, dentro, por nombre', () => {
    const rows = Array.from(el.querySelectorAll('.results-table tr.mat-mdc-row'));
    const names = rows.map(r => r.querySelector('.student-name')?.textContent?.trim());
    const tags = rows.map(r => r.querySelector('.registration-tag'));

    expect(names).toEqual(['Bruno', 'Zoe', 'Ana', 'Carla']);
    expect(tags.map(tag => tag?.textContent?.trim())).toEqual(['Registrado', 'Registrado', 'No registrado', 'No registrado']);
    expect(tags.map(tag => tag?.classList.contains('registered'))).toEqual([true, true, false, false]);
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
