import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { QuestionsComponent } from './questions.component';
import { QuestionResponse, QuestionType } from '../../core/models/question.models';
import { TagResponse } from '../../core/models/tag.models';
import { ConfirmService } from '../../core/services/confirm.service';
import { of } from 'rxjs';

const API = 'http://localhost:8080/api';
const COMBATE: TagResponse = { id: 1, name: 'Combate', color: '#C62828', createdAt: '2026-10-12T10:00:00' };
const FORMAS: TagResponse = { id: 2, name: 'Formas', color: '#1565C0', createdAt: '2026-10-12T10:00:00' };

function question(id: number, tags: TagResponse[] = [], type: QuestionType | null = null): QuestionResponse {
  return {
    id, text: `Pregunta número ${id}`, options: ['Sí', 'No'], correctAnswers: [0], explanation: '', grade: 'BLANCO', type,
    tags, createdAt: '2026-10-12T10:00:00', updatedAt: '2026-10-12T10:00:00', sharedBy: null,
    usedInExams: 0, deletedAt: null
  };
}

/** Una pregunta eliminada del banco, que sigue en un examen publicado. */
function deleted(id: number): QuestionResponse {
  return { ...question(id), text: `Eliminada ${id}`, usedInExams: 1, deletedAt: '2026-10-20T10:00:00' };
}

/** El tag «Combate» de la profesora que comparte: otro id, el mismo nombre. */
const COMBATE_ANA: TagResponse = { id: 90, name: 'Combate', color: '#000000', createdAt: '2026-10-12T10:00:00' };

/** Preguntas de un examen que me ha compartido Ana (RF-57). */
function shared(id: number, tags: TagResponse[] = []): QuestionResponse {
  return { ...question(id, tags), text: `Compartida ${id}`, sharedBy: 'Ana Pérez', usedInExams: null };
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
    http.expectOne(`${API}/questions`).flush([
      { ...question(1, [COMBATE, FORMAS]), usedInExams: 2 }, { ...question(2, [FORMAS], 'C'), usedInExams: 1 }, question(3)]);
    http.expectOne(`${API}/shares/questions`).flush([shared(10, [COMBATE_ANA]), shared(11)]);
    http.expectOne(`${API}/questions/deleted`).flush([deleted(5)]);
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

  function source(label: string): HTMLButtonElement {
    return Array.from(el.querySelectorAll('.source-switch button')).find(b => b.textContent?.includes(label)) as HTMLButtonElement;
  }

  function cardButton(card: HTMLElement, label: string): HTMLButtonElement {
    return Array.from(card.querySelectorAll('button')).find(b => b.textContent?.includes(label)) as HTMLButtonElement;
  }

  describe('por páginas de 20 (RNF-09)', () => {
    function next(): HTMLButtonElement {
      return el.querySelector('.mat-mdc-paginator-navigation-next') as HTMLButtonElement;
    }

    async function manyQuestions(n: number): Promise<void> {
      fixture.componentInstance.questions.set(Array.from({ length: n }, (_, i) => question(100 + i)));
      await stable();
    }

    it('con pocas no hay paginador', () => {
      expect(el.querySelector('mat-paginator')).toBeNull();
    });

    it('con más de 20, 20 por página sobre las filtradas; filtrar vuelve a la primera', async () => {
      await manyQuestions(45);

      expect(cards()).toHaveLength(20);
      expect(el.querySelector('.subtitle')?.textContent).toContain('45 de 45 preguntas');
      next().click();
      await stable();
      expect(visibleTexts()[0]).toBe('Pregunta número 120');

      const search = el.querySelector('.filter-text input') as HTMLInputElement;
      search.value = 'número 1';
      search.dispatchEvent(new Event('input'));
      await stable();
      expect(el.querySelector('.mat-mdc-paginator-range-label')?.textContent?.trim()).toBe('1 – 20 de 45');
      expect(visibleTexts()[0]).toBe('Pregunta número 100');
    });

    it('si se vacía la última página, enseña la anterior', async () => {
      await manyQuestions(21);
      next().click();
      await stable();
      expect(visibleTexts()).toEqual(['Pregunta número 120']);

      fixture.componentInstance.questions.update(list => list.slice(0, 20));
      await stable();

      expect(cards()).toHaveLength(20);
      expect(el.querySelector('mat-paginator')).toBeNull();
    });
  });

  describe('preguntas eliminadas', () => {
    it('cada pregunta propia dice en gris en cuántos exámenes se usa; las compartidas no', async () => {
      const usage = cards().map(c => c.querySelector('.usage')?.textContent?.trim());
      expect(usage).toEqual(['Se usa en 2 exámenes', 'Se usa en 1 examen', 'No se usa en ningún examen']);

      source('Preguntas compartidas conmigo').click();
      await stable();
      expect(el.querySelector('.usage')).toBeNull();
    });

    it('eliminar la saca del banco y la pasa a «Preguntas eliminadas», avisando de que se puede recuperar', async () => {
      const confirmSpy = vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));

      cardButton(cards()[0], 'delete').click();
      http.expectOne(r => r.method === 'DELETE' && r.url === `${API}/questions/1`).flush(null);
      await stable();

      expect(confirmSpy.mock.calls[0][0].message).toContain('Podrás recuperarla en «Preguntas eliminadas»');
      expect(visibleTexts()).toEqual(['Pregunta número 2', 'Pregunta número 3']);
      source('Preguntas eliminadas').click();
      await stable();
      expect(visibleTexts()).toEqual(['Pregunta número 1', 'Eliminada 5']);
      confirmSpy.mockRestore();
    });

    it('las eliminadas solo se pueden recuperar; al recuperarla vuelve al banco', async () => {
      source('Preguntas eliminadas').click();
      await stable();
      expect(visibleTexts()).toEqual(['Eliminada 5']);
      expect(cardButton(cards()[0], 'edit')).toBeUndefined();

      cardButton(cards()[0], 'Recuperar').click();
      http.expectOne(r => r.method === 'POST' && r.url === `${API}/questions/5/restore`).flush({ ...deleted(5), deletedAt: null });
      await stable();

      expect(visibleTexts()).toEqual([]);
      source('Mi banco de preguntas').click();
      await stable();
      expect(visibleTexts()).toContain('Eliminada 5');
    });
  });

  describe('preguntas compartidas conmigo (RF-57)', () => {
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

  describe('al cambiar o borrar un tag, las preguntas lo reflejan sin recargar (RF-08)', () => {
    function tagManager(): import('./components/tag-manager/tag-manager.component').TagManagerComponent {
      return fixture.debugElement.query(d => d.name === 'app-tag-manager').componentInstance;
    }

    function chipsOf(card: HTMLElement): string[] {
      return Array.from(card.querySelectorAll('.tag-chip-small')).map(c => c.textContent?.trim() ?? '');
    }

    it('un tag borrado desaparece de sus preguntas y del filtro', async () => {
      tagCheckbox('Combate').click();
      await stable();

      tagManager().removed.emit(COMBATE.id);
      await stable();

      expect(chipsOf(cards()[0])).toEqual(['Formas']);
      expect(visibleTexts().length).toBe(3);
    });

    it('un tag renombrado cambia en sus preguntas', async () => {
      tagManager().updated.emit({ ...COMBATE, name: 'Reglas' });
      await stable();

      expect(chipsOf(cards()[0])).toContain('Reglas');
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
