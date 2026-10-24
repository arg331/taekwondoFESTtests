import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatSliderHarness } from '@angular/material/slider/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { ExamQuestionPickerComponent } from './exam-question-picker.component';
import { Grade, QuestionResponse, QuestionType } from '../../../../core/models/question.models';
import { TagResponse } from '../../../../core/models/tag.models';

const tag = (id: number, name: string): TagResponse => ({ id, name, color: '#123456', createdAt: '2026-10-10T10:00:00' });
const COMBATE = tag(1, 'Combate');
const FORMAS = tag(2, 'Formas');
const ITF = tag(3, 'ITF');

function question(id: number, grade: Grade | null, tags: TagResponse[], text = `Pregunta ${id}`, type: QuestionType | null = null): QuestionResponse {
  return {
    id, text, options: ['Sí', 'No'], correctAnswers: [1], explanation: `Explicación ${id}`, grade, type, tags,
    createdAt: '2026-10-10T10:00:00', updatedAt: '2026-10-10T10:00:00', sharedBy: null
  };
}

const QUESTIONS = [
  question(1, 'BLANCO', [COMBATE, ITF]),
  question(2, 'VERDE', [COMBATE]),
  question(3, 'ROJO', [FORMAS], 'Pregunta 3', 'C'),
  question(4, 'BLANCO', []),
  question(5, 'VERDE', [FORMAS, ITF])
];

