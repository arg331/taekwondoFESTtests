import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatSliderHarness } from '@angular/material/slider/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { ExamQuestionPickerComponent } from './exam-question-picker.component';
import { Difficulty, QuestionResponse } from '../../../../core/models/question.models';
import { TagResponse } from '../../../../core/models/tag.models';

const tag = (id: number, name: string): TagResponse => ({ id, name, color: '#123456', createdAt: '2026-10-10T10:00:00' });
const COMBATE = tag(1, 'Combate');
const FORMAS = tag(2, 'Formas');
const ITF = tag(3, 'ITF');

function question(id: number, difficulty: Difficulty, tags: TagResponse[], text = `Pregunta ${id}`): QuestionResponse {
  return {
    id, text, options: ['Sí', 'No'], correctAnswer: 1, explanation: `Explicación ${id}`, difficulty, tags,
    createdAt: '2026-10-10T10:00:00', updatedAt: '2026-10-10T10:00:00'
  };
}

const QUESTIONS = [
  question(1, 'FACIL', [COMBATE, ITF]),
  question(2, 'MEDIO', [COMBATE]),
  question(3, 'DIFICIL', [FORMAS]),
  question(4, 'FACIL', []),
  question(5, 'MEDIO', [FORMAS, ITF])
];

describe('ExamQuestionPickerComponent', () => {
  let fixture: ComponentFixture<ExamQuestionPickerComponent>;
  let el: HTMLElement;
  let loader: HarnessLoader;
  let created: QuestionResponse[];
  let dialogOpen: ReturnType<typeof vi.fn>;

  async function render(selected: number[] = [], requiredCount = 3): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ExamQuestionPickerComponent],
      providers: [{ provide: MatDialog, useValue: { open: dialogOpen } }]
    });
    fixture = TestBed.createComponent(ExamQuestionPickerComponent);
    fixture.componentRef.setInput('questions', QUESTIONS);
    fixture.componentRef.setInput('tags', [COMBATE, FORMAS, ITF]);
    fixture.componentRef.setInput('requiredCount', requiredCount);
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
    it('cada fila muestra su dificultad y sus tags', async () => {
      await render();

      const row = el.querySelector('.question-pick-row[data-id="1"]') as HTMLElement;
      expect(row.textContent).toContain('Fácil');
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
      expect(await (await checkbox('Sin tag')).getLabelText()).toContain('(1)');
    });

    it('desmarcar un tag oculta las preguntas que no tienen ningún otro tag marcado', async () => {
      await render();

      await (await checkbox('Combate')).uncheck();
      await stable();

      expect(visibleIds()).toEqual([1, 3, 4, 5]);
    });

    it('la casilla «Sin tag» controla las preguntas sin tags', async () => {
      await render();

      await (await checkbox('Sin tag')).uncheck();
      await stable();

      expect(visibleIds()).not.toContain(4);
    });
  });

  describe('barra de dificultad', () => {
    it('al mover el inicio a «Medio» se ocultan las fáciles', async () => {
      await render();
      const slider = await loader.getHarness(MatSliderHarness);

      await (await slider.getStartThumb()).setValue(1);
      await stable();

      expect(visibleIds()).toEqual([2, 3, 5]);
    });

    it('al mover el final a «Fácil» solo quedan las fáciles', async () => {
      await render();
      const slider = await loader.getHarness(MatSliderHarness);

      await (await slider.getEndThumb()).setValue(0);
      await stable();

      expect(visibleIds()).toEqual([1, 4]);
    });
  });

  describe('carrito', () => {
    it('muestra las preguntas elegidas en orden con el recuento y permite quitarlas', async () => {
      await render([3, 1], 3);

      const items = Array.from(el.querySelectorAll('.cart-item')).map(i => i.textContent?.replace(/\s+/g, ' ').trim());
      expect(el.querySelector('.cart-count')?.textContent).toContain('2 / 3');
      expect(items[0]).toContain('Pregunta 3');
      expect(items[1]).toContain('Pregunta 1');

      (el.querySelector('.cart-item .cart-remove') as HTMLElement).click();
      await stable();
      expect(selected()).toEqual([1]);
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
      await (await checkbox('Formas')).uncheck();
      await (await checkbox('ITF')).uncheck();
      await (await checkbox('Sin tag')).uncheck();
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
    const nueva = question(99, 'MEDIO', [FORMAS], 'Pregunta nueva');
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

  it('recuerda los filtros de tags y dificultad entre exámenes', async () => {
    await render();
    await (await checkbox('Combate')).uncheck();
    await (await (await loader.getHarness(MatSliderHarness)).getStartThumb()).setValue(1);
    await stable();
    TestBed.resetTestingModule();

    await render();

    expect(await (await checkbox('Combate')).isChecked()).toBe(false);
    expect(visibleIds()).toEqual([3, 5]);
  });
});
