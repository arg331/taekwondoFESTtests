import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { QuestionFormComponent } from './question-form.component';
import { QuestionResponse } from '../../../../core/models/question.models';

const API = 'http://localhost:8080/api';

/** Caracterización del formulario de preguntas: qué se envía al guardar. */
describe('QuestionFormComponent', () => {
  let fixture: ComponentFixture<QuestionFormComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function render(question: QuestionResponse | null): Promise<void> {
    TestBed.configureTestingModule({
      imports: [QuestionFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(QuestionFormComponent);
    fixture.componentRef.setInput('question', question);
    fixture.componentRef.setInput('tags', []);
    el = fixture.nativeElement;
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function type(input: HTMLInputElement | HTMLTextAreaElement, value: string): void {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function fill(text: string, options: string[], correctIndex: number, explanation: string): Promise<void> {
    const textareas = el.querySelectorAll('textarea');
    type(textareas[0], text);
    type(textareas[1], explanation);
    const optionInputs = el.querySelectorAll('.option-row input:not([type=radio])') as NodeListOf<HTMLInputElement>;
    options.forEach((value, i) => type(optionInputs[i], value));
    (el.querySelectorAll('.option-row input[type=radio]')[correctIndex] as HTMLInputElement).click();
    await stable();
  }

  function saveButton(): HTMLButtonElement {
    return Array.from(el.querySelectorAll('mat-card-actions button'))
      .find(b => /Crear pregunta|Guardar cambios/.test(b.textContent ?? '')) as HTMLButtonElement;
  }

  afterEach(() => http.verify());

  it('nueva: descarta las opciones vacías y reindexa la respuesta correcta', async () => {
    await render(null);
    await fill('¿Cuántos puntos vale un giro a la cabeza?', ['1 punto', '', '2 puntos', '3 puntos'], 3, 'Son 3');

    saveButton().click();
    await stable();

    const req = http.expectOne(`${API}/questions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      text: '¿Cuántos puntos vale un giro a la cabeza?',
      options: ['1 punto', '2 puntos', '3 puntos'],
      correctAnswer: 2,
      explanation: 'Son 3',
      difficulty: 'FACIL',
      tagIds: []
    });
    req.flush(null);
  });

  it('avisa y no deja guardar con menos de 2 opciones o con la correcta vacía', async () => {
    await render(null);
    await fill('Texto suficientemente largo', ['Solo una', '', '', ''], 0, 'x');

    expect(el.textContent).toContain('Necesitas al menos 2 opciones con texto.');
    expect(saveButton().disabled).toBe(true);

    await fill('Texto suficientemente largo', ['A', 'B', '', ''], 2, 'x');

    expect(el.textContent).toContain('La opción marcada como correcta está vacía.');
    expect(saveButton().disabled).toBe(true);
  });

  it('edición: carga la pregunta y guarda con PUT', async () => {
    await render({
      id: 42, text: 'Pregunta existente', options: ['Sí', 'No'], correctAnswer: 1, explanation: 'Porque sí',
      difficulty: 'DIFICIL', tags: [], createdAt: '2026-10-08T10:00:00', updatedAt: '2026-10-08T10:00:00'
    });

    expect((el.querySelectorAll('textarea')[0] as HTMLTextAreaElement).value).toBe('Pregunta existente');
    saveButton().click();
    await stable();

    const req = http.expectOne(`${API}/questions/42`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      text: 'Pregunta existente', options: ['Sí', 'No'], correctAnswer: 1, explanation: 'Porque sí',
      difficulty: 'DIFICIL', tagIds: []
    });
    req.flush(null);
  });
});
