import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExamDetailComponent } from './exam-detail.component';
import { ExamResponse, ExamStatus, PublishedQuestionResponse } from '../../../../core/models/exam.models';
import { QuestionResponse } from '../../../../core/models/question.models';
import { QrCodeService } from '../../../../core/services/qr-code.service';
import { QrColorService } from '../../../../core/services/qr-color.service';
import { By } from '@angular/platform-browser';
import { ExamQuestionPickerComponent } from '../exam-question-picker/exam-question-picker.component';

const API = 'http://localhost:8080/api';
/** QR falso: el data URL indica el tamaño pedido para poder comprobarlo. */
const fakeQr = (_text: string, size: number, _color?: string): Promise<string> => Promise.resolve(`data:image/png;base64,QR-${size}`);

function exam(status: ExamStatus, questionIds: number[] = []): ExamResponse {
  return {
    id: 7, title: 'Examen', ownerId: 1, status, visibility: 'PUBLIC', accessMode: 'OPEN',
    config: { numberOfQuestions: 10, timeLimitMinutes: null, showScore: true, randomizeOptions: false,
      randomizeQuestionOrder: false },
    questionIds, generationTags: [], code: status === 'DRAFT' ? null : 'EXM-1234ABCD',
    createdAt: '2026-10-08T10:00:00', expiresAt: null
  };
}

function question(id: number): QuestionResponse {
  return {
    id, text: `Pregunta ${id}`, options: ['A', 'B'], correctAnswer: 0, explanation: '', grade: 'BLANCO', type: null,
    tags: [], createdAt: '2026-10-08T10:00:00', updatedAt: '2026-10-08T10:00:00'
  };
}

/** La copia publicada de una pregunta (DE-03); por defecto, igual que la del banco. */
function published(id: number, text = `Pregunta ${id}`, editedSincePublished = false): PublishedQuestionResponse {
  return { questionId: id, text, options: ['A', 'B'], correctAnswer: 0, explanation: null, editedSincePublished };
}

