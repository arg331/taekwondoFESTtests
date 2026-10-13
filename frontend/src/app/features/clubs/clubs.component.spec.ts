import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ClubsComponent } from './clubs.component';
import { AuthService } from '../../core/services/auth.service';
import { UserResponse, UserRole } from '../../core/models/auth.models';
import { ClubResponse } from '../../core/models/club.models';

const API = 'http://localhost:8080/api';
const AVILA: ClubResponse = { id: 1, name: 'Club Ávila' };
const BADAJOZ: ClubResponse = { id: 2, name: 'Club Badajoz' };

function me(role: UserRole, club: ClubResponse | null): UserResponse {
  return { id: 7, username: 'yo', email: 'yo@x.es', displayName: 'Yo', role, club, active: true, createdAt: '2026-10-13T10:00:00' };
}

/** Página «Clubes» para profesores y administrador (RF-38, RF-40, RF-42). */
describe('ClubsComponent', () => {
  let fixture: ComponentFixture<ClubsComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function render(user: UserResponse, clubs: ClubResponse[]): Promise<void> {
    localStorage.setItem('fest_token', 't');
    localStorage.setItem('fest_user', JSON.stringify(user));
    TestBed.configureTestingModule({
      imports: [ClubsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ClubsComponent);
    el = fixture.nativeElement;
    // Los miembros del club abierto se cargan con un resource, que tiene la app «ocupada» hasta que
    // llega la respuesta: cada prueba responde a esa carga (si la hay) antes de esperar con stable()
    fixture.detectChanges();
    http.expectOne(`${API}/clubs`).flush(clubs);
    fixture.detectChanges();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function button(text: string): HTMLButtonElement | undefined {
    return Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes(text)) as HTMLButtonElement | undefined;
  }

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('lista los clubes y marca el del profesor, que se abre al entrar', async () => {
    await render(me('TEACHER', BADAJOZ), [AVILA, BADAJOZ]);
    http.expectOne(`${API}/clubs/2/members`).flush([]);
    await stable();

    expect(Array.from(el.querySelectorAll('.club-item .club-name')).map(n => n.textContent?.trim()))
      .toEqual(['Club Ávila', 'Club Badajoz']);
    expect(Array.from(el.querySelectorAll('.club-item')).map(i => !!i.querySelector('.mine'))).toEqual([false, true]);
    expect(el.querySelector('app-club-detail h2')?.textContent).toContain('Club Badajoz');
  });

  it('un profesor que ya tiene club no ve el formulario para crear otro', async () => {
    await render(me('TEACHER', BADAJOZ), [BADAJOZ]);
    http.expectOne(`${API}/clubs/2/members`).flush([]);
    await stable();

    expect(el.querySelector('.create-club')).toBeNull();
  });

  it('un profesor sin club lo crea, entra en él y queda abierto', async () => {
    await render(me('TEACHER', null), [AVILA]);
    await stable();
    const input = el.querySelector('.create-club input') as HTMLInputElement;
    input.value = 'Club Cáceres';
    input.dispatchEvent(new Event('input'));
    await stable();

    button('Crear club')!.click();
    const req = http.expectOne(`${API}/clubs`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Club Cáceres' });
    req.flush({ id: 3, name: 'Club Cáceres' });
    fixture.detectChanges();
    http.expectOne(`${API}/clubs/3/members`).flush([]);
    await stable();

    expect(TestBed.inject(AuthService).currentUser()?.club).toEqual({ id: 3, name: 'Club Cáceres' });
    expect(el.querySelector('.create-club')).toBeNull();
    expect(el.querySelector('app-club-detail h2')?.textContent).toContain('Club Cáceres');
  });

  it('el administrador crea clubes sin entrar en ellos y no abre ninguno al principio', async () => {
    await render(me('ADMIN', null), [AVILA]);
    await stable();
    expect(el.querySelector('app-club-detail')).toBeNull();

    const input = el.querySelector('.create-club input') as HTMLInputElement;
    input.value = 'Club Cáceres';
    input.dispatchEvent(new Event('input'));
    await stable();
    button('Crear club')!.click();
    http.expectOne(`${API}/clubs`).flush({ id: 3, name: 'Club Cáceres' });
    fixture.detectChanges();
    http.expectOne(`${API}/clubs/3/members`).flush([]);
    await stable();

    expect(TestBed.inject(AuthService).currentUser()?.club).toBeNull();
    expect(el.querySelector('.create-club')).not.toBeNull();
  });

  it('un nombre repetido muestra el error del servidor', async () => {
    await render(me('ADMIN', null), [AVILA]);
    await stable();
    const input = el.querySelector('.create-club input') as HTMLInputElement;
    input.value = 'club ávila';
    input.dispatchEvent(new Event('input'));
    await stable();

    button('Crear club')!.click();
    http.expectOne(`${API}/clubs`).flush({ message: 'Ya existe un club llamado «club ávila»' }, { status: 409, statusText: 'Conflict' });
    await stable();

    expect(el.textContent).toContain('Ya existe un club llamado «club ávila»');
  });
});
