import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TeacherResultsComponent } from './teacher-results.component';
import { ExamResponse } from '../../../../core/models/exam.models';
import { ResultResponse } from '../../../../core/models/result.models';

const API = 'http://localhost:8080/api';

const EXAM: ExamResponse = {
  id: 7, title: 'Examen de Árbitros 2026', ownerId: 1, status: 'PUBLISHED', visibility: 'PRIVATE', accessMode: 'OPEN',
  config: { numberOfQuestions: 5, timeLimitMinutes: null, showScore: true, randomizeOptions: false, randomizeQuestionOrder: false },
  questionIds: [1, 2, 3, 4, 5], generationTags: [], code: 'EXM-1234ABCD', createdAt: '2026-10-12T10:00:00', expiresAt: null
};

const RESULT: ResultResponse = {
  id: 1, examId: 7, examTitle: EXAM.title, studentName: 'Ana', studentClub: 'Club Badajoz', studentEmail: null,
  scoreVisible: true, answers: [], correctAnswers: 5, totalQuestions: 5, score: 100, passed: true,
  timeSpentSeconds: 125, completedAt: '2026-10-12T11:00:00'
};

/** Resultados del profesor: exportar a Excel (RF-27). */
describe('TeacherResultsComponent', () => {
  let fixture: ComponentFixture<TeacherResultsComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;
  let downloads: string[];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [TeacherResultsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
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
    http.expectOne(`${API}/results/exam/7`).flush([RESULT]);
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
