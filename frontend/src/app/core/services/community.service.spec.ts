import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CommunityService } from './community.service';

const API = 'http://localhost:8080/api';

/** Contrato HTTP de los exámenes de prueba de otros profesores (RF-30, RF-31, DE-12). */
describe('CommunityService (contrato HTTP)', () => {
  let community: CommunityService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    community = TestBed.inject(CommunityService);
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

  it('listados, favoritos, preguntas y copia', () => {
    community.list().subscribe();
    expectCall('GET', `${API}/community/exams`);
    community.favorite(7).subscribe();
    expectCall('POST', `${API}/exams/7/favorite`, {});
    community.unfavorite(7).subscribe();
    expectCall('DELETE', `${API}/exams/7/favorite`);
    community.questions(7).subscribe();
    expectCall('GET', `${API}/community/exams/7/questions`);
    community.copyQuestions(7, [1, 2]).subscribe();
    expectCall('POST', `${API}/community/exams/7/copy-questions`, { questionIds: [1, 2] });
  });
});
