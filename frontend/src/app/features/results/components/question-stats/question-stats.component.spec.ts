import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { QuestionStatsComponent } from './question-stats.component';
import { QuestionStatsResponse } from '../../../../core/models/result.models';

const API = 'http://localhost:8080/api';

/** Como llegan del servidor: en el orden del examen. */
const STATS: QuestionStatsResponse[] = [
  { questionId: 1, text: '¿Cuántos jueces de esquina hay?', options: ['Dos', 'Cuatro', 'Seis'], correctAnswers: [1],
    submissions: 4, correct: 3, blank: 0, optionCounts: [1, 3, 0], successRate: 75 },
  { questionId: 2, text: '¿Qué técnicas puntúan dos puntos?', options: ['Patada al tronco', 'Puño', 'Patada a la cabeza'],
    correctAnswers: [0, 2], submissions: 4, correct: 1, blank: 1, optionCounts: [2, 2, 1], successRate: 25 },
  { questionId: 3, text: 'Pregunta fácil', options: ['Sí', 'No'], correctAnswers: [0],
    submissions: 4, correct: 4, blank: 0, optionCounts: [4, 0], successRate: 100 }
];

/** Acierto por pregunta (RF-28): para detectar preguntas mal planteadas. */
describe('QuestionStatsComponent', () => {
  let fixture: ComponentFixture<QuestionStatsComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function render(respond: (url: string) => void = url => http.expectOne(url).flush(STATS)): Promise<void> {
    TestBed.configureTestingModule({
      imports: [QuestionStatsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(QuestionStatsComponent);
    fixture.componentRef.setInput('examId', 7);
    el = fixture.nativeElement;
    // El resource tiene la app «ocupada» hasta que llega la respuesta: se responde antes de esperar
    fixture.detectChanges();
    respond(`${API}/results/exam/7/questions`);
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function rows(): HTMLElement[] {
    return Array.from(el.querySelectorAll('.question-row'));
  }

  afterEach(() => http.verify());

  it('de la menos a la más acertada, con su número en el examen, el % y cuántas la acertaron', async () => {
    await render();

    expect(rows().map(r => r.querySelector('.question-number')?.textContent?.trim())).toEqual(['P2', 'P1', 'P3']);
    expect(rows().map(r => r.querySelector('.success-rate')?.textContent?.trim())).toEqual(['25 %', '75 %', '100 %']);
    expect(rows()[0].textContent).toContain('¿Qué técnicas puntúan dos puntos?');
    expect(rows()[0].textContent).toContain('1 de 4');
  });

  it('al pinchar una pregunta, cuántos marcaron cada opción, con las correctas señaladas, y cuántos en blanco', async () => {
    await render();
    expect(el.querySelector('.option-stat')).toBeNull();

    (rows()[0].querySelector('.question-toggle') as HTMLButtonElement).click();
    await stable();

    const options = Array.from(el.querySelectorAll('.option-stat'));
    expect(options.map(o => o.querySelector('.option-text')?.textContent?.trim()))
      .toEqual(['Patada al tronco', 'Puño', 'Patada a la cabeza']);
    expect(options.map(o => o.classList.contains('correct'))).toEqual([true, false, true]);
    expect(options.map(o => o.querySelector('.option-count')?.textContent?.trim())).toEqual(['2 (50 %)', '2 (50 %)', '1 (25 %)']);
    expect(el.querySelector('.blank-stat')?.textContent).toContain('1 en blanco');

    (rows()[0].querySelector('.question-toggle') as HTMLButtonElement).click();
    await stable();
    expect(el.querySelector('.option-stat')).toBeNull();
  });

  it('una pregunta sin entregas sale sin porcentaje', async () => {
    await render(url => http.expectOne(url).flush([{ ...STATS[0], submissions: 0, correct: 0, optionCounts: [0, 0, 0], successRate: null }]));

    expect(rows()[0].querySelector('.success-rate')?.textContent?.trim()).toBe('—');
  });

  it('si falla la carga, lo dice', async () => {
    await render(url => http.expectOne(url).flush(null, { status: 500, statusText: 'Error' }));

    expect(el.textContent).toContain('No se ha podido cargar el acierto por pregunta.');
  });
});
