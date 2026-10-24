import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { UserResultsComponent } from './user-results.component';
import { ResultResponse, UserResultsResponse } from '../../../../core/models/result.models';

const API = 'http://localhost:8080/api';

function result(id: number, examTitle: string, score: number, autoSubmitted = false): ResultResponse {
  return {
    id, examId: id * 10, examTitle, studentName: 'Ana Pérez', studentClub: 'Club Badajoz', studentEmail: null,
    registered: true, studentUserId: 42, studentKey: 'u:42', scoreVisible: true, answers: [], correctAnswers: score / 20,
    totalQuestions: 5, score, passed: score >= 70, timeSpentSeconds: 125, completedAt: `2026-10-1${id}T10:00:00`,
    autoSubmitted
  };
}

const ANA: UserResultsResponse = {
  userId: 42, username: 'anaperez', displayName: 'Ana Pérez', clubName: 'Club Badajoz',
  results: [result(2, 'Examen tipo C', 40, true), result(1, 'Examen de Árbitros 2026', 100)]
};

/** Resultados de un usuario (RF-55): el profesor ve los de sus exámenes; el administrador, todos. */
describe('UserResultsComponent', () => {
  let fixture: ComponentFixture<UserResultsComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function render(respond: (url: string) => void = url => http.expectOne(url).flush(ANA)): Promise<void> {
    TestBed.configureTestingModule({
      imports: [UserResultsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(UserResultsComponent);
    // Llega de la ruta /results/user/:id
    fixture.componentRef.setInput('id', '42');
    el = fixture.nativeElement;
    fixture.detectChanges();
    respond(`${API}/results/user/42`);
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('muestra quién es y sus entregas, la más reciente primero, con nota, resultado y entrega automática', async () => {
    await render();

    expect(el.querySelector('.user-title')?.textContent).toContain('Ana Pérez');
    expect(el.querySelector('.user-meta')?.textContent).toContain('anaperez');
    expect(el.querySelector('.user-meta')?.textContent).toContain('Club Badajoz');
    expect(el.textContent).toContain('2 entregas');
    const rows = Array.from(el.querySelectorAll('.result-row'));
    expect(rows.map(r => r.querySelector('.exam-title')?.textContent?.trim())).toEqual(['Examen tipo C', 'Examen de Árbitros 2026']);
    expect(rows.map(r => r.querySelector('.score-badge')?.textContent?.trim())).toEqual(['40', '100']);
    expect(rows[0].textContent).toContain('Suspenso');
    expect(rows[0].querySelector('.auto-tag')?.textContent?.trim()).toBe('Entrega automática');
    expect(rows[1].textContent).toContain('Aprobado');
    expect(rows[1].querySelector('.auto-tag')).toBeNull();
  });

  it('sin entregas que pueda ver, lo dice', async () => {
    await render(url => http.expectOne(url).flush({ ...ANA, results: [] }));

    expect(el.querySelector('.user-title')?.textContent).toContain('Ana Pérez');
    expect(el.textContent).toContain('No hay entregas de este usuario en los exámenes que puedes ver.');
  });

  it('si el usuario no existe, lo dice', async () => {
    await render(url => http.expectOne(url).flush({ message: 'No existe un usuario con id 42' }, { status: 404, statusText: 'Not Found' }));

    expect(el.textContent).toContain('No existe ese usuario.');
  });
});