/** Caracterización del panel de un examen: acciones disponibles por estado y llamadas al publicar. */
describe('ExamDetailComponent', () => {
  let fixture: ComponentFixture<ExamDetailComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;
  let changed: ExamResponse[];
  let qr: { toDataUrl: ReturnType<typeof vi.fn> };
  /** Lo que devuelve el servidor como preguntas publicadas del examen que se muestra. */
  let publishedFor: (exam: ExamResponse) => PublishedQuestionResponse[];
  let publishedRequests: string[];

  async function render(examResponse: ExamResponse): Promise<void> {
    qr = { toDataUrl: vi.fn(fakeQr) };
    publishedFor ??= e => e.questionIds.map(id => published(id));
    publishedRequests = [];
    TestBed.configureTestingModule({
      imports: [ExamDetailComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: QrCodeService, useValue: qr }]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExamDetailComponent);
    fixture.componentRef.setInput('exam', examResponse);
    fixture.componentRef.setInput('questions', [1, 2, 3, 4, 5, 6].map(question));
    changed = [];
    fixture.componentInstance.changed.subscribe(e => changed.push(e));
    el = fixture.nativeElement;
    await stable();
  }

  /** Responde a las peticiones de preguntas publicadas antes de esperar: si no, la app sigue ocupada. */
  async function stable(): Promise<void> {
    fixture.detectChanges();
    http.match(r => r.url.endsWith('/published-questions'))
      .forEach(req => {
        publishedRequests.push(req.request.url);
        req.flush(publishedFor(fixture.componentInstance.exam()));
      });
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

  afterEach(() => {
    http.verify();
    localStorage.clear();
    publishedFor = undefined!;
  });

  it('acciones según el estado del examen', async () => {
    await render(exam('DRAFT'));
    expect(buttonTexts().join(' | ')).toContain('Publicar examen');
    expect(buttonTexts().join(' | ')).toContain('Eliminar borrador');

    fixture.componentRef.setInput('exam', exam('PUBLISHED', [1, 2]));
    await stable();
    expect(buttonTexts().join(' | ')).toContain('Cambiar plazo');
    expect(buttonTexts().join(' | ')).toContain('Pasar a oficial (privado)');
    expect(buttonTexts().join(' | ')).toContain('Cerrar examen');

    fixture.componentRef.setInput('exam', exam('EXPIRED', [1, 2]));
    await stable();
    expect(buttonTexts().join(' | ')).toContain('Reabrir');
    expect(buttonTexts().join(' | ')).not.toContain('Cerrar examen');
  });

  function pick(...indexes: number[]): void {
    const rows = el.querySelectorAll('.question-pick-row');
    indexes.forEach(i => (rows[i] as HTMLElement).click());
  }

  describe('número de preguntas = las elegidas (RF-46)', () => {
    it('con menos de 5 elegidas no se puede publicar', async () => {
      await render(exam('DRAFT'));

      expect(el.textContent).toContain('Selecciona al menos 5 preguntas para publicar (tienes 0).');
      expect(button('Publicar examen').disabled).toBe(true);
    });

    it('con más de 50 tampoco', async () => {
      await render(exam('DRAFT'));
      fixture.componentInstance.selectedIds.set(Array.from({ length: 51 }, (_, i) => i + 1));
      await stable();

      expect(el.textContent).toContain('Un examen admite como máximo 50 preguntas (tienes 51).');
      expect(button('Publicar examen').disabled).toBe(true);
    });

    it('se puede publicar con más o menos preguntas que el objetivo', async () => {
      await render(exam('DRAFT'));
      pick(0, 1, 2, 3, 4, 5);
      await stable();

      expect(el.querySelector('.section-title')?.textContent).toContain('6 elegidas');
      expect(button('Publicar examen').disabled).toBe(false);
    });
  });

  it('publicar con selección sin guardar: primero guarda las preguntas y luego publica', async () => {
    await render(exam('DRAFT'));
    pick(0, 2, 3, 4, 5);
    await stable();

    button('Publicar examen').click();
    await stable();

    const save = http.expectOne(`${API}/exams/7/questions`);
    expect(save.request.method).toBe('PUT');
    expect(save.request.body).toEqual({ questionIds: [1, 3, 4, 5, 6] });
    save.flush(exam('DRAFT', [1, 3, 4, 5, 6]));
    const publish = http.expectOne(`${API}/exams/7/publish`);
    expect(publish.request.body).toEqual({ visibility: 'PUBLIC', accessMode: 'OPEN', expiresAt: null, neverExpires: false });
    publish.flush(exam('PUBLISHED', [1, 3, 4, 5, 6]));
    await stable();

    expect(changed.at(-1)?.status).toBe('PUBLISHED');
  });

  describe('publicar sin caducidad (RF-48)', () => {
    it('«Sin caducidad» desactiva la fecha y lo envía al publicar', async () => {
      await render(exam('DRAFT', [1, 2, 3, 4, 5]));
      const date = el.querySelector('.publish-options input[type=datetime-local]') as HTMLInputElement;
      date.value = '2026-12-31T10:00';
      date.dispatchEvent(new Event('input'));

      (el.querySelector('.never-expires input[type=checkbox]') as HTMLInputElement).click();
      await stable();
      expect(date.disabled).toBe(true);

      button('Publicar examen').click();
      const publish = http.expectOne(`${API}/exams/7/publish`);
      expect(publish.request.body).toEqual({ visibility: 'PUBLIC', accessMode: 'OPEN', expiresAt: null, neverExpires: true });
      publish.flush(exam('PUBLISHED', [1, 2, 3, 4, 5]));
    });

    it('un examen publicado sin fecha lo indica en el resumen', async () => {
      await render(exam('PUBLISHED', [1, 2]));

      expect(el.querySelector('.config-summary')?.textContent).toContain('Sin caducidad');
    });

    it('«Cambiar plazo» parte de la fecha actual', async () => {
      await render({ ...exam('PUBLISHED', [1, 2]), expiresAt: '2026-12-31T10:00:00' });

      expect((el.querySelector('.inline-form input') as HTMLInputElement).value).toBe('2026-12-31T10:00');
    });

    it('tras cambiar el plazo, el campo muestra la fecha nueva', async () => {
      await render({ ...exam('PUBLISHED', [1, 2]), expiresAt: '2026-12-31T10:00:00' });

      fixture.componentRef.setInput('exam', { ...exam('PUBLISHED', [1, 2]), expiresAt: '2027-01-15T09:30:00' });
      await stable();

      expect((el.querySelector('.inline-form input') as HTMLInputElement).value).toBe('2027-01-15T09:30');
    });

    it('al reabrir un examen cerrado, la fecha empieza vacía', async () => {
      await render({ ...exam('EXPIRED', [1, 2]), expiresAt: '2026-10-01T10:00:00' });

      expect((el.querySelector('.inline-form input') as HTMLInputElement).value).toBe('');
    });

    it('con fecha muestra hasta cuándo', async () => {
      await render({ ...exam('PUBLISHED', [1, 2]), expiresAt: '2026-12-31T10:00:00' });

      expect(el.querySelector('.config-summary')?.textContent).toContain('Hasta 31/12/2026 10:00');
      expect(el.querySelector('.config-summary')?.textContent).not.toContain('Sin caducidad');
    });
  });

  describe('público = examen de prueba, privado = oficial (RF-49)', () => {
    it('rotula el examen según su visibilidad', async () => {
      await render(exam('PUBLISHED', [1, 2]));
      expect(el.querySelector('.badges')?.textContent).toContain('De prueba');

      fixture.componentRef.setInput('exam', { ...exam('PUBLISHED', [1, 2]), visibility: 'PRIVATE' });
      await stable();
      expect(el.querySelector('.badges')?.textContent).toContain('Oficial');
      expect(buttonTexts().join(' | ')).toContain('Pasar a examen de prueba (público)');
    });

    it('al publicar se elige entre examen de prueba u oficial', async () => {
      await render(exam('DRAFT'));

      expect(el.querySelector('.publish-options mat-select')?.textContent).toContain('De prueba (público)');
    });
  });

  describe('selector de preguntas del borrador', () => {
    it('recibe los tags y el objetivo de preguntas del examen', async () => {
      await render(exam('DRAFT'));
      fixture.componentRef.setInput('tags', [{ id: 9, name: 'Combate', color: '#C62828', createdAt: '2026-10-10T10:00:00' }]);
      await stable();

      expect(el.querySelector('.cart-target')?.textContent).toContain('objetivo 10');
      expect(el.querySelector('app-exam-question-picker')?.textContent).toContain('Combate');
    });

    it('una pregunta creada desde el selector se avisa al padre y queda seleccionada', async () => {
      await render(exam('DRAFT'));
      const createdQuestions: QuestionResponse[] = [];
      fixture.componentInstance.questionCreated.subscribe(q => createdQuestions.push(q));
      const picker = fixture.debugElement.query(By.directive(ExamQuestionPickerComponent)).componentInstance as ExamQuestionPickerComponent;

      picker.questionCreated.emit(question(7));
      picker.selectedIds.update(ids => [...ids, 7]);
      await stable();

      expect(createdQuestions.map(q => q.id)).toEqual([7]);
      expect(fixture.componentInstance.selectedIds()).toEqual([7]);
    });
  });

  describe('preguntas tal como se publicaron (DE-03)', () => {
    it('publicado: lista la copia publicada, no el banco', async () => {
      publishedFor = () => [published(2, 'Así se publicó la 2'), published(1, 'Así se publicó la 1')];
      await render(exam('PUBLISHED', [2, 1]));

      expect(Array.from(el.querySelectorAll('.assigned-list li')).map(li => li.textContent?.trim()))
        .toEqual(['Así se publicó la 2', 'Así se publicó la 1']);
      expect(el.textContent).toContain('Así las ven los alumnos');
    });

    it('avisa de las preguntas editadas en el banco después de publicar', async () => {
      publishedFor = () => [published(2, 'Pregunta 2', true), published(1)];
      await render(exam('EXPIRED', [2, 1]));

      const items = Array.from(el.querySelectorAll('.assigned-list li'));
      expect(items[0].textContent).toContain('editada en el banco después de publicar');
      expect(items[1].textContent).not.toContain('editada');
    });

    it('un borrador no pide preguntas publicadas', async () => {
      await render(exam('DRAFT', [1, 2]));

      expect(publishedRequests).toEqual([]);
    });

    it('pide las del examen que se muestra', async () => {
      await render(exam('PUBLISHED', [1]));

      expect(publishedRequests).toEqual([`${API}/exams/7/published-questions`]);
    });
  });

  it('publicado: muestra el código y la lista de preguntas sin selector', async () => {
    await render(exam('PUBLISHED', [2, 1]));

    expect(el.textContent).toContain('EXM-1234ABCD');
    expect(el.querySelector('app-exam-question-picker')).toBeNull();
    expect(Array.from(el.querySelectorAll('.assigned-list li')).map(li => li.textContent?.trim()))
      .toEqual(['Pregunta 2', 'Pregunta 1']);
  });

  describe('QR generado en el navegador (DE-14, DE-18)', () => {
    it('codifica el enlace completo al examen sin enviarlo a ningún servicio externo', async () => {
      await render(exam('PUBLISHED', [1, 2]));
      button('qr_code').click();
      await stable();

      const src = (el.querySelector('.qr-image') as HTMLImageElement).src;
      expect(qr.toDataUrl).toHaveBeenCalledWith(`${window.location.origin}/exam/EXM-1234ABCD`, expect.any(Number), '#000000');
      expect(src).toMatch(/^data:image\/png;base64,/);
    });

    it('no genera el QR hasta que se muestra', async () => {
      await render(exam('PUBLISHED', [1, 2]));

      expect(qr.toDataUrl).not.toHaveBeenCalled();
    });

    it('si no se puede generar, lo dice', async () => {
      await render(exam('PUBLISHED', [1, 2]));
      qr.toDataUrl.mockRejectedValue(new Error('sin canvas'));
      button('qr_code').click();
      await stable();

      expect(el.querySelector('.qr-image')).toBeNull();
      expect(el.querySelector('.qr-section')?.textContent).toContain('No se pudo generar el QR');
    });

    it('«Proyectar» abre el QR grande con el título y el código', async () => {
      await render(exam('PUBLISHED', [1, 2]));

      button('Proyectar').click();
      await stable();

      const dialog = document.querySelector('app-exam-qr-dialog') as HTMLElement;
      expect(dialog.textContent).toContain('Examen');
      expect(dialog.textContent).toContain('EXM-1234ABCD');
      expect(qr.toDataUrl).toHaveBeenCalledWith(`${window.location.origin}/exam/EXM-1234ABCD`, 1024, '#000000');
    });

    it('el QR del panel usa el color de este examen (RF-50)', async () => {
      await render(exam('PUBLISHED', [1, 2]));
      TestBed.inject(QrColorService).setColor('EXM-1234ABCD', '#0d47a1');
      button('qr_code').click();
      await stable();

      expect(qr.toDataUrl).toHaveBeenLastCalledWith(`${window.location.origin}/exam/EXM-1234ABCD`, expect.any(Number), '#0d47a1');
    });

    it('al publicar, el QR recibe un color que no usan los otros exámenes abiertos', async () => {
      await render(exam('DRAFT', [1, 2, 3, 4, 5]));
      const colors = TestBed.inject(QrColorService);
      colors.setColor('EXM-OTRO0001', '#c62828');
      fixture.componentRef.setInput('openExamCodes', ['EXM-OTRO0001', 'EXM-OTRO0002']);

      button('Publicar examen').click();
      http.expectOne(`${API}/exams/7/publish`).flush(exam('PUBLISHED', [1, 2, 3, 4, 5]));
      await stable();

      expect(colors.colorOf('EXM-1234ABCD')).toBe('#0d47a1');
    });

    it('un borrador no tiene QR ni botón de proyectar', async () => {
      await render(exam('DRAFT'));

      expect(button('Proyectar')).toBeUndefined();
      expect(button('qr_code')).toBeUndefined();
    });
  });

  describe('copiar el enlace y avisos del QR (DE-20)', () => {
    function stubClipboard(clipboard: Partial<Clipboard> | undefined): void {
      Object.defineProperty(navigator, 'clipboard', { value: clipboard, configurable: true });
    }

    afterEach(() => stubClipboard(undefined));

    it('avisa de que el enlace se ha copiado', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      stubClipboard({ writeText });
      await render(exam('PUBLISHED', [1, 2]));

      button('link').click();
      await stable();
      await stable();

      expect(writeText).toHaveBeenCalledWith(`${window.location.origin}/exam/EXM-1234ABCD`);
      expect(document.body.textContent).toContain('Enlace copiado');
    });

    it('sin portapapeles avisa y muestra el enlace para copiarlo a mano', async () => {
      stubClipboard(undefined);
      await render(exam('PUBLISHED', [1, 2]));

      button('link').click();
      await stable();

      expect(document.body.textContent).toContain('No se pudo copiar el enlace');
      expect(el.querySelector('.qr-url')?.textContent).toContain(`${window.location.origin}/exam/EXM-1234ABCD`);
    });

    it('si el portapapeles falla, también avisa', async () => {
      stubClipboard({ writeText: vi.fn().mockRejectedValue(new Error('denegado')) });
      await render(exam('PUBLISHED', [1, 2]));

      button('link').click();
      await stable();
      await stable();

      expect(document.body.textContent).toContain('No se pudo copiar el enlace');
    });

    it('un examen cerrado avisa junto al QR de que no admite entregas', async () => {
      await render(exam('EXPIRED', [1, 2]));
      button('qr_code').click();
      await stable();

      expect(el.querySelector('.qr-closed')?.textContent).toContain('no admite entregas');
    });

    it('un examen que no admite entregas no se puede proyectar', async () => {
      await render(exam('EXPIRED', [1, 2]));

      expect(button('Proyectar').disabled).toBe(true);
      button('qr_code').click();
      await stable();
      expect(button('Proyectar').disabled).toBe(true);
    });

    it('fuera de plazo tampoco', async () => {
      await render({ ...exam('PUBLISHED', [1, 2]), expiresAt: '2020-01-01T10:00:00' });

      expect(button('Proyectar').disabled).toBe(true);
    });

    it('un examen publicado y en plazo no muestra ese aviso', async () => {
      await render(exam('PUBLISHED', [1, 2]));
      button('qr_code').click();
      await stable();

      expect(el.querySelector('.qr-closed')).toBeNull();
    });
  });

  describe('al cambiar a otro examen (DE-15)', () => {
    function other(status: ExamStatus): ExamResponse {
      return { ...exam(status, [1, 2]), id: 8, title: 'Otro examen', code: status === 'DRAFT' ? null : 'EXM-8888AAAA' };
    }

    it('cierra la edición de configuración: guardar ya no puede tocar el examen nuevo', async () => {
      await render(exam('DRAFT'));
      button('Editar').click();
      await stable();
      expect(button('Guardar cambios')).toBeTruthy();

      fixture.componentRef.setInput('exam', other('DRAFT'));
      await stable();

      expect(button('Guardar cambios')).toBeUndefined();
      expect(el.querySelector('.config-summary')).not.toBeNull();
    });

    it('oculta el QR del examen anterior', async () => {
      await render(exam('PUBLISHED', [1, 2]));
      button('qr_code').click();
      await stable();
      expect(el.querySelector('.qr-image')).not.toBeNull();

      fixture.componentRef.setInput('exam', other('PUBLISHED'));
      await stable();

      expect(el.querySelector('.qr-image')).toBeNull();
    });

    it('borra el error y el plazo escrito para el examen anterior', async () => {
      await render(exam('PUBLISHED', [1, 2]));
      const deadline = el.querySelector('.inline-form input') as HTMLInputElement;
      deadline.value = '2026-12-31T10:00';
      deadline.dispatchEvent(new Event('input'));
      button('Cambiar plazo').click();
      http.expectOne(`${API}/exams/7/expiration`)
        .flush({ message: 'Fecha no válida' }, { status: 400, statusText: 'Bad Request' });
      await stable();
      expect(el.textContent).toContain('Fecha no válida');

      fixture.componentRef.setInput('exam', other('PUBLISHED'));
      await stable();

      expect(el.textContent).not.toContain('Fecha no válida');
      expect((el.querySelector('.inline-form input') as HTMLInputElement).value).toBe('');
    });
  });

  describe('hacer una copia (RF-18)', () => {
    it('crea un borrador nuevo a partir del examen y se lo pasa al padre', async () => {
      await render(exam('PUBLISHED', [1, 2]));
      const copies: ExamResponse[] = [];
      fixture.componentInstance.copied.subscribe(e => copies.push(e));

      button('Hacer una copia').click();
      http.expectOne(`${API}/exams/7/copy`).flush({ ...exam('DRAFT', [1, 2]), id: 8, title: 'Copia de Examen' });
      await stable();

      expect(copies.map(c => c.id)).toEqual([8]);
      expect(document.body.textContent).toContain('Copia creada como borrador');
    });

    it('también se puede copiar un borrador', async () => {
      await render(exam('DRAFT'));

      expect(button('Hacer una copia')).toBeTruthy();
    });
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
