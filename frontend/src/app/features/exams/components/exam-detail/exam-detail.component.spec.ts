import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExamDetailComponent } from './exam-detail.component';
import { ExamResponse, ExamStatus } from '../../../../core/models/exam.models';
import { QuestionResponse } from '../../../../core/models/question.models';

const API = 'http://localhost:8080/api';

function exam(status: ExamStatus, questionIds: number[] = []): ExamResponse {
  return {
    id: 7, title: 'Examen', ownerId: 1, status, visibility: 'PUBLIC', accessMode: 'OPEN',
    config: { numberOfQuestions: 2, timeLimitMinutes: null, showScore: true, randomizeOptions: false,
      randomizeQuestionOrder: false },
    questionIds, generationTags: [], code: status === 'DRAFT' ? null : 'EXM-1234ABCD',
    createdAt: '2026-10-08T10:00:00', expiresAt: null
  };
}

function question(id: number): QuestionResponse {
  return {
    id, text: `Pregunta ${id}`, options: ['A', 'B'], correctAnswer: 0, explanation: '', difficulty: 'FACIL',
    tags: [], createdAt: '2026-10-08T10:00:00', updatedAt: '2026-10-08T10:00:00'
  };
}

/** Caracterización del panel de un examen: acciones disponibles por estado y llamadas al publicar. */
describe('ExamDetailComponent', () => {
  let fixture: ComponentFixture<ExamDetailComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;
  let changed: ExamResponse[];

  async function render(examResponse: ExamResponse): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ExamDetailComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExamDetailComponent);
    fixture.componentRef.setInput('exam', examResponse);
    fixture.componentRef.setInput('questions', [question(1), question(2), question(3)]);
    changed = [];
    fixture.componentInstance.changed.subscribe(e => changed.push(e));
    el = fixture.nativeElement;
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function buttonTexts(): string[] {
    return Array.from(el.querySelectorAll('.actions-section button'))
      .map(b => b.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  }

  function button(text: string): HTMLButtonElement {
    return Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes(text)) as HTMLButtonElement;
  }

  afterEach(() => http.verify());

  it('acciones según el estado del examen', async () => {
    await render(exam('DRAFT'));
    expect(buttonTexts().join(' | ')).toContain('Publicar examen');
    expect(buttonTexts().join(' | ')).toContain('Eliminar borrador');

    fixture.componentRef.setInput('exam', exam('PUBLISHED', [1, 2]));
    await stable();
    expect(buttonTexts().join(' | ')).toContain('Cambiar plazo');
    expect(buttonTexts().join(' | ')).toContain('Hacer privado');
    expect(buttonTexts().join(' | ')).toContain('Cerrar examen');

    fixture.componentRef.setInput('exam', exam('EXPIRED', [1, 2]));
    await stable();
    expect(buttonTexts().join(' | ')).toContain('Reabrir');
    expect(buttonTexts().join(' | ')).not.toContain('Cerrar examen');
  });

  it('borrador: sin el número de preguntas exacto no se puede publicar', async () => {
    await render(exam('DRAFT'));

    expect(el.textContent).toContain('Tienes 0 preguntas seleccionadas y el examen requiere 2.');
    expect(button('Publicar examen').disabled).toBe(true);
  });

  it('publicar con selección sin guardar: primero guarda las preguntas y luego publica', async () => {
    await render(exam('DRAFT'));
    const rows = el.querySelectorAll('.question-pick-row');
    (rows[0] as HTMLElement).click();
    (rows[2] as HTMLElement).click();
    await stable();

    button('Publicar examen').click();
    await stable();

    const save = http.expectOne(`${API}/exams/7/questions`);
    expect(save.request.method).toBe('PUT');
    expect(save.request.body).toEqual({ questionIds: [1, 3] });
    save.flush(exam('DRAFT', [1, 3]));
    const publish = http.expectOne(`${API}/exams/7/publish`);
    expect(publish.request.body).toEqual({ visibility: 'PUBLIC', accessMode: 'OPEN', expiresAt: null });
    publish.flush(exam('PUBLISHED', [1, 3]));
    await stable();

    expect(changed.at(-1)?.status).toBe('PUBLISHED');
  });

  it('publicado: muestra el código y la lista de preguntas sin selector', async () => {
    await render(exam('PUBLISHED', [2, 1]));

    expect(el.textContent).toContain('EXM-1234ABCD');
    expect(el.querySelector('app-exam-question-picker')).toBeNull();
    expect(Array.from(el.querySelectorAll('.assigned-list li')).map(li => li.textContent?.trim()))
      .toEqual(['Pregunta 2', 'Pregunta 1']);
  });

  it('el QR codifica el enlace completo al examen', async () => {
    await render(exam('PUBLISHED', [1, 2]));
    button('qr_code').click();
    await stable();

    const src = (el.querySelector('.qr-image') as HTMLImageElement).src;
    expect(decodeURIComponent(src)).toContain(`data=${window.location.origin}/exam/EXM-1234ABCD`);
  });

  it('un error del servidor se muestra en el panel', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await render(exam('PUBLISHED', [1, 2]));

    button('Cerrar examen').click();
    http.expectOne(`${API}/exams/7/close`)
      .flush({ message: 'Solo se pueden cerrar exámenes publicados' }, { status: 409, statusText: 'Conflict' });
    await stable();

    expect(el.textContent).toContain('Solo se pueden cerrar exámenes publicados');
    vi.restoreAllMocks();
  });
});
