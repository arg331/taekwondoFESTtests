import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TagManagerComponent } from './tag-manager.component';
import { TagResponse } from '../../../../core/models/tag.models';
import { ConfirmService } from '../../../../core/services/confirm.service';
import { of } from 'rxjs';

const API = 'http://localhost:8080/api';
const COMBATE: TagResponse = { id: 1, name: 'Combate', color: '#C62828', createdAt: '2026-10-10T10:00:00' };

/** Gestión de tags: crear y renombrar (RF-07), cambiar el color y borrar (RF-08). */
describe('TagManagerComponent', () => {
  let fixture: ComponentFixture<TagManagerComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;
  let updated: TagResponse[];
  let removed: number[];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [TagManagerComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TagManagerComponent);
    fixture.componentRef.setInput('tags', [COMBATE]);
    updated = [];
    removed = [];
    fixture.componentInstance.updated.subscribe(tag => updated.push(tag));
    fixture.componentInstance.removed.subscribe(id => removed.push(id));
    el = fixture.nativeElement;
    await stable();
  });

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function renameButton(): HTMLButtonElement {
    return el.querySelector('.tag-item .tag-rename') as HTMLButtonElement;
  }

  function button(text: string): HTMLButtonElement {
    return Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes(text)) as HTMLButtonElement;
  }

  function type(selector: string, value: string): void {
    const input = el.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('el botón de editar va pegado a su tag, con su color', () => {
    const chip = el.querySelector('.tag-item .tag-chip') as HTMLElement;

    expect(chip.nextElementSibling).toBe(renameButton());
    expect(renameButton().getAttribute('aria-label')).toBe('Editar Combate');
    expect(renameButton().style.background).toBe(chip.style.background);
  });

  it('renombrar abre el formulario con el nombre y guarda el nuevo', async () => {
    renameButton().click();
    await stable();
    const input = el.querySelector('input[formcontrolname="name"]') as HTMLInputElement;
    expect(input.value).toBe('Combate');

    input.value = 'Combate y reglas';
    input.dispatchEvent(new Event('input'));
    await stable();
    button('Guardar').click();

    const req = http.expectOne(`${API}/tags/1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ newName: 'Combate y reglas' });
    req.flush({ ...COMBATE, name: 'Combate y reglas' });
    await stable();

    expect(el.querySelector('.tag-chip')?.textContent?.trim()).toBe('Combate y reglas');
    expect(updated.map(tag => tag.name)).toEqual(['Combate y reglas']);
  });

  it('al editar se puede cambiar solo el color (RF-08)', async () => {
    renameButton().click();
    await stable();
    type('input[formcontrolname="color"]', '#1565c0');
    await stable();
    button('Guardar').click();

    const req = http.expectOne(`${API}/tags/1/color`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ newColor: '#1565c0' });
    req.flush({ ...COMBATE, color: '#1565c0' });
    await stable();

    expect((el.querySelector('.tag-chip') as HTMLElement).style.background).toBe('rgb(21, 101, 192)');
    expect(updated.map(tag => tag.color)).toEqual(['#1565c0']);
  });

  it('nombre y color a la vez: renombra y después cambia el color', async () => {
    renameButton().click();
    await stable();
    type('input[formcontrolname="name"]', 'Reglas');
    type('input[formcontrolname="color"]', '#1565c0');
    await stable();
    button('Guardar').click();

    http.expectOne(r => r.url === `${API}/tags/1` && r.method === 'PATCH').flush({ ...COMBATE, name: 'Reglas' });
    http.expectOne(`${API}/tags/1/color`).flush({ ...COMBATE, name: 'Reglas', color: '#1565c0' });
    await stable();

    expect(updated.at(-1)).toEqual({ ...COMBATE, name: 'Reglas', color: '#1565c0' });
  });

  it('borrar pide confirmación, lo quita de la lista y lo avisa (RF-08)', async () => {
    const confirm = vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(true));
    renameButton().click();
    await stable();

    button('Borrar tag').click();
    expect(confirm.mock.calls[0][0].message).toContain('Combate');
    const req = http.expectOne(`${API}/tags/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    await stable();

    expect(el.querySelector('.tag-chip')).toBeNull();
    expect(removed).toEqual([1]);
  });

  it('si no se confirma, no se borra', async () => {
    vi.spyOn(ConfirmService.prototype, 'ask').mockReturnValue(of(false));
    renameButton().click();
    await stable();

    button('Borrar tag').click();

    http.expectNone(`${API}/tags/1`);
    expect(el.querySelector('.tag-chip')).not.toBeNull();
  });
});
