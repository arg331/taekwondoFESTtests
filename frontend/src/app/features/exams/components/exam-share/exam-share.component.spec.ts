import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExamShareComponent } from './exam-share.component';
import { ExamShareResponse } from '../../../../core/models/share.models';
import { ConfirmService } from '../../../../core/services/confirm.service';
import { of } from 'rxjs';

const API = 'http://localhost:8080/api';

const ANA: ExamShareResponse = { teacherId: 8, username: 'ana', displayName: 'Ana Pérez', status: 'ACCEPTED', createdAt: '2026-10-21T10:00:00' };
const LUIS: ExamShareResponse = { teacherId: 9, username: 'luis', displayName: 'Luis Gil', status: 'PENDING', createdAt: '2026-10-21T11:00:00' };

/** El dueño comparte su examen con otros profesores (RF-51). */
describe('ExamShareComponent', () => {
  let fixture: ComponentFixture<ExamShareComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function render(shares: ExamShareResponse[] = [ANA, LUIS]): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ExamShareComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExamShareComponent);
    fixture.componentRef.setInput('examId', 7);
    el = fixture.nativeElement;
    // El resource tiene la app «ocupada» hasta que llega la respuesta: se responde antes de esperar
    fixture.detectChanges();
    http.expectOne(`${API}/exams/7/shares`).flush(shares);
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function rows(): HTMLElement[] {
    return Array.from(el.querySelectorAll('.share-row'));
  }

  function button(text: string, root: ParentNode = el): HTMLButtonElement {
    return Array.from(root.querySelectorAll('button')).find(b => b.textContent?.includes(text)) as HTMLButtonElement;
  }

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  it('lista con quién se comparte y si ha aceptado ya', async () => {
    await render();

    expect(rows().map(r => r.querySelector('.share-name')?.textContent?.trim())).toEqual(['Ana Pérez', 'Luis Gil']);
    expect(rows().map(r => r.querySelector('.share-status')?.textContent?.trim())).toEqual(['Aceptada', 'Pendiente']);
    expect(button('Quitar', rows()[0])).toBeTruthy();
    expect(button('Cancelar invitación', rows()[1])).toBeTruthy();
    expect(el.textContent).toContain('verán sus resultados con los datos de los alumnos');
  });

  it('busca profesores e invita al elegido', async () => {
    await render([]);
    const input = el.querySelector('input') as HTMLInputElement;
    input.value = 'lu';
    input.dispatchEvent(new Event('input'));
    button('Buscar').click();
    http.expectOne(`${API}/exams/7/shares/candidates?query=lu`).flush([{ id: 9, username: 'luis', displayName: 'Luis Gil' }]);
    await stable();

    button('Invitar').click();
    const req = http.expectOne(`${API}/exams/7/shares`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ teacherId: 9 });
    req.flush(LUIS);
    await stable();

    expect(rows().map(r => r.querySelector('.share-name')?.textContent?.trim())).toEqual(['Luis Gil']);
    expect(el.querySelector('.candidate')).toBeNull();
  });

  it('retira el acceso tras confirmarlo', async () => {
    vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
    await render();

    button('Quitar', rows()[0]).click();
    const req = http.expectOne(`${API}/exams/7/shares/8`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    await stable();

    expect(rows().map(r => r.querySelector('.share-name')?.textContent?.trim())).toEqual(['Luis Gil']);
  });

  it('si el servidor lo rechaza, muestra su mensaje', async () => {
    await render([]);
    const input = el.querySelector('input') as HTMLInputElement;
    input.value = 'lu';
    input.dispatchEvent(new Event('input'));
    button('Buscar').click();
    http.expectOne(`${API}/exams/7/shares/candidates?query=lu`).flush([{ id: 9, username: 'luis', displayName: 'Luis Gil' }]);
    await stable();

    button('Invitar').click();
    http.expectOne(`${API}/exams/7/shares`)
      .flush({ message: 'Ya has compartido este examen con ese profesor' }, { status: 409, statusText: 'Conflict' });
    await stable();

    expect(el.querySelector('.share-error')?.textContent).toContain('Ya has compartido este examen con ese profesor');
  });
});
