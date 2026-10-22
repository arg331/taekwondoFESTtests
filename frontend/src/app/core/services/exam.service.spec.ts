import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExamService } from './exam.service';
import { ResultService } from './result.service';
import { ExamDraftRequest } from '../models/exam.models';

const API = 'http://localhost:8080/api';

/**
 * Caracterización del contrato HTTP de exámenes y resultados: método, URL y
 * cuerpo que el frontend envía al backend en cada operación.
 */
describe('ExamService y ResultService (contrato HTTP)', () => {
  let exams: ExamService;
  let results: ResultService;
  let http: HttpTestingController;

  const draft: ExamDraftRequest = {
    title: 'Examen', numberOfQuestions: 5, timeLimitMinutes: null, showScore: true,
    randomizeOptions: false, randomizeQuestionOrder: false
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    exams = TestBed.inject(ExamService);
    results = TestBed.inject(ResultService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function expectCall(method: string, url: string, body?: unknown): void {
    const req = http.expectOne(r => r.method === method && r.urlWithParams === url);
    if (body !== undefined) {
      expect(req.request.body).toEqual(body);
    }
    req.flush(null);
  }

  it('consultas del profesor', () => {
    exams.getMine().subscribe();
    expectCall('GET', `${API}/exams/mine`);
    exams.getById(7).subscribe();
    expectCall('GET', `${API}/exams/7`);
    exams.getPublic().subscribe();
    expectCall('GET', `${API}/exams/public`);
    exams.getFavorites().subscribe();
    expectCall('GET', `${API}/favorites`);
  });

  it('creación y edición de borradores', () => {
    exams.createDraft(draft).subscribe();
    expectCall('POST', `${API}/exams/drafts`, draft);
    exams.preGenerate({ ...draft, requiredAnyOfTagIds: [3] }).subscribe();
    expectCall('POST', `${API}/exams/drafts/pre-generated`, { ...draft, requiredAnyOfTagIds: [3] });
    exams.rename(7, { newTitle: 'Nuevo' }).subscribe();
    expectCall('PATCH', `${API}/exams/7/title`, { newTitle: 'Nuevo' });
    exams.changeConfig(7, draft).subscribe();
    expectCall('PATCH', `${API}/exams/7/config`, draft);
    exams.updateQuestions(7, { questionIds: [1, 2] }).subscribe();
    expectCall('PUT', `${API}/exams/7/questions`, { questionIds: [1, 2] });
    exams.deleteDraft(7).subscribe();
    expectCall('DELETE', `${API}/exams/7`);
  });

  it('ciclo de vida y favoritos', () => {
    exams.publish(7, { visibility: 'PUBLIC', accessMode: 'OPEN', expiresAt: null }).subscribe();
    expectCall('POST', `${API}/exams/7/publish`, { visibility: 'PUBLIC', accessMode: 'OPEN', expiresAt: null });
    exams.close(7).subscribe();
    expectCall('POST', `${API}/exams/7/close`, {});
    exams.reopen(7, { newExpiresAt: null }).subscribe();
    expectCall('POST', `${API}/exams/7/reopen`, { newExpiresAt: null });
    exams.changeExpiration(7, { newExpiresAt: '2026-10-10T10:00' }).subscribe();
    expectCall('PATCH', `${API}/exams/7/expiration`, { newExpiresAt: '2026-10-10T10:00' });
    exams.changeVisibility(7, { newVisibility: 'PRIVATE' }).subscribe();
    expectCall('PATCH', `${API}/exams/7/visibility`, { newVisibility: 'PRIVATE' });
    exams.favorite(7).subscribe();
    expectCall('POST', `${API}/exams/7/favorite`, {});
    exams.unfavorite(7).subscribe();
    expectCall('DELETE', `${API}/exams/7/favorite`);
  });

  it('flujo del alumno: el código va codificado en la URL', () => {
    exams.getByCode('EXM 1/2').subscribe();
    expectCall('GET', `${API}/exams/by-code/EXM%201%2F2`);
    exams.startAttempt('EXM-1').subscribe();
    expectCall('POST', `${API}/exams/by-code/EXM-1/attempts`, {});
  });

  it('resultados', () => {
    const submission = {
      examCode: 'EXM-1', attemptToken: 't', studentName: 'Ana', studentClub: null, studentEmail: null,
      answers: [{ questionId: 1, chosenOption: null }]
    };
    results.submit(submission).subscribe();
    expectCall('POST', `${API}/results`, submission);
    results.getByExam(7).subscribe();
    expectCall('GET', `${API}/results/exam/7`);
    results.getStatistics(7).subscribe();
    expectCall('GET', `${API}/results/exam/7/statistics`);
    results.getMyAttempts().subscribe();
    expectCall('GET', `${API}/results/me`);
  });
});
