import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { QuestionsComponent } from './questions.component';
import { QuestionResponse } from '../../core/models/question.models';
import { TagResponse } from '../../core/models/tag.models';

const API = 'http://localhost:8080/api';
const COMBATE: TagResponse = { id: 1, name: 'Combate', color: '#C62828', createdAt: '2026-10-12T10:00:00' };
const FORMAS: TagResponse = { id: 2, name: 'Formas', color: '#1565C0', createdAt: '2026-10-12T10:00:00' };

function question(id: number, tags: TagResponse[] = []): QuestionResponse {
  return {
    id, text: `Pregunta número ${id}`, options: ['Sí', 'No'], correctAnswer: 0, explanation: '', difficulty: 'FACIL',
    tags, createdAt: '2026-10-12T10:00:00', updatedAt: '2026-10-12T10:00:00'
  };
}

describe('QuestionsComponent', () => {
  let fixture: ComponentFixture<QuestionsComponent>;
  let el: HTMLElement;

  afterEach(() => localStorage.clear());

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [QuestionsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    const http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(QuestionsComponent);
    el = fixture.nativeElement;
    await stable();
    http.expectOne(`${API}/tags`).flush([COMBATE, FORMAS]);
    http.expectOne(`${API}/questions`).flush([question(1, [COMBATE, FORMAS]), question(2, [FORMAS]), question(3)]);
    await stable();
  });

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function visibleTexts(): string[] {
    return Array.from(el.querySelectorAll('.question-text, .row-text')).map(t => t.textContent?.trim() ?? '');
  }

  function tagCheckbox(label: string): HTMLInputElement {
    const box = Array.from(el.querySelectorAll('.tag-filters mat-checkbox')).find(c => c.textContent?.includes(label));
    return box?.querySelector('input') as HTMLInputElement;
  }

  function clearButton(): HTMLButtonElement {
    return Array.from(el.querySelectorAll('.filters-card button')).find(b => b.textContent?.includes('Limpiar filtros')) as HTMLButtonElement;
  }

  function viewButton(label: string): HTMLButtonElement {
    return el.querySelector(`.view-switch button[aria-label="${label}"]`) as HTMLButtonElement;
  }

  function cards(): HTMLElement[] {
    return Array.from(el.querySelectorAll('app-question-card'));
  }

  it('al editar, el formulario aparece justo debajo de esa pregunta (RF-45)', async () => {
    const second = cards()[1];
    (Array.from(second.querySelectorAll('button')).find(b => b.textContent?.includes('edit')) as HTMLElement).click();
    await stable();

    const forms = el.querySelectorAll('app-question-form');
    expect(forms.length).toBe(1);
    expect(cards()[1].nextElementSibling?.tagName.toLowerCase()).toBe('app-question-form');
    expect((forms[0].querySelector('textarea') as HTMLTextAreaElement).value).toBe('Pregunta número 2');
  });

  it('una pregunta nueva se crea en el formulario de arriba', async () => {
    (Array.from(el.querySelectorAll('.page-header button')).find(b => b.textContent?.includes('Nueva pregunta')) as HTMLElement).click();
    await stable();

    const form = el.querySelector('app-question-form') as HTMLElement;
    expect(form).not.toBeNull();
    expect(form.closest('.questions-list')).toBeNull();
  });

  describe('vista del banco: tarjetas o lista', () => {
    it('por defecto muestra tarjetas', () => {
      expect(el.querySelector('.questions-list')?.classList).toContain('view-cards');
      expect(viewButton('Ver como tarjetas').getAttribute('aria-pressed')).toBe('true');
      expect(el.querySelectorAll('.question-card .option-item').length).toBe(6);
    });

    it('«Lista» cambia a una línea por pregunta y se recuerda', async () => {
      viewButton('Ver como lista').click();
      await stable();

      expect(el.querySelector('.questions-list')?.classList).toContain('view-list');
      expect(el.querySelectorAll('.question-row').length).toBe(3);
      expect(el.querySelectorAll('.option-item').length).toBe(0);
      expect(localStorage.getItem('questions-view')).toBe('list');
    });

    it('en la lista, pinchar una pregunta despliega sus opciones', async () => {
      viewButton('Ver como lista').click();
      await stable();

      (el.querySelectorAll('.question-row .row-main')[1] as HTMLElement).click();
      await stable();

      const options = Array.from(el.querySelectorAll('.option-item')).map(o => o.textContent?.replace(/\s+/g, ' ').trim());
      expect(options).toEqual(['check_circle Sí', 'radio_button_unchecked No']);
    });

    it('en la lista, editar también abre el formulario debajo de la pregunta', async () => {
      viewButton('Ver como lista').click();
      await stable();

      (Array.from(cards()[2].querySelectorAll('button')).find(b => b.textContent?.includes('edit')) as HTMLElement).click();
      await stable();

      expect(cards()[2].nextElementSibling?.tagName.toLowerCase()).toBe('app-question-form');
    });
  });

  describe('filtro por tags con casillas', () => {
    it('al principio no hay ninguna marcada y se ven todas las preguntas', () => {
      expect(tagCheckbox('Combate (1)').checked).toBe(false);
      expect(tagCheckbox('Formas (2)').checked).toBe(false);
      expect(tagCheckbox('Sin tag (1)').checked).toBe(false);
      expect(visibleTexts().length).toBe(3);
    });

    it('cada tag va rodeado de un borde de su color', () => {
      const label = Array.from(el.querySelectorAll('.tag-filters .tag-pill')).find(l => l.textContent?.includes('Combate')) as HTMLElement;

      expect(label.style.getPropertyValue('--tag-color')).toBe('#C62828');
    });

    it('marcar un tag muestra las preguntas que lo tienen, aunque tengan más tags', async () => {
      tagCheckbox('Formas').click();
      await stable();

      expect(visibleTexts()).toEqual(['Pregunta número 1', 'Pregunta número 2']);
    });

    it('con varios marcados, basta con tener uno de ellos', async () => {
      tagCheckbox('Combate').click();
      tagCheckbox('Sin tag').click();
      await stable();

      expect(visibleTexts()).toEqual(['Pregunta número 1', 'Pregunta número 3']);
    });

    it('desmarcarlos todos vuelve a mostrar todas', async () => {
      tagCheckbox('Combate').click();
      await stable();
      tagCheckbox('Combate').click();
      await stable();

      expect(visibleTexts().length).toBe(3);
    });
  });

  describe('limpiar filtros', () => {
    it('sin filtros, el botón está desactivado', () => {
      expect(clearButton().disabled).toBe(true);
    });

    it('quita el texto buscado y desmarca los tags', async () => {
      const search = el.querySelector('.filter-text input') as HTMLInputElement;
      search.value = 'número';
      search.dispatchEvent(new Event('input'));
      tagCheckbox('Combate').click();
      await stable();
      expect(visibleTexts()).toEqual(['Pregunta número 1']);

      clearButton().click();
      await stable();

      expect(visibleTexts().length).toBe(3);
      expect(search.value).toBe('');
      expect(tagCheckbox('Combate').checked).toBe(false);
      expect(clearButton().disabled).toBe(true);
    });
  });
});
