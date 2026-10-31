import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { CommunityComponent } from './community.component';
import { CommunityExamResponse, CommunityQuestionResponse } from '../../core/models/community.models';

const API = 'http://localhost:8080/api';

function exam(id: number, title: string, ownerName: string, favorite = false): CommunityExamResponse {
  return {
    id, title, code: `EXM-${id}`, ownerName, questionCount: 3, status: 'PUBLISHED',
    createdAt: '2026-10-20T10:00:00', expiresAt: null, favorite
  };
}

function question(questionId: number, text: string): CommunityQuestionResponse {
  return { questionId, text, options: ['A', 'B', 'C'], correctAnswers: [1], explanation: `Porque ${text}` };
}

const REGLAMENTO = exam(1, 'Reglamento básico', 'Ana', true);
const PUNTOS = exam(2, 'Puntuación', 'Luis');
const QUESTIONS = [question(11, 'Primera'), question(12, 'Segunda'), question(13, 'Tercera')];

/** Exámenes de prueba de otros profesores (RF-30), favoritos (RF-31) y copiar sus preguntas a mi banco (DE-12). */
describe('CommunityComponent', () => {
  let fixture: ComponentFixture<CommunityComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [CommunityComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } }
      ]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CommunityComponent);
    el = fixture.nativeElement;
    fixture.detectChanges();
    http.expectOne(`${API}/community/exams`).flush([REGLAMENTO, PUNTOS]);
    await stable();
  });

  afterEach(() => http.verify());

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function titles(): string[] {
    return Array.from(el.querySelectorAll('.exam-item .exam-title')).map(t => t.textContent?.trim() ?? '');
  }

  function item(title: string): HTMLElement {
    return Array.from(el.querySelectorAll('.exam-item')).find(i => i.textContent?.includes(title)) as HTMLElement;
  }

  function button(text: string): HTMLButtonElement {
    return Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes(text)) as HTMLButtonElement;
  }

  /** Elige el examen; sus preguntas se piden y se responden antes de esperar a que se estabilice. */
  function open(title: string): void {
    (item(title).querySelector('.exam-select') as HTMLButtonElement).click();
    fixture.detectChanges();
  }

  it('lista los exámenes de otros con su autor y cuántas preguntas tienen (RF-30)', () => {
    expect(titles()).toEqual(['Reglamento básico', 'Puntuación']);
    expect(item('Puntuación').textContent).toContain('Luis');
    expect(item('Puntuación').textContent).toContain('3 preguntas');
    expect(el.querySelector('.subtitle')?.textContent).toContain('2 exámenes');
  });

  it('busca por título o por autor', async () => {
    const input = el.querySelector('.search input') as HTMLInputElement;
    input.value = 'luis';
    input.dispatchEvent(new Event('input'));
    await stable();

    expect(titles()).toEqual(['Puntuación']);
  });

  it('«Favoritos» deja solo los marcados (RF-31)', async () => {
    button('Favoritos').click();
    await stable();

    expect(titles()).toEqual(['Reglamento básico']);
  });

  it('la estrella marca y desmarca el favorito', async () => {
    const star = () => item('Puntuación').querySelector('button.favorite') as HTMLButtonElement;
    expect(star().getAttribute('aria-pressed')).toBe('false');

    star().click();
    const add = http.expectOne(`${API}/exams/2/favorite`);
    expect(add.request.method).toBe('POST');
    add.flush(null);
    await stable();
    expect(star().getAttribute('aria-pressed')).toBe('true');

    star().click();
    const remove = http.expectOne(`${API}/exams/2/favorite`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
    await stable();
    expect(star().getAttribute('aria-pressed')).toBe('false');
  });

  it('si el servidor rechaza el favorito, no cambia y lo dice', async () => {
    (item('Puntuación').querySelector('button.favorite') as HTMLButtonElement).click();
    http.expectOne(`${API}/exams/2/favorite`).flush({ message: 'Este examen ya está en tus favoritos' },
      { status: 409, statusText: 'Conflict' });
    await stable();

    expect(item('Puntuación').querySelector('button.favorite')!.getAttribute('aria-pressed')).toBe('false');
    expect(document.body.textContent).toContain('Este examen ya está en tus favoritos');
  });

  describe('copiar preguntas a mi banco (DE-12)', () => {
    beforeEach(async () => {
      open('Reglamento básico');
      http.expectOne(`${API}/community/exams/1/questions`).flush(QUESTIONS);
      await stable();
    });

    function checkboxes(): HTMLInputElement[] {
      return Array.from(el.querySelectorAll('.question input[type="checkbox"]'));
    }

    it('enseña las preguntas con su solución, todas elegidas', () => {
      expect(el.querySelector('.detail h2')?.textContent).toContain('Reglamento básico');
      expect(el.querySelectorAll('.question').length).toBe(3);
      expect(el.querySelector('.question .correct')?.textContent).toContain('B');
      expect(el.querySelector('.question')?.textContent).toContain('Porque Primera');
      expect(checkboxes().every(c => c.checked)).toBe(true);
      expect(button('Copiar 3 a mi banco')).toBeTruthy();
    });

    it('copia solo las elegidas y dice cuántas ya tenía', async () => {
      checkboxes()[1].click();
      await stable();
      button('Copiar 2 a mi banco').click();

      const req = http.expectOne(`${API}/community/exams/1/copy-questions`);
      expect(req.request.body).toEqual({ questionIds: [11, 13] });
      req.flush({ copied: 1, alreadyInBank: 1 });
      await stable();

      expect(document.body.textContent).toContain('1 pregunta copiada a tu banco; 1 ya la tenías');
    });

    it('sin ninguna elegida no se puede copiar; «Elegir todas» las vuelve a marcar', async () => {
      button('Quitar todas').click();
      await stable();
      expect(button('Copiar 0 a mi banco').disabled).toBe(true);

      button('Elegir todas').click();
      await stable();
      expect(checkboxes().every(c => c.checked)).toBe(true);
    });

    it('al cambiar de examen se cargan sus preguntas, de nuevo todas elegidas', async () => {
      checkboxes()[0].click();
      await stable();

      open('Puntuación');
      http.expectOne(`${API}/community/exams/2/questions`).flush([question(21, 'Otra')]);
      await stable();

      expect(el.querySelector('.detail h2')?.textContent).toContain('Puntuación');
      expect(checkboxes().map(c => c.checked)).toEqual([true]);
    });
  });

  it('sin exámenes de otros lo dice', async () => {
    fixture = TestBed.createComponent(CommunityComponent);
    el = fixture.nativeElement;
    fixture.detectChanges();
    http.expectOne(`${API}/community/exams`).flush([]);
    await stable();

    expect(el.textContent).toContain('Ningún otro profesor ha publicado exámenes de prueba todavía.');
  });
});
