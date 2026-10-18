import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TagManagerComponent } from './tag-manager.component';
import { TagResponse } from '../../../../core/models/tag.models';

const API = 'http://localhost:8080/api';
const COMBATE: TagResponse = { id: 1, name: 'Combate', color: '#C62828', createdAt: '2026-10-10T10:00:00' };

/** Gestión de tags (RF-07): crear y renombrar. */
describe('TagManagerComponent', () => {
  let fixture: ComponentFixture<TagManagerComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [TagManagerComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TagManagerComponent);
    fixture.componentRef.setInput('tags', [COMBATE]);
    el = fixture.nativeElement;
    await stable();
  });

  afterEach(() => http.verify());

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function renameButton(): HTMLButtonElement {
    return el.querySelector('.tag-item .tag-rename') as HTMLButtonElement;
  }

  it('el botón de renombrar va pegado a su tag, con su color', () => {
    const chip = el.querySelector('.tag-item .tag-chip') as HTMLElement;

    expect(chip.nextElementSibling).toBe(renameButton());
    expect(renameButton().getAttribute('aria-label')).toBe('Renombrar Combate');
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
    (Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes('Renombrar') && b.type === 'submit') as HTMLButtonElement).click();

    const req = http.expectOne(`${API}/tags/1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ newName: 'Combate y reglas' });
    req.flush({ ...COMBATE, name: 'Combate y reglas' });
    await stable();

    expect(el.querySelector('.tag-chip')?.textContent?.trim()).toBe('Combate y reglas');
  });
});
