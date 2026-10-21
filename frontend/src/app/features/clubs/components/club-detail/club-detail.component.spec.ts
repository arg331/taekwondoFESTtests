import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ClubDetailComponent } from './club-detail.component';
import { ClubMemberResponse, ClubResponse } from '../../../../core/models/club.models';

const API = 'http://localhost:8080/api';
const CLUB: ClubResponse = { id: 2, name: 'Club Badajoz' };
const PROFE: ClubMemberResponse = { id: 7, username: 'profe', displayName: 'Profe', role: 'TEACHER' };
const ANA: ClubMemberResponse = { id: 8, username: 'ana', displayName: 'Ana', role: 'STUDENT' };

/** Detalle de un club: miembros, añadir, quitar, renombrar y borrar (RF-40, RF-42). */
describe('ClubDetailComponent', () => {
  let fixture: ComponentFixture<ClubDetailComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function render(canManage: boolean, isAdmin = false, members: ClubMemberResponse[] = [PROFE, ANA]): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ClubDetailComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ClubDetailComponent);
    fixture.componentRef.setInput('club', CLUB);
    fixture.componentRef.setInput('canManage', canManage);
    fixture.componentRef.setInput('isAdmin', isAdmin);
    el = fixture.nativeElement;
    // Los miembros se cargan con un resource, que tiene la app «ocupada» hasta que llega la respuesta:
    // hay que responder antes de esperar a que se estabilice
    fixture.detectChanges();
    if (canManage) {
      http.expectOne(`${API}/clubs/2/members`).flush(members);
    }
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function button(text: string, root: ParentNode = el): HTMLButtonElement | undefined {
    return Array.from(root.querySelectorAll('button')).find(b => b.textContent?.includes(text)) as HTMLButtonElement | undefined;
  }

  function memberNames(): string[] {
    return Array.from(el.querySelectorAll('.member-name')).map(m => m.textContent?.trim() ?? '');
  }

  it('el nombre de cada miembro lleva a sus resultados (RF-55)', async () => {
    await render(true);

    expect(Array.from(el.querySelectorAll('a.member-name')).map(a => a.getAttribute('href')))
      .toEqual(['/results/user/7', '/results/user/8']);
  });

  function type(selector: string, value: string): void {
    const input = el.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  it('quien no gestiona el club no ve sus miembros', async () => {
    await render(false);

    expect(el.querySelector('.members')).toBeNull();
    expect(el.textContent).toContain('Solo el administrador y los profesores del club');
  });

  it('muestra los miembros con su rol', async () => {
    await render(true);

    expect(memberNames()).toEqual(['Profe', 'Ana']);
    expect(el.querySelector('.members')?.textContent).toContain('Profesor');
    expect(el.querySelector('.members')?.textContent).toContain('Alumno');
  });

  it('busca candidatos y añade uno al club', async () => {
    await render(true, false, [PROFE]);

    type('.add-member input', 'an');
    await stable();
    button('Buscar')!.click();
    http.expectOne(`${API}/clubs/2/candidates?q=an`).flush([{ ...ANA, clubName: null }]);
    await stable();
    button('Añadir', el.querySelector('.candidates')!)!.click();
    http.expectOne(`${API}/clubs/2/members/8`).flush(ANA);
    await stable();

    expect(memberNames()).toEqual(['Profe', 'Ana']);
    expect(el.querySelector('.candidates')?.textContent ?? '').not.toContain('Ana');
  });

  it('al administrador le avisa de que el candidato cambiará de club', async () => {
    await render(true, true, [PROFE]);

    type('.add-member input', 'an');
    await stable();
    button('Buscar')!.click();
    http.expectOne(`${API}/clubs/2/candidates?q=an`).flush([{ ...ANA, clubName: 'Club Ávila' }]);
    await stable();

    expect(el.querySelector('.candidates')?.textContent).toContain('ahora en Club Ávila');
  });

  it('quita a un miembro tras confirmarlo', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await render(true);

    const anaRow = Array.from(el.querySelectorAll('.member')).find(m => m.textContent?.includes('Ana')) as HTMLElement;
    button('Quitar', anaRow)!.click();
    http.expectOne(`${API}/clubs/2/members/8`).flush(null);
    await stable();

    expect(memberNames()).toEqual(['Profe']);
  });

  it('renombra el club y se lo dice al padre', async () => {
    await render(true);
    const renamed: ClubResponse[] = [];
    fixture.componentInstance.renamed.subscribe(c => renamed.push(c));

    button('Renombrar')!.click();
    await stable();
    type('.rename-form input', 'Club Badajoz Centro');
    button('Guardar')!.click();
    const req = http.expectOne(`${API}/clubs/2`);
    expect(req.request.body).toEqual({ name: 'Club Badajoz Centro' });
    req.flush({ id: 2, name: 'Club Badajoz Centro' });
    await stable();

    expect(renamed).toEqual([{ id: 2, name: 'Club Badajoz Centro' }]);
  });

  it('solo el administrador ve «Borrar club»', async () => {
    await render(true, false);
    expect(button('Borrar club')).toBeUndefined();
  });

  it('el administrador borra el club tras confirmarlo', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await render(true, true);
    const deleted: number[] = [];
    fixture.componentInstance.deleted.subscribe(id => deleted.push(id));

    button('Borrar club')!.click();
    http.expectOne(`${API}/clubs/2`).flush(null);
    await stable();

    expect(deleted).toEqual([2]);
  });
});
