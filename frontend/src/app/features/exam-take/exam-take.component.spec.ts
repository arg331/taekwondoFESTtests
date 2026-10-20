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
    config: { numberOfQuestions: 3, timeLimitMinutes: null, showScore: true, showAnswers: true, randomizeOptions: true,
      randomizeQuestionOrder: true },
    questionIds: [10, 20, 30], generationTags: [], code: 'EXM-ABC', createdAt: '2026-10-08T10:00:00',
    expiresAt: null, maxAttempts: 1, ...overrides
  };
}

/** Preguntas tal como las manda el servidor: orden y opciones ya barajados. */
const ATTEMPT: ExamAttemptResponse = {
  attemptToken: 'intento-123',
  attemptNumber: 1,
  maxAttempts: 1,
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
    fixture.detectChanges();
    // La portada pide los clubes para el desplegable
    http.match(`${API}/clubs`).forEach(req => req.flush([]));
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

  async function startAs(name: string, attempt: ExamAttemptResponse = ATTEMPT): Promise<void> {
    const input = el.querySelector('app-exam-access-card input') as HTMLInputElement;
    input.value = name;
    input.dispatchEvent(new Event('input'));
    await stable();
    button('Comenzar examen').click();
    await stable();
    const req = http.expectOne(`${API}/exams/by-code/EXM-ABC/attempts`);
    expect(req.request.method).toBe('POST');
    // Se identifica al empezar (DE-04)
    expect(req.request.body).toEqual({ studentName: name.trim(), studentClub: null, studentEmail: null });
    req.flush(attempt);
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

  it('un código escrito en minúsculas en la URL se busca en mayúsculas (DE-17)', async () => {
    TestBed.configureTestingModule({
      imports: [ExamTakeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExamTakeComponent);
    fixture.componentRef.setInput('code', ' exm-abc ');
    el = fixture.nativeElement;
    await stable();

    http.expectOne(`${API}/exams/by-code/EXM-ABC`).flush(exam());
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
      answers: [
        { questionId: 20, chosenOption: 0 },
        { questionId: 10, chosenOption: 1 },
        { questionId: 30, chosenOption: null }
      ]
    });
    expect(window.confirm).toHaveBeenCalledWith('Tienes 1 pregunta(s) sin responder. ¿Enviar el examen igualmente?');
    req.flush(null);
  });

  describe('intentos (RF-52)', () => {
    /** Contesta, entrega y recibe una nota: queda en la pantalla del resultado. */
    async function finish(): Promise<void> {
      vi.spyOn(window, 'confirm').mockReturnValue(true);
      await goToQuestion(3);
      button('Enviar examen').click();
      await stable();
      http.expectOne(`${API}/results`).flush({
        id: 5, examId: 1, examTitle: 'Examen de árbitros', studentName: 'Ana', studentClub: null, studentEmail: null,
        registered: false, scoreVisible: true, answers: [], correctAnswers: 0, totalQuestions: 3, score: 0, passed: false,
        timeSpentSeconds: 40, completedAt: '2026-10-08T10:10:00'
      } satisfies ResultResponse);
      await stable();
    }

    function repeatButton(): HTMLButtonElement | undefined {
      return Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes('Repetir examen'));
    }

    it('si le quedan intentos, al terminar ve «Repetir examen» con los que le quedan y empieza otro con sus datos', async () => {
      await render(exam({ maxAttempts: 3 }));
      await startAs('Ana', { ...ATTEMPT, attemptNumber: 1, maxAttempts: 3 });
      await finish();

      expect(repeatButton()?.textContent).toContain('Te quedan 2 intentos');
      repeatButton()!.click();
      await stable();

      const again = http.expectOne(`${API}/exams/by-code/EXM-ABC/attempts`);
      expect(again.request.body).toEqual({ studentName: 'Ana', studentClub: null, studentEmail: null });
      again.flush({ ...ATTEMPT, attemptNumber: 2, maxAttempts: 3 });
      await stable();
      expect(el.querySelector('.exam-header')?.textContent).toContain('Intento 2 de 3');
    });

    it('con el último intento ya hecho, no aparece', async () => {
      await render(exam({ maxAttempts: 2 }));
      await startAs('Ana', { ...ATTEMPT, attemptNumber: 2, maxAttempts: 2 });
      await finish();

      expect(repeatButton()).toBeUndefined();
    });

    it('sin límite: «Intento N» sin total y «Repetir examen» sin contar', async () => {
      await render(exam({ maxAttempts: null }));
      await startAs('Ana', { ...ATTEMPT, attemptNumber: 4, maxAttempts: null });
      expect(el.querySelector('.exam-header')?.textContent).toContain('Intento 4');
      expect(el.querySelector('.exam-header')?.textContent).not.toContain('Intento 4 de');
      await finish();

      expect(repeatButton()).toBeDefined();
      expect(repeatButton()?.textContent).not.toContain('Te quedan');
    });

    it('con varios intentos, dice cuál es', async () => {
      await render(exam({ maxAttempts: 3 }));
      await startAs('Ana', { ...ATTEMPT, attemptNumber: 2, maxAttempts: 3 });

      expect(el.querySelector('.exam-header')?.textContent).toContain('Intento 2 de 3');
    });

    it('con uno solo, no lo menciona', async () => {
      await render(exam());
      await startAs('Ana');

      expect(el.querySelector('.exam-header')?.textContent).not.toContain('Intento');
    });

    it('si ya no le quedan intentos, muestra el aviso del servidor', async () => {
      await render(exam());
      const input = el.querySelector('app-exam-access-card input') as HTMLInputElement;
      input.value = 'Ana';
      input.dispatchEvent(new Event('input'));
      await stable();
      button('Comenzar examen').click();
      await stable();

      http.expectOne(`${API}/exams/by-code/EXM-ABC/attempts`).flush(
        { message: 'Ya has hecho todos los intentos de este examen' }, { status: 409, statusText: 'Conflict' });
      await stable();

      expect(el.textContent).toContain('Ya has hecho todos los intentos de este examen');
    });
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

  it('resultado con nota y respuestas: puntuación y sus respuestas bien o mal, sin decir la correcta (RF-53)', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await render(exam());
    await startAs('Ana');
    await goToQuestion(3);
    button('Enviar examen').click();
    await stable();

    const result: ResultResponse = {
      id: 5, examId: 1, examTitle: 'Examen de árbitros', studentName: 'Ana', studentClub: null, studentEmail: null,
      registered: false, scoreVisible: true, correctAnswers: 1, totalQuestions: 3, score: 33, passed: false, timeSpentSeconds: 40,
      completedAt: '2026-10-08T10:10:00',
      answers: [
        { questionId: 10, studentAnswer: 1, correctAnswer: null, correct: true },
        { questionId: 20, studentAnswer: null, correctAnswer: null, correct: false },
        { questionId: 30, studentAnswer: 0, correctAnswer: null, correct: false }
      ]
    };
    http.expectOne(`${API}/results`).flush(result);
    await stable();

    const rows = Array.from(el.querySelectorAll('.answer-row')).map(r => r.textContent?.replace(/\s+/g, ' ').trim());
    expect(el.querySelector('.score-value')?.textContent).toContain('33');
    expect(el.textContent).toContain('SUSPENSO');
    expect(rows[0]).toContain('1. Segunda');
    expect(rows[0]).toContain('Tu respuesta: Sin responder');
    expect(rows[1]).toContain('2. Primera');
    expect(rows[1]).toContain('Tu respuesta: Sí');
    expect(rows[2]).toContain('Tu respuesta: X');
    expect(el.textContent).not.toContain('Correcta:');
    const classes = Array.from(el.querySelectorAll('.answer-row')).map(r => r.classList.contains('answer-correct'));
    expect(classes).toEqual([false, true, false]);
  });

  it('resultado con solo la nota: la puntuación, sin el detalle de respuestas', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await render(exam({ config: { ...exam().config, showAnswers: false } }));
    await startAs('Ana');
    await goToQuestion(3);
    button('Enviar examen').click();
    await stable();

    http.expectOne(`${API}/results`).flush({
      id: 5, examId: 1, examTitle: 'X', studentName: 'Ana', studentClub: null, studentEmail: null,
      registered: false, scoreVisible: true, answers: null, correctAnswers: 2, totalQuestions: 3, score: 67, passed: false,
      timeSpentSeconds: 40, completedAt: '2026-10-08T10:10:00'
    } satisfies ResultResponse);
    await stable();

    expect(el.querySelector('.score-value')?.textContent).toContain('67');
    expect(el.textContent).not.toContain('Detalle de respuestas');
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
      registered: false, scoreVisible: false, answers: null, correctAnswers: null, totalQuestions: 3, score: null, passed: null,
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
      id: 2, username: 'alu', email: 'alu@x.es', displayName: 'Alumna Registrada', role: 'STUDENT', club: null, active: true, mustChangePassword: false,
      createdAt: '2026-10-08T10:00:00'
    }));

    await render(exam({ accessMode: 'REGISTERED_ONLY' }));

    const inputs = el.querySelectorAll('app-exam-access-card input') as NodeListOf<HTMLInputElement>;
    expect(inputs[0].value).toBe('Alumna Registrada');
    expect(inputs[1].value).toBe('alu@x.es');
  });
});
