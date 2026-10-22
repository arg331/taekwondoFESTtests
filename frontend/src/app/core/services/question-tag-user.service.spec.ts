import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { QuestionService } from './question.service';
import { TagService } from './tag.service';
import { UserService } from './user.service';
import { QuestionRequest } from '../models/question.models';

const API = 'http://localhost:8080/api';

/** Caracterización del contrato HTTP de preguntas, tags y usuarios. */
describe('QuestionService, TagService y UserService (contrato HTTP)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
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

  it('preguntas', () => {
    const questions = TestBed.inject(QuestionService);
    const request: QuestionRequest = {
      text: '¿?', options: ['A', 'B'], correctAnswer: 1, explanation: '', difficulty: 'FACIL', tagIds: [2]
    };

    questions.getAll().subscribe();
    expectCall('GET', `${API}/questions`);
    questions.getById(4).subscribe();
    expectCall('GET', `${API}/questions/4`);
    questions.create(request).subscribe();
    expectCall('POST', `${API}/questions`, request);
    questions.edit(4, request).subscribe();
    expectCall('PUT', `${API}/questions/4`, request);
    questions.delete(4).subscribe();
    expectCall('DELETE', `${API}/questions/4`);
  });

  it('búsqueda de preguntas: tags separados por comas y texto opcionales', () => {
    const questions = TestBed.inject(QuestionService);

    questions.search([1, 2], 'patada').subscribe();
    expectCall('GET', `${API}/questions/search?tagIds=1,2&textContains=patada`);
    questions.search([], '').subscribe();
    expectCall('GET', `${API}/questions/search`);
  });

  it('tags', () => {
    const tags = TestBed.inject(TagService);

    tags.getAll().subscribe();
    expectCall('GET', `${API}/tags`);
    tags.getById(3).subscribe();
    expectCall('GET', `${API}/tags/3`);
    tags.create({ name: 'Reglamento', color: '#C62828' }).subscribe();
    expectCall('POST', `${API}/tags`, { name: 'Reglamento', color: '#C62828' });
    tags.rename(3, { newName: 'Otro' }).subscribe();
    expectCall('PATCH', `${API}/tags/3`, { newName: 'Otro' });
  });

  it('usuarios', () => {
    const users = TestBed.inject(UserService);

    users.getAll().subscribe();
    expectCall('GET', `${API}/users`);
    users.promote(9).subscribe();
    expectCall('PATCH', `${API}/users/9/promote`, {});
    users.demote(9).subscribe();
    expectCall('PATCH', `${API}/users/9/demote`, {});
  });
});
