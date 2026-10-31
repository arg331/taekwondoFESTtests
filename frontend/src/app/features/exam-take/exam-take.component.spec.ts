import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ExamTakeComponent } from './exam-take.component';
import { ExamAttemptResponse, PublicExamResponse } from '../../core/models/exam.models';
import { ResultResponse } from '../../core/models/result.models';
import { ConfirmService } from '../../core/services/confirm.service';
import { of } from 'rxjs';

const API = 'http://localhost:8080/api';

function exam(overrides: Partial<PublicExamResponse> = {}): PublicExamResponse {
  return {
    title: 'Examen de árbitros', code: 'EXM-ABC', visibility: 'PUBLIC', accessMode: 'OPEN',
    config: { numberOfQuestions: 3, timeLimitMinutes: null }, maxAttempts: 1, expiresAt: null, ...overrides
  };
}

/** Preguntas tal como las manda el servidor: orden y opciones ya barajados. */
const ATTEMPT: ExamAttemptResponse = {
  attemptToken: 'intento-123',
  attemptNumber: 1,
  maxAttempts: 1,
  answers: {},
  secondsLeft: null,
  questions: [
    { id: 20, text: 'Segunda', options: [{ index: 2, text: 'C' }, { index: 0, text: 'A' }, { index: 1, text: 'B' }], multipleCorrect: false },
    { id: 10, text: 'Primera', options: [{ index: 1, text: 'Sí' }, { index: 0, text: 'No' }], multipleCorrect: false },
    { id: 30, text: 'Tercera', options: [{ index: 0, text: 'X' }, { index: 1, text: 'Y' }], multipleCorrect: false }
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

  async function render(examResponse: PublicExamResponse): Promise<void> {
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

  async function clickOption(optionText: string): Promise<void> {
    const option = Array.from(el.querySelectorAll('.option-item'))
      .find(o => o.querySelector('.option-text')?.textContent?.trim() === optionText) as HTMLElement;
    option.click();
    await stable();
  }

  /** Contesta: cada respuesta se guarda al momento en el servidor (RF-24), que la acepta. */
  async function choose(optionText: string): Promise<void> {
    await clickOption(optionText);
    http.match(`${API}/exams/by-code/EXM-ABC/attempts/answers`).forEach(req => req.flush(null));
    await stable();
  }

  async function goToQuestion(number: number): Promise<void> {
    (el.querySelectorAll('.nav-btn')[number - 1] as HTMLElement).click();
    await stable();
  }

  beforeEach(() => (localStorage.clear(), sessionStorage.clear()));
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
    const confirmSpy = vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
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
        { questionId: 20, chosenOptions: [0] },
        { questionId: 10, chosenOptions: [1] },
        { questionId: 30, chosenOptions: [] }
      ]
    });
    expect(confirmSpy.mock.calls[0][0].message).toBe('Tienes 1 pregunta(s) sin responder. ¿Enviar el examen igualmente?');
    req.flush(null);
  });

  describe('varias respuestas correctas (RF-09)', () => {
    const MULTIPLE: ExamAttemptResponse = {
      ...ATTEMPT,
      questions: [
        { id: 20, text: 'Varias', options: [{ index: 0, text: 'A' }, { index: 1, text: 'B' }, { index: 2, text: 'C' }], multipleCorrect: true },
        { id: 10, text: 'Una', options: [{ index: 0, text: 'No' }, { index: 1, text: 'Sí' }], multipleCorrect: false },
        { id: 30, text: 'Otra', options: [{ index: 0, text: 'X' }, { index: 1, text: 'Y' }], multipleCorrect: false }
      ]
    };

    it('la pregunta con varias correctas avisa y deja marcar varias; volver a pulsar desmarca', async () => {
      vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
      await render(exam());
      await startAs('Ana', MULTIPLE);

      expect(el.querySelector('.multiple-hint')?.textContent).toContain('Marca todas las correctas');
      await choose('A');
      await choose('C');
      await choose('B');
      await choose('B');
      await goToQuestion(3);
      button('Enviar examen').click();
      await stable();

      const req = http.expectOne(`${API}/results`);
      expect(req.request.body.answers[0]).toEqual({ questionId: 20, chosenOptions: [0, 2] });
      req.flush(null);
    });

    it('en una pregunta de una sola correcta, elegir otra opción sustituye a la anterior y no hay aviso', async () => {
      vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
      await render(exam());
      await startAs('Ana', MULTIPLE);
      await goToQuestion(2);

      expect(el.querySelector('.multiple-hint')).toBeNull();
      await choose('No');
      await choose('Sí');
      await goToQuestion(3);
      button('Enviar examen').click();
      await stable();

      const req = http.expectOne(`${API}/results`);
      expect(req.request.body.answers[1]).toEqual({ questionId: 10, chosenOptions: [1] });
      req.flush(null);
    });
  });

  describe('con teclado y lector de pantalla (RNF-12)', () => {
    function inputs(): HTMLInputElement[] {
      return Array.from(el.querySelectorAll('.options-list input'));
    }

    it('una sola correcta: un grupo de radios que se llama como el enunciado', async () => {
      await render(exam());
      await startAs('Ana');

      const group = el.querySelector('.options-list') as HTMLElement;
      expect(group.getAttribute('role')).toBe('radiogroup');
      expect(el.querySelector('#' + group.getAttribute('aria-labelledby'))?.textContent).toContain('Segunda');
      expect(inputs().map(i => i.type)).toEqual(['radio', 'radio', 'radio']);
      expect(new Set(inputs().map(i => i.name)).size).toBe(1);
      // Cada control está dentro de su etiqueta: el lector lee el texto de la opción
      expect(inputs()[0].closest('label')?.querySelector('.option-text')?.textContent?.trim()).toBe('C');
    });

    it('varias correctas: casillas, con el aviso «Marca todas las correctas» como descripción', async () => {
      await render(exam());
      await startAs('Ana', { ...ATTEMPT, questions: [{ ...ATTEMPT.questions[0], multipleCorrect: true }, ...ATTEMPT.questions.slice(1)] });

      const group = el.querySelector('.options-list') as HTMLElement;
      expect(group.getAttribute('role')).toBe('group');
      expect(el.querySelector('#' + group.getAttribute('aria-describedby'))?.textContent).toContain('Marca todas las correctas');
      expect(inputs().every(i => i.type === 'checkbox')).toBe(true);
    });

    it('se contesta con el teclado: marcar el control (la barra espaciadora hace clic) guarda la respuesta', async () => {
      await render(exam());
      await startAs('Ana');

      const a = inputs()[1];
      a.focus();
      a.click();
      await stable();

      expect(http.expectOne(`${API}/exams/by-code/EXM-ABC/attempts/answers`).request.body.chosenOptions).toEqual([0]);
      expect(inputs()[1].checked).toBe(true);
      expect(inputs()[1].closest('label')?.classList.contains('option-selected')).toBe(true);
    });

    it('los botones de las preguntas dicen cuál es, si está respondida y cuál se está viendo', async () => {
      await render(exam());
      await startAs('Ana');
      await choose('A');

      const nav = Array.from(el.querySelectorAll('.nav-btn'));
      expect(nav.map(b => b.getAttribute('aria-label')))
        .toEqual(['Pregunta 1, respondida', 'Pregunta 2, sin responder', 'Pregunta 3, sin responder']);
      expect(nav.map(b => b.getAttribute('aria-current'))).toEqual(['step', null, null]);
    });

    it('con más de cuatro opciones, las letras siguen: E, F…', async () => {
      await render(exam());
      const six = ['uno', 'dos', 'tres', 'cuatro', 'cinco', 'seis'].map((text, index) => ({ index, text }));
      await startAs('Ana', { ...ATTEMPT, questions: [{ id: 20, text: 'Seis', options: six, multipleCorrect: false }, ...ATTEMPT.questions.slice(1)] });

      expect(Array.from(el.querySelectorAll('.option-letter')).map(l => l.textContent?.trim())).toEqual(['A', 'B', 'C', 'D', 'E', 'F']);
    });
  });

  describe('no perder el examen (RF-24)', () => {
    const KEY = 'fest_attempt_EXM-ABC';
    const SAVES = `${API}/exams/by-code/EXM-ABC/attempts/answers`;
    const RESUME = `${API}/exams/by-code/EXM-ABC/attempts/resume`;
    const ANA = { studentName: 'Ana', studentClub: null, studentEmail: null };

    it('cada respuesta se guarda en el servidor al contestarla', async () => {
      await render(exam());
      await startAs('Ana');
      await clickOption('A');

      const req = http.expectOne(SAVES);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ attemptToken: 'intento-123', questionId: 20, chosenOptions: [0] });
      req.flush(null);
    });

    it('se guardan de una en una y en orden: la última respuesta es la que queda', async () => {
      await render(exam());
      await startAs('Ana');
      await clickOption('A');
      await clickOption('B');

      const first = http.expectOne(SAVES);
      expect(first.request.body.chosenOptions).toEqual([0]);
      first.flush(null);
      await stable();
      const second = http.expectOne(SAVES);
      expect(second.request.body.chosenOptions).toEqual([1]);
      second.flush(null);
    });

    it('si no se puede guardar, avisa sin cortar el examen; al guardar la siguiente, el aviso se va', async () => {
      await render(exam());
      await startAs('Ana');
      await clickOption('A');
      http.expectOne(SAVES).error(new ProgressEvent('error'));
      await stable();

      expect(el.querySelector('.save-warning')?.textContent).toContain('No se ha podido guardar');
      await choose('B');
      expect(el.querySelector('.save-warning')).toBeNull();
    });

    it('al recargar recupera el intento: mismas preguntas, sus respuestas y el reloj donde iba', async () => {
      localStorage.setItem(KEY, JSON.stringify({ attemptToken: 'intento-123', student: ANA }));
      await render(exam({ config: { ...exam().config, timeLimitMinutes: 10 } }));

      const req = http.expectOne(RESUME);
      expect(req.request.body).toEqual({ attemptToken: 'intento-123' });
      req.flush({ ...ATTEMPT, answers: { 20: [2], 10: [], 30: [] }, secondsLeft: 125 });
      await stable();

      expect(el.querySelector('.question-text')?.textContent).toContain('Segunda');
      expect(el.querySelector('.option-item.option-selected')?.textContent).toContain('C');
      expect(el.querySelector('.timer')?.textContent).toContain('02:05');
      expect(el.querySelector('.exam-header')?.textContent).toContain('1/3');
    });

    it('si el intento ya no vale (entregado o caducado), lo olvida y muestra la portada', async () => {
      localStorage.setItem(KEY, JSON.stringify({ attemptToken: 'intento-123', student: ANA }));
      await render(exam());

      http.expectOne(RESUME).flush({ message: 'Este intento ya se ha entregado' }, { status: 409, statusText: 'Conflict' });
      await stable();
      http.match(`${API}/clubs`).forEach(req => req.flush([]));
      await stable();

      expect(el.querySelector('app-exam-access-card')).not.toBeNull();
      expect(localStorage.getItem(KEY)).toBeNull();
    });

    it('recuerda el intento en curso y lo olvida al entregarlo', async () => {
      vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
      await render(exam());
      await startAs('Ana');

      expect(JSON.parse(localStorage.getItem(KEY)!)).toEqual({ attemptToken: 'intento-123', student: ANA });
      await goToQuestion(3);
      button('Enviar examen').click();
      await stable();
      http.expectOne(`${API}/results`).flush({
        id: 5, examId: 1, examTitle: 'Examen de árbitros', studentName: 'Ana', studentClub: null, studentEmail: null,
        registered: false, studentUserId: null, studentKey: 'n:ana', scoreVisible: false, answers: null, correctAnswers: null, totalQuestions: 3,
        score: null, passed: null, timeSpentSeconds: 40, completedAt: '2026-10-08T10:10:00', autoSubmitted: false
      } satisfies ResultResponse);
      await stable();

      expect(localStorage.getItem(KEY)).toBeNull();
    });
  });

  describe('intentos (RF-52)', () => {
    /** Contesta, entrega y recibe una nota: queda en la pantalla del resultado. */
    async function finish(): Promise<void> {
      vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
      await goToQuestion(3);
      button('Enviar examen').click();
      await stable();
      http.expectOne(`${API}/results`).flush({
        id: 5, examId: 1, examTitle: 'Examen de árbitros', studentName: 'Ana', studentClub: null, studentEmail: null,
        registered: false, studentUserId: null, studentKey: 'n:ana', scoreVisible: true, answers: [], correctAnswers: 0, totalQuestions: 3, score: 0, passed: false,
        timeSpentSeconds: 40, completedAt: '2026-10-08T10:10:00', autoSubmitted: false
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

  it('si el servidor no deja empezar con ese nombre, avisa en la portada y deja escribir otro (DE-21)', async () => {
    const inUse = 'Ya hay un intento en curso con ese nombre. Si es tuyo, sigue en el navegador donde lo empezaste; '
      + 'si no, escribe tu nombre completo.';
    await render(exam());
    const input = el.querySelector('app-exam-access-card input') as HTMLInputElement;
    input.value = 'Ana';
    input.dispatchEvent(new Event('input'));
    await stable();
    button('Comenzar examen').click();
    await stable();

    http.expectOne(`${API}/exams/by-code/EXM-ABC/attempts`).flush(
      { message: inUse }, { status: 409, statusText: 'Conflict' });
    await stable();

    expect(el.querySelector('app-exam-access-card [role="alert"]')?.textContent?.trim()).toBe(inUse);
    expect(button('Comenzar examen').disabled).toBe(false);

    await startAs('Ana García');

    expect(el.querySelector('app-exam-access-card')).toBeNull();
    expect(el.textContent).toContain('Segunda');
  });

  it('si el alumno cancela la confirmación no se envía nada', async () => {
    vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(false));
    await render(exam());
    await startAs('Ana');
    await goToQuestion(3);

    button('Enviar examen').click();
    await stable();

    http.expectNone(`${API}/results`);
  });

  it('resultado con nota y respuestas: puntuación y sus respuestas bien o mal, sin decir la correcta (RF-53)', async () => {
    vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
    await render(exam());
    await startAs('Ana');
    await goToQuestion(3);
    button('Enviar examen').click();
    await stable();

    const result: ResultResponse = {
      id: 5, examId: 1, examTitle: 'Examen de árbitros', studentName: 'Ana', studentClub: null, studentEmail: null,
      registered: false, studentUserId: null, studentKey: 'n:ana', scoreVisible: true, correctAnswers: 1, totalQuestions: 3, score: 33, passed: false, timeSpentSeconds: 40,
      completedAt: '2026-10-08T10:10:00', autoSubmitted: false,
      answers: [
        { questionId: 10, studentAnswers: [1], correctAnswers: null, correct: true },
        { questionId: 20, studentAnswers: [], correctAnswers: null, correct: false },
        { questionId: 30, studentAnswers: [0], correctAnswers: null, correct: false }
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
    vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
    await render(exam());
    await startAs('Ana');
    await goToQuestion(3);
    button('Enviar examen').click();
    await stable();

    http.expectOne(`${API}/results`).flush({
      id: 5, examId: 1, examTitle: 'X', studentName: 'Ana', studentClub: null, studentEmail: null,
      registered: false, studentUserId: null, studentKey: 'n:ana', scoreVisible: true, answers: null, correctAnswers: 2, totalQuestions: 3, score: 67, passed: false,
      timeSpentSeconds: 40, completedAt: '2026-10-08T10:10:00', autoSubmitted: false
    } satisfies ResultResponse);
    await stable();

    expect(el.querySelector('.score-value')?.textContent).toContain('67');
    expect(el.textContent).not.toContain('Detalle de respuestas');
  });

  it('resultado con nota oculta: solo confirma la entrega', async () => {
    vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
    await render(exam());
    await startAs('Ana');
    await goToQuestion(3);
    button('Enviar examen').click();
    await stable();

    http.expectOne(`${API}/results`).flush({
      id: 5, examId: 1, examTitle: 'X', studentName: 'Ana', studentClub: null, studentEmail: null,
      registered: false, studentUserId: null, studentKey: 'n:ana', scoreVisible: false, answers: null, correctAnswers: null, totalQuestions: 3, score: null, passed: null,
      timeSpentSeconds: 40, completedAt: '2026-10-08T10:10:00', autoSubmitted: false
    } satisfies ResultResponse);
    await stable();

    expect(el.textContent).toContain('Examen completado');
    expect(el.querySelector('.score-value')).toBeNull();
  });

  it('con tiempo límite: muestra el reloj y al llegar a cero envía sin pedir confirmación', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: false });
    const confirm = vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
    await render(exam({ config: { ...exam().config, timeLimitMinutes: 1 } }));
    await startAs('Ana', { ...ATTEMPT, secondsLeft: 60 });

    expect(el.querySelector('.timer')?.textContent).toContain('01:00');
    vi.advanceTimersByTime(30_000);
    await stable();
    expect(el.querySelector('.timer')?.textContent).toContain('00:30');
    vi.advanceTimersByTime(30_000);
    await stable();

    const req = http.expectOne(`${API}/results`);
    expect(req.request.body.answers.every((a: { chosenOptions: number[] }) => a.chosenOptions.length === 0)).toBe(true);
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
    sessionStorage.setItem('fest_token', 't');
    sessionStorage.setItem('fest_user', JSON.stringify({
      id: 2, username: 'alu', email: 'alu@x.es', displayName: 'Alumna Registrada', role: 'STUDENT', club: null, active: true, mustChangePassword: false,
      createdAt: '2026-10-08T10:00:00'
    }));

    await render(exam({ accessMode: 'REGISTERED_ONLY' }));

    const inputs = el.querySelectorAll('app-exam-access-card input') as NodeListOf<HTMLInputElement>;
    expect(inputs[0].value).toBe('Alumna Registrada');
    expect(inputs[1].value).toBe('alu@x.es');
  });
});
