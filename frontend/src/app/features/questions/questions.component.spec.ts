import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { QuestionsComponent } from './questions.component';
import { QuestionResponse, QuestionType } from '../../core/models/question.models';
import { TagResponse } from '../../core/models/tag.models';

const API = 'http://localhost:8080/api';
const COMBATE: TagResponse = { id: 1, name: 'Combate', color: '#C62828', createdAt: '2026-10-12T10:00:00' };
const FORMAS: TagResponse = { id: 2, name: 'Formas', color: '#1565C0', createdAt: '2026-10-12T10:00:00' };

function question(id: number, tags: TagResponse[] = [], type: QuestionType | null = null): QuestionResponse {
  return {
    id, text: `Pregunta número ${id}`, options: ['Sí', 'No'], correctAnswers: [0], explanation: '', grade: 'BLANCO', type,
    tags, createdAt: '2026-10-12T10:00:00', updatedAt: '2026-10-12T10:00:00', sharedBy: null
  };
}

/** El tag «Combate» de la profesora que comparte: otro id, el mismo nombre. */
const COMBATE_ANA: TagResponse = { id: 90, name: 'Combate', color: '#000000', createdAt: '2026-10-12T10:00:00' };

/** Preguntas de un examen que me ha compartido Ana (RF-57). */
function shared(id: number, tags: TagResponse[] = []): QuestionResponse {
  return { ...question(id, tags), text: `Compartida ${id}`, sharedBy: 'Ana Pérez' };
}

describe('QuestionsComponent', () => {
  let fixture: ComponentFixture<QuestionsComponent>;
  let el: HTMLElement;
  let http: HttpTestingController;

  afterEach(() => localStorage.clear());

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [QuestionsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(QuestionsComponent);
    el = fixture.nativeElement;
    await stable();
    http.expectOne(`${API}/tags`).flush([COMBATE, FORMAS]);
    http.expectOne(`${API}/questions`).flush([question(1, [COMBATE, FORMAS]), question(2, [FORMAS], 'C'), question(3)]);
    http.expectOne(`${API}/shares/questions`).flush([shared(10, [COMBATE_ANA]), shared(11)]);
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
    const box = Array.from(el.querySelectorAll('.tag-grid mat-checkbox')).find(c => c.textContent?.includes(label));
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

  describe('preguntas compartidas conmigo (RF-57)', () => {
    function source(label: string): HTMLButtonElement {
      return Array.from(el.querySelectorAll('.source-switch button')).find(b => b.textContent?.includes(label)) as HTMLButtonElement;
    }

    it('tres vistas: mi banco por defecto, las compartidas conmigo y todas las disponibles', async () => {
      expect(visibleTexts()).toEqual(['Pregunta número 1', 'Pregunta número 2', 'Pregunta número 3']);

      source('Preguntas compartidas conmigo').click();
      await stable();
      expect(visibleTexts()).toEqual(['Compartida 10', 'Compartida 11']);
      expect(el.querySelector('.subtitle')?.textContent).toContain('2 de 2 preguntas');

      source('Todas las preguntas disponibles').click();
      await stable();
      expect(visibleTexts().length).toBe(5);
    });

    it('las compartidas avisan en gris de que no se pueden modificar y no tienen editar ni borrar', async () => {
      expect(el.querySelector('.shared-hint')).toBeNull();

      source('Preguntas compartidas conmigo').click();
      await stable();

      expect(el.querySelector('.shared-hint')?.textContent?.trim()).toBe('No puedes modificar las preguntas que han compartido contigo');
      expect(cards()[0].querySelectorAll('.question-actions button').length).toBe(0);
      expect(cards()[0].querySelector('.shared-by')?.textContent?.trim()).toBe('Compartida por Ana Pérez');
    });

    it('en «Todas», el filtro de tags va por nombre: también cuenta el «Combate» de quien la compartió', async () => {
      source('Todas las preguntas disponibles').click();
      await stable();

      tagCheckbox('Combate').click();
      await stable();

      expect(visibleTexts()).toEqual(['Pregunta número 1', 'Compartida 10']);
    });
  });

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
      expect(el.textContent).not.toContain('Sin tag');
      expect(visibleTexts().length).toBe(3);
    });

    it('cada tag va rodeado de un borde de su color', () => {
      const label = Array.from(el.querySelectorAll('.tag-grid .tag-pill')).find(l => l.textContent?.includes('Combate')) as HTMLElement;

      expect(label.style.getPropertyValue('--tag-color')).toBe('#C62828');
    });

    it('marcar un tag muestra las preguntas que lo tienen, aunque tengan más tags', async () => {
      tagCheckbox('Formas').click();
      await stable();

      expect(visibleTexts()).toEqual(['Pregunta número 1', 'Pregunta número 2']);
    });

    it('«Desmarcar tags» quita todos los marcados y vuelven a verse todas', async () => {
      tagCheckbox('Combate').click();
      tagCheckbox('Formas').click();
      await stable();
      (el.querySelector('.clear-tags') as HTMLButtonElement).click();
      await stable();

      expect(tagCheckbox('Combate').checked).toBe(false);
      expect(visibleTexts().length).toBe(3);
    });

    it('desmarcarlos todos vuelve a mostrar todas', async () => {
      tagCheckbox('Combate').click();
      await stable();
      tagCheckbox('Combate').click();
      await stable();

      expect(visibleTexts().length).toBe(3);
    });
  });

  describe('filtro por tipo (RF-44)', () => {
    function typeToggle(label: string): HTMLButtonElement {
      return Array.from(el.querySelectorAll('.type-filter button')).find(b => b.textContent?.trim() === label) as HTMLButtonElement;
    }

    it('«C» deja solo las de tipo C', async () => {
      typeToggle('C').click();
      await stable();

      expect(visibleTexts()).toEqual(['Pregunta número 2']);
    });

    it('«Sin tipo» deja las que no tienen tipo y «Limpiar filtros» lo quita', async () => {
      typeToggle('Sin tipo').click();
      await stable();
      expect(visibleTexts()).toEqual(['Pregunta número 1', 'Pregunta número 3']);

      clearButton().click();
      await stable();
      expect(visibleTexts().length).toBe(3);
    });

    it('la tarjeta muestra el grado y el tipo', () => {
      const card = cards()[1];
      expect(card.textContent).toContain('Blanco');
      expect(card.textContent).toContain('Tipo C');
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
