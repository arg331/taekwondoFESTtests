import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ExamTakeComponent } from './exam-take.component';
import { ExamAttemptResponse, ExamResponse } from '../../core/models/exam.models';
import { ResultResponse } from '../../core/models/result.models';

const API = 'http://localhost:8080/api';

function exam(overrides: Partial<ExamResponse> = {}): ExamResponse {
  return {
    id: 1, title: 'Examen de árbitros', ownerId: 1, status: 'PUBLISHED', visibility: 'PUBLIC', accessMode: 'OPEN',
    config: { numberOfQuestions: 3, timeLimitMinutes: null, showScore: true, randomizeOptions: true,
      randomizeQuestionOrder: true },
    questionIds: [10, 20, 30], generationTags: [], code: 'EXM-ABC', createdAt: '2026-10-08T10:00:00',
    expiresAt: null, ...overrides
  };
}

/** Preguntas tal como las manda el servidor: orden y opciones ya barajados. */
const ATTEMPT: ExamAttemptResponse = {
  attemptToken: 'intento-123',
  questions: [
    { id: 20, text: 'Segunda', options: [{ index: 2, text: 'C' }, { index: 0, text: 'A' }, { index: 1, text: 'B' }] },
    { id: 10, text: 'Primera', options: [{ index: 1, text: 'Sí' }, { index: 0, text: 'No' }] },
    { id: 30, text: 'Tercera', options: [{ index: 0, text: 'X' }, { index: 1, text: 'Y' }] }
  ]
};

/**
 * Caracterización del flujo del alumno: portada → preguntas → entrega → resultado.
 * Interactúa como un usuario (DOM) y comprueba las peticiones al backend.
 */