describe('ExamQuestionPickerComponent', () => {
  let fixture: ComponentFixture<ExamQuestionPickerComponent>;
  let el: HTMLElement;
  let loader: HarnessLoader;
  let created: QuestionResponse[];
  let dialogOpen: ReturnType<typeof vi.fn>;

  async function render(selected: number[] = [], targetCount = 3, questions: QuestionResponse[] = QUESTIONS): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ExamQuestionPickerComponent],
      providers: [{ provide: MatDialog, useValue: { open: dialogOpen } }]
    });
    fixture = TestBed.createComponent(ExamQuestionPickerComponent);
    fixture.componentRef.setInput('questions', questions);
    fixture.componentRef.setInput('tags', [COMBATE, FORMAS, ITF]);
    fixture.componentRef.setInput('targetCount', targetCount);
    fixture.componentRef.setInput('selectedIds', selected);
    created = [];
    fixture.componentInstance.questionCreated.subscribe(q => created.push(q));
    el = fixture.nativeElement;
    loader = TestbedHarnessEnvironment.loader(fixture);
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function visibleIds(): number[] {
    return Array.from(el.querySelectorAll('.question-pick-row'))
      .map(row => Number(row.getAttribute('data-id')));
  }

  function selected(): number[] {
    return fixture.componentInstance.selectedIds();
  }

  async function checkbox(label: string): Promise<MatCheckboxHarness> {
    return loader.getHarness(MatCheckboxHarness.with({ label: new RegExp(`^${label}`) }));
  }

  function button(text: string): HTMLButtonElement {
    return Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes(text)) as HTMLButtonElement;
  }

  beforeEach(() => {
    localStorage.clear();
    dialogOpen = vi.fn();
  });

  describe('filas', () => {
    it('cada fila muestra su grado, su tipo y sus tags', async () => {
      await render();

      const row = el.querySelector('.question-pick-row[data-id="1"]') as HTMLElement;
      expect(row.textContent).toContain('Blanco');
      expect(row.textContent).not.toContain('Tipo');
      expect(el.querySelector('.question-pick-row[data-id="3"]')?.textContent).toContain('Tipo C');
      expect(row.textContent).toContain('Combate');
      expect(row.textContent).toContain('ITF');
    });

    it('la vista previa muestra opciones, la correcta y la explicación sin cambiar la selección', async () => {
      await render();

      (el.querySelector('.question-pick-row[data-id="2"] .preview-toggle') as HTMLElement).click();
      await stable();

      const preview = el.querySelector('.question-preview') as HTMLElement;
      expect(preview.textContent).toContain('Sí');
      expect(preview.querySelector('.correct')?.textContent).toContain('No');
      expect(preview.textContent).toContain('Explicación 2');
      expect(selected()).toEqual([]);
    });

    it('hacer clic en la fila la selecciona o la quita', async () => {
      await render();

      (el.querySelector('.question-pick-row[data-id="3"]') as HTMLElement).click();
      await stable();
      expect(selected()).toEqual([3]);

      (el.querySelector('.question-pick-row[data-id="3"]') as HTMLElement).click();
      await stable();
      expect(selected()).toEqual([]);
    });
  });

  describe('filtro por tags', () => {
    it('cada casilla indica cuántas preguntas tienen ese tag', async () => {
      await render();

      expect(await (await checkbox('Combate')).getLabelText()).toContain('(2)');
      expect(await (await checkbox('Formas')).getLabelText()).toContain('(2)');
      expect(el.textContent).not.toContain('Sin tag');
    });

    it('al principio no hay ninguna marcada y se ven todas las preguntas', async () => {
      await render();

      expect(await (await checkbox('Combate')).isChecked()).toBe(false);
      expect(visibleIds()).toEqual([1, 2, 3, 4, 5]);
      expect(el.querySelector('.list-header')?.textContent).not.toContain('filtrando tags');
    });

    it('marcar un tag muestra las preguntas que lo tienen, aunque tengan más tags', async () => {
      await render();

      await (await checkbox('Combate')).check();
      await stable();

      expect(visibleIds()).toEqual([1, 2]);
      expect(el.querySelector('.list-header')?.textContent).toContain('2 de 5 preguntas (filtrando tags)');
    });

    it('«Desmarcar tags» quita todos los marcados; sin ninguno marcado está desactivado', async () => {
      await render();
      const clear = () => el.querySelector('.clear-tags') as HTMLButtonElement;
      expect(clear().disabled).toBe(true);

      await (await checkbox('Combate')).check();
      await (await checkbox('Formas')).check();
      await stable();
      clear().click();
      await stable();

      expect(await (await checkbox('Combate')).isChecked()).toBe(false);
      expect(await (await checkbox('Formas')).isChecked()).toBe(false);
      expect(visibleIds()).toEqual([1, 2, 3, 4, 5]);
    });

    it('los tags van en una cuadrícula y «Desmarcar tags» queda fuera, pegado a ella', async () => {
      await render();

      expect(el.querySelectorAll('.tag-grid .tag-pill').length).toBe(3);
      expect(el.querySelector('.tag-grid .clear-tags')).toBeNull();
      expect(el.querySelector('.tag-grid')?.nextElementSibling?.classList).toContain('clear-tags');
    });

    it('cada tag es una pastilla con el borde de su color', async () => {
      await render();

      const pill = Array.from(el.querySelectorAll('.tag-pill')).find(p => p.textContent?.includes('Combate')) as HTMLElement;
      expect(pill.style.getPropertyValue('--tag-color')).toBe('#123456');
    });
  });

  describe('barra de grados (RF-43)', () => {
    it('una pregunta sin grado se ve con la barra completa y se oculta al acotarla; su fila no lleva cinturón', async () => {
      await render([], 3, [...QUESTIONS, question(6, null, [])]);
      expect(visibleIds()).toContain(6);
      expect(el.querySelector('.question-pick-row[data-id="6"] app-grade-badge .grade-badge')).toBeNull();

      await (await (await loader.getHarness(MatSliderHarness)).getEndThumb()).setValue(8);
      await stable();

      expect(visibleIds()).not.toContain(6);
    });

    it('al mover el inicio a «Verde» se ocultan las de cinturón blanco', async () => {
      await render();
      const slider = await loader.getHarness(MatSliderHarness);

      await (await slider.getStartThumb()).setValue(4);
      await stable();

      expect(visibleIds()).toEqual([2, 3, 5]);
    });

    it('al mover el final a «Blanco» solo quedan las de cinturón blanco', async () => {
      await render();
      const slider = await loader.getHarness(MatSliderHarness);

      await (await slider.getEndThumb()).setValue(0);
      await stable();

      expect(visibleIds()).toEqual([1, 4]);
    });
  });

  describe('filtro por tipo (RF-44)', () => {
    function typeToggle(label: string): HTMLButtonElement {
      return Array.from(el.querySelectorAll('.type-filter button')).find(b => b.textContent?.trim() === label) as HTMLButtonElement;
    }

    it('por defecto se ven todos los tipos', async () => {
      await render();

      expect(visibleIds()).toEqual([1, 2, 3, 4, 5]);
    });

    it('«C» deja solo las de tipo C', async () => {
      await render();

      typeToggle('C').click();
      await stable();

      expect(visibleIds()).toEqual([3]);
    });

    it('«Sin tipo» deja las que no tienen tipo', async () => {
      await render();

      typeToggle('Sin tipo').click();
      await stable();

      expect(visibleIds()).toEqual([1, 2, 4, 5]);
    });
  });

  describe('carrito', () => {
    it('muestra las preguntas elegidas en orden con el recuento y permite quitarlas', async () => {
      await render([3, 1], 3);

      const items = Array.from(el.querySelectorAll('.cart-item')).map(i => i.textContent?.replace(/\s+/g, ' ').trim());
      expect(el.querySelector('.cart-count')?.textContent?.trim()).toBe('2');
      expect(el.querySelector('.cart-target')?.textContent).toContain('objetivo 3');
      expect(items[0]).toContain('Pregunta 3');
      expect(items[1]).toContain('Pregunta 1');
      // Sin carrito ni color de cinturón
      expect(el.querySelector('.cart-header mat-icon')?.textContent).not.toContain('shopping_cart');
      expect(el.querySelector('.cart-item app-grade-badge')).toBeNull();

      (el.querySelector('.cart-item .cart-remove') as HTMLElement).click();
      await stable();
      expect(selected()).toEqual([1]);
    });

    it('pasarse del objetivo no es un error (RF-46)', async () => {
      await render([1, 2, 3, 4], 3);

      expect(el.querySelector('.cart-warning')).toBeNull();
    });

    it('pasar de 50 sí lo avisa', async () => {
      await render(Array.from({ length: 51 }, (_, i) => i + 1), 3);

      expect(el.querySelector('.cart-warning')?.textContent).toContain('máximo 50');
    });

    it('«Vaciar» quita todas', async () => {
      await render([3, 1], 3);

      button('Vaciar').click();
      await stable();

      expect(selected()).toEqual([]);
    });
  });

  describe('completar al azar', () => {
    it('rellena los huecos solo con preguntas que cumplen los filtros', async () => {
      await render([3], 3);
      await (await checkbox('Combate')).check();
      await stable();

      button('Completar al azar').click();
      await stable();

      expect(selected().length).toBe(3);
      expect([...selected()].sort()).toEqual([1, 2, 3]);
    });

    it('si no hay bastantes preguntas con esos filtros, añade las que hay y lo avisa', async () => {
      await render([3], 4);
      const slider = await loader.getHarness(MatSliderHarness);
      await (await slider.getEndThumb()).setValue(0);
      await stable();

      button('Completar al azar').click();
      await stable();

      expect([...selected()].sort()).toEqual([1, 3, 4]);
      expect(el.querySelector('.random-note')?.textContent).toContain('Solo había 2');
    });

    it('si el examen ya está completo, el botón está desactivado', async () => {
      await render([1, 2, 3], 3);

      expect(button('Completar al azar').disabled).toBe(true);
    });
  });

  it('«Nueva pregunta» abre el formulario en un diálogo y la pregunta creada queda seleccionada', async () => {
    const nueva = question(99, 'VERDE', [FORMAS], 'Pregunta nueva');
    dialogOpen.mockReturnValue({ afterClosed: () => of(nueva) });
    await render([3], 3);

    button('Nueva pregunta').click();
    await stable();

    expect(dialogOpen).toHaveBeenCalled();
    expect(created).toEqual([nueva]);
    expect(selected()).toEqual([3, 99]);
  });

  it('si se cancela el diálogo no cambia nada', async () => {
    dialogOpen.mockReturnValue({ afterClosed: () => of(undefined) });
    await render([3], 3);

    button('Nueva pregunta').click();
    await stable();

    expect(created).toEqual([]);
    expect(selected()).toEqual([3]);
  });

  it('recuerda los filtros de tags y grado entre exámenes', async () => {
    await render();
    await (await checkbox('Combate')).check();
    await (await (await loader.getHarness(MatSliderHarness)).getStartThumb()).setValue(4);
    await stable();
    TestBed.resetTestingModule();

    await render();

    expect(await (await checkbox('Combate')).isChecked()).toBe(true);
    expect(visibleIds()).toEqual([2]);
  });
});
