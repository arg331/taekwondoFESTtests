import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { QuestionFormComponent } from './question-form.component';
import { QuestionResponse } from '../../../../core/models/question.models';

const API = 'http://localhost:8080/api';

function existing(overrides: Partial<QuestionResponse>): QuestionResponse {
  return {
    id: 42, text: 'Pregunta existente', options: ['Sí', 'No'], correctAnswer: 1, explanation: 'Porque sí',
    grade: 'NEGRO', type: 'D', tags: [], createdAt: '2026-10-08T10:00:00', updatedAt: '2026-10-08T10:00:00',
    ...overrides
  };
}

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

  function optionInputs(): HTMLInputElement[] {
    return Array.from(el.querySelectorAll('.option-row input:not([type=radio])'));
  }

  /** Escribe las opciones una a una, como el profesor: cada vez que rellena la última aparece otra. */
  async function fillOptions(options: string[]): Promise<void> {
    for (let i = 0; i < options.length; i++) {
      type(optionInputs()[i], options[i]);
      await stable();
    }
  }

  async function markCorrect(index: number): Promise<void> {
    (el.querySelectorAll('.option-row input[type=radio]')[index] as HTMLInputElement).click();
    await stable();
  }

  async function fill(text: string, options: string[], correctIndex: number, explanation: string): Promise<void> {
    const textareas = el.querySelectorAll('textarea');
    type(textareas[0], text);
    type(textareas[1], explanation);
    await fillOptions(options);
    await markCorrect(correctIndex);
  }

  function saveButton(): HTMLButtonElement {
    return Array.from(el.querySelectorAll('mat-card-actions button'))
      .find(b => /Crear pregunta|Guardar cambios/.test(b.textContent ?? '')) as HTMLButtonElement;
  }

  function typeToggle(label: string): HTMLButtonElement {
    return Array.from(el.querySelectorAll('.type-toggle button')).find(b => b.textContent?.trim() === label) as HTMLButtonElement;
  }

  afterEach(() => http.verify());

  describe('opciones sin máximo (RF-09)', () => {
    it('una pregunta nueva empieza con 2 casillas', async () => {
      await render(null);

      expect(optionInputs().length).toBe(2);
    });

    it('al escribir en la última casilla aparece otra vacía', async () => {
      await render(null);

      await fillOptions(['A', 'B']);
      expect(optionInputs().length).toBe(3);

      await fillOptions(['A', 'B', 'C', 'D', 'E']);
      expect(optionInputs().length).toBe(6);
      expect(optionInputs()[5].value).toBe('');
    });

    it('se puede quitar una opción y la correcta sigue siendo la misma', async () => {
      await render(existing({ options: ['Uno', 'Dos', 'Tres'], correctAnswer: 2 }));

      (el.querySelectorAll('.option-row .remove-option')[0] as HTMLButtonElement).click();
      await stable();
      saveButton().click();

      const req = http.expectOne(`${API}/questions/42`);
      expect(req.request.body.options).toEqual(['Dos', 'Tres']);
      expect(req.request.body.correctAnswer).toBe(1);
      req.flush(null);
    });

    it('con solo dos opciones no se puede quitar ninguna', async () => {
      await render(existing({}));

      expect(el.querySelectorAll('.option-row .remove-option').length).toBe(0);
    });
  });

  it('el tipo es el de examen de arbitraje: Tipo C, Tipo D o «Sin tipo definido», marcado por defecto', async () => {
    await render(null);

    expect(el.querySelector('.type-row .section-label')?.textContent).toBe('Tipo de examen de arbitraje');
    expect(Array.from(el.querySelectorAll('.type-toggle button')).map(b => b.textContent?.trim()))
      .toEqual(['Tipo C', 'Tipo D', 'Sin tipo definido']);
    expect(typeToggle('Sin tipo definido').getAttribute('aria-checked')).toBe('true');
  });

  it('nueva: envía grado y tipo, descarta las opciones vacías y reindexa la correcta', async () => {
    await render(null);
    await fill('¿Cuántos puntos vale un giro a la cabeza?', ['1 punto', '2 puntos', '3 puntos'], 2, 'Son 3');
    type(optionInputs()[1], '');
    const loader = TestbedHarnessEnvironment.loader(fixture);
    await (await loader.getHarness(MatSelectHarness.with({ selector: '.grade-select' }))).clickOptions({ text: 'Azul' });
    typeToggle('Tipo C').click();
    await stable();

    saveButton().click();
    await stable();

    const req = http.expectOne(`${API}/questions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      text: '¿Cuántos puntos vale un giro a la cabeza?',
      options: ['1 punto', '3 puntos'],
      correctAnswer: 1,
      explanation: 'Son 3',
      grade: 'AZUL',
      type: 'C',
      tagIds: []
    });
    req.flush(null);
  });

  it('la explicación y el tipo son opcionales (RF-09, RF-44)', async () => {
    await render(null);
    await fill('Pregunta sin explicación alguna', ['Sí', 'No'], 0, '');

    expect(saveButton().disabled).toBe(false);
    saveButton().click();
    await stable();

    const req = http.expectOne(`${API}/questions`);
    expect(req.request.body.explanation).toBe('');
    expect(req.request.body.type).toBeNull();
    expect(req.request.body.grade).toBe('BLANCO');
    req.flush(null);
  });

  it('avisa y no deja guardar con menos de 2 opciones o con la correcta vacía', async () => {
    await render(null);
    await fill('Texto suficientemente largo', ['Solo una'], 0, 'x');

    expect(el.textContent).toContain('Necesitas al menos 2 opciones con texto.');
    expect(saveButton().disabled).toBe(true);

    await fillOptions(['Solo una', 'Otra']);
    await markCorrect(2);

    expect(el.textContent).toContain('La opción marcada como correcta está vacía.');
    expect(saveButton().disabled).toBe(true);
  });

  it('edición: carga la pregunta con una casilla libre al final y guarda con PUT', async () => {
    await render(existing({}));

    expect((el.querySelectorAll('textarea')[0] as HTMLTextAreaElement).value).toBe('Pregunta existente');
    expect(optionInputs().map(i => i.value)).toEqual(['Sí', 'No', '']);
    expect(typeToggle('Tipo D').getAttribute('aria-checked')).toBe('true');
    saveButton().click();
    await stable();

    const req = http.expectOne(`${API}/questions/42`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      text: 'Pregunta existente', options: ['Sí', 'No'], correctAnswer: 1, explanation: 'Porque sí',
      grade: 'NEGRO', type: 'D', tagIds: []
    });
    req.flush(null);
  });
});