describe('ExamTakeComponent', () => {
  let fixture: ComponentFixture<ExamTakeComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function render(examResponse: ExamResponse): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ExamTakeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExamTakeComponent);
    fixture.componentRef.setInput('code', examResponse.code);
    el = fixture.nativeElement;
    await stable();
    http.expectOne(`${API}/exams/by-code/${examResponse.code}`).flush(examResponse);
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function button(text: string): HTMLButtonElement {
    const found = Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes(text));
    if (!found) throw new Error(`No hay botón "${text}"`);
    return found as HTMLButtonElement;
  }

  async function startAs(name: string): Promise<void> {
    const input = el.querySelector('app-exam-access-card input') as HTMLInputElement;
    input.value = name;
    input.dispatchEvent(new Event('input'));
    await stable();
    button('Comenzar examen').click();
    await stable();
    const req = http.expectOne(`${API}/exams/by-code/EXM-ABC/attempts`);
    expect(req.request.method).toBe('POST');
    req.flush(ATTEMPT);
    await stable();
  }

  async function choose(optionText: string): Promise<void> {
    const option = Array.from(el.querySelectorAll('.option-item'))
      .find(o => o.querySelector('span')?.textContent?.trim() === optionText) as HTMLElement;
    option.click();
    await stable();
  }

  async function goToQuestion(number: number): Promise<void> {
    (el.querySelectorAll('.nav-btn')[number - 1] as HTMLElement).click();
    await stable();
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => {
    http.verify();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('portada: muestra título y número de preguntas', async () => {
    await render(exam());

    expect(el.textContent).toContain('Examen de árbitros');
    expect(el.textContent).toContain('3 preguntas');
  });

  it('examen no disponible: muestra el mensaje del servidor', async () => {
    TestBed.configureTestingModule({
      imports: [ExamTakeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExamTakeComponent);
    fixture.componentRef.setInput('code', 'EXM-XXX');
    el = fixture.nativeElement;
    await stable();

    http.expectOne(`${API}/exams/by-code/EXM-XXX`)
      .flush({ message: 'Este examen no está disponible en este momento' }, { status: 409, statusText: 'Conflict' });
    await stable();

    expect(el.textContent).toContain('Examen no disponible');
    expect(el.textContent).toContain('Este examen no está disponible en este momento');
  });

  it('muestra las preguntas en el orden del servidor y envía el índice ORIGINAL de cada opción', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await render(exam());
    await startAs('  Ana Pérez  ');

    expect(el.querySelector('.question-text')?.textContent).toContain('Segunda');
    await choose('A');
    await goToQuestion(2);
    await choose('Sí');
    await goToQuestion(3);
    button('Enviar examen').click();
    await stable();

    const req = http.expectOne(`${API}/results`);
    expect(req.request.body).toEqual({
      examCode: 'EXM-ABC',
      attemptToken: 'intento-123',
      studentName: 'Ana Pérez',
      studentClub: null,
      studentEmail: null,
      answers: [
        { questionId: 20, chosenOption: 0 },
        { questionId: 10, chosenOption: 1 },
        { questionId: 30, chosenOption: null }
      ]
    });
    expect(window.confirm).toHaveBeenCalledWith('Tienes 1 pregunta(s) sin responder. ¿Enviar el examen igualmente?');
    req.flush(null);
  });

  it('si el alumno cancela la confirmación no se envía nada', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    await render(exam());
    await startAs('Ana');
    await goToQuestion(3);

    button('Enviar examen').click();
    await stable();

    http.expectNone(`${API}/results`);
  });

  it('resultado con nota: muestra la puntuación y el detalle con el texto de las opciones', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await render(exam());
    await startAs('Ana');
    await goToQuestion(3);
    button('Enviar examen').click();
    await stable();

    const result: ResultResponse = {
      id: 5, examId: 1, examTitle: 'Examen de árbitros', studentName: 'Ana', studentClub: null, studentEmail: null,
      scoreVisible: true, correctAnswers: 1, totalQuestions: 3, score: 33, passed: false, timeSpentSeconds: 40,
      completedAt: '2026-10-08T10:10:00',
      answers: [
        { questionId: 10, studentAnswer: 1, correctAnswer: 1, correct: true },
        { questionId: 20, studentAnswer: null, correctAnswer: 2, correct: false },
        { questionId: 30, studentAnswer: 0, correctAnswer: 1, correct: false }
      ]
    };
    http.expectOne(`${API}/results`).flush(result);
    await stable();

    const rows = Array.from(el.querySelectorAll('.answer-row')).map(r => r.textContent?.replace(/\s+/g, ' ').trim());
    expect(el.querySelector('.score-value')?.textContent).toContain('33');
    expect(el.textContent).toContain('SUSPENSO');
    expect(rows[0]).toContain('1. Segunda');
    expect(rows[0]).toContain('Tu respuesta: Sin responder');
    expect(rows[0]).toContain('Correcta: C');
    expect(rows[1]).toContain('2. Primera');
    expect(rows[1]).toContain('Tu respuesta: Sí');
    expect(rows[2]).toContain('Tu respuesta: X');
    expect(rows[2]).toContain('Correcta: Y');
  });

  it('resultado con nota oculta: solo confirma la entrega', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await render(exam({ config: { ...exam().config, showScore: false } }));
    await startAs('Ana');
    await goToQuestion(3);
    button('Enviar examen').click();
    await stable();

    http.expectOne(`${API}/results`).flush({
      id: 5, examId: 1, examTitle: 'X', studentName: 'Ana', studentClub: null, studentEmail: null,
      scoreVisible: false, answers: null, correctAnswers: null, totalQuestions: 3, score: null, passed: null,
      timeSpentSeconds: 40, completedAt: '2026-10-08T10:10:00'
    } satisfies ResultResponse);
    await stable();

    expect(el.textContent).toContain('Examen completado');
    expect(el.querySelector('.score-value')).toBeNull();
  });

  it('con tiempo límite: muestra el reloj y al llegar a cero envía sin pedir confirmación', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: false });
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    await render(exam({ config: { ...exam().config, timeLimitMinutes: 1 } }));
    await startAs('Ana');

    expect(el.querySelector('.timer')?.textContent).toContain('01:00');
    vi.advanceTimersByTime(30_000);
    await stable();
    expect(el.querySelector('.timer')?.textContent).toContain('00:30');
    vi.advanceTimersByTime(30_000);
    await stable();

    const req = http.expectOne(`${API}/results`);
    expect(req.request.body.answers.every((a: { chosenOption: number | null }) => a.chosenOption === null)).toBe(true);
    expect(confirm).not.toHaveBeenCalled();
    req.flush(null);
  });

  it('solo registrados y sin sesión: pide iniciar sesión y volver al examen', async () => {
    await render(exam({ accessMode: 'REGISTERED_ONLY' }));

    const loginLink = Array.from(el.querySelectorAll('a')).find(a => a.textContent?.includes('Iniciar sesión'));
    expect(el.textContent).toContain('solo puede hacerse con una cuenta registrada');
    expect(loginLink?.getAttribute('href')).toBe('/auth/login?returnUrl=%2Fexam%2FEXM-ABC');
    expect(el.querySelector('app-exam-access-card input')).toBeNull();
  });

  it('con sesión iniciada: rellena nombre y email con los de la cuenta', async () => {
    localStorage.setItem('fest_token', 't');
    localStorage.setItem('fest_user', JSON.stringify({
      id: 2, username: 'alu', email: 'alu@x.es', displayName: 'Alumna Registrada', role: 'STUDENT', active: true,
      createdAt: '2026-10-08T10:00:00'
    }));

    await render(exam({ accessMode: 'REGISTERED_ONLY' }));

    const inputs = el.querySelectorAll('app-exam-access-card input') as NodeListOf<HTMLInputElement>;
    expect(inputs[0].value).toBe('Alumna Registrada');
    expect(inputs[2].value).toBe('alu@x.es');
  });
});
