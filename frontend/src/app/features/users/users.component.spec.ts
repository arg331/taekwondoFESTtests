import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { UsersComponent } from './users.component';
import { UserResponse, UserRole } from '../../core/models/auth.models';
import { PageResponse } from '../../core/models/page.models';

const API = 'http://localhost:8080/api';

function user(id: number, role: UserRole, displayName: string): UserResponse {
  return {
    id, username: `u${id}`, email: `u${id}@x.es`, displayName, role, club: null, active: true, mustChangePassword: false, createdAt: '2026-10-13T10:00:00'
  };
}

const ME = user(1, 'ADMIN', 'Administrador');
const ANA = user(2, 'STUDENT', 'Ana');
const LUIS = user(3, 'TEACHER', 'Luis');

function page(items: UserResponse[], totalItems = items.length, pageIndex = 0): PageResponse<UserResponse> {
  return { items, page: pageIndex, size: 20, totalItems, totalPages: Math.ceil(totalItems / 20) };
}

/** Gestión de usuarios: el administrador cambia el rol de los demás (RF-36). */
describe('UsersComponent', () => {
  let fixture: ComponentFixture<UsersComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;
  let loader: HarnessLoader;

  beforeEach(async () => {
    localStorage.setItem('fest_token', 't');
    localStorage.setItem('fest_user', JSON.stringify(ME));
    TestBed.configureTestingModule({
      imports: [UsersComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        // Sin animaciones, el diálogo avisa de que se ha cerrado en cuanto se cierra
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } }
      ]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(UsersComponent);
    el = fixture.nativeElement;
    loader = TestbedHarnessEnvironment.loader(fixture);
    // El resource espera su petición: se responde antes de esperar a que se estabilice
    fixture.detectChanges();
    usersRequest('', 0).flush(page([ME, ANA, LUIS]));
    await stable();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  /** La página que pide la lista: 20 usuarios, con lo buscado (RNF-09). */
  function usersRequest(q: string, pageIndex: number) {
    return http.expectOne(r => r.url === `${API}/users` && r.params.get('q') === q &&
      r.params.get('page') === String(pageIndex) && r.params.get('size') === '20');
  }

  function names(): string[] {
    return Array.from(el.querySelectorAll('.user-card')).map(c => c.querySelector('.user-name')?.textContent?.trim() ?? '');
  }

  /** Escribe en el buscador y espera a que se deje de escribir. */
  async function type(text: string): Promise<void> {
    const input = el.querySelector('.user-search input') as HTMLInputElement;
    input.value = text;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await new Promise(resolve => setTimeout(resolve, 350));
    fixture.detectChanges();
  }

  describe('buscar (RF-37: si alguien olvida su usuario, se busca por su email)', () => {
    it('busca en el servidor al dejar de escribir y enseña cuántos coinciden', async () => {
      expect(el.querySelector('.subtitle')?.textContent).toContain('3 usuarios registrados');

      await type('  luis ');
      usersRequest('luis', 0).flush(page([LUIS]));
      await stable();

      expect(names()).toEqual(['Luis']);
      expect(el.querySelector('.subtitle')?.textContent).toContain('1 coincide con «luis»');
    });

    it('sin resultados lo dice', async () => {
      await type('nadie');
      usersRequest('nadie', 0).flush(page([]));
      await stable();

      expect(el.textContent).toContain('Ningún usuario coincide con «nadie».');
    });
  });

  describe('por páginas de 20 (RNF-09)', () => {
    it('con una sola página no hay paginador', () => {
      expect(el.querySelector('mat-paginator')).toBeNull();
    });

    it('con más, el paginador pide la siguiente; buscar vuelve a la primera', async () => {
      await type('alumno');
      usersRequest('alumno', 0).flush(page([ANA], 45));
      await stable();

      const paginator = await loader.getHarness(MatPaginatorHarness);
      expect(await paginator.getRangeLabel()).toBe('1 – 20 de 45');
      (el.querySelector('.mat-mdc-paginator-navigation-next') as HTMLButtonElement).click();
      fixture.detectChanges();
      usersRequest('alumno', 1).flush(page([LUIS], 45, 1));
      await stable();
      expect(names()).toEqual(['Luis']);

      await type('alumna');
      usersRequest('alumna', 0).flush(page([ANA]));
      await stable();
      expect(names()).toEqual(['Ana']);
    });
  });

  it('cada usuario lleva a sus resultados (RF-55)', () => {
    const link = el.querySelector('[data-id="2"] a.user-results') as HTMLAnchorElement;

    expect(link.getAttribute('href')).toBe('/results/user/2');
    expect(link.textContent).toContain('Ver resultados');
  });

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function row(name: string): HTMLElement {
    return Array.from(el.querySelectorAll('.user-card')).find(c => c.textContent?.includes(name)) as HTMLElement;
  }

  async function roleSelect(name: string): Promise<MatSelectHarness> {
    return loader.getHarness(MatSelectHarness.with({ ancestor: `.user-card[data-id="${name}"]` }));
  }

  it('muestra el rol de cada usuario con su nombre (RF-04)', async () => {
    expect(await (await roleSelect('2')).getValueText()).toBe('Alumno');
    expect(await (await roleSelect('3')).getValueText()).toBe('Profesor');
  });

  it('el propio rol no se puede cambiar: se muestra sin desplegable', () => {
    expect(row('Administrador').querySelector('mat-select')).toBeNull();
    expect(row('Administrador').textContent).toContain('Administrador');
  });

  it('cambiar el rol lo guarda y actualiza la fila', async () => {
    await (await roleSelect('2')).clickOptions({ text: 'Profesor' });

    const req = http.expectOne(`${API}/users/2/role`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ role: 'TEACHER' });
    req.flush({ ...ANA, role: 'TEACHER' });
    await stable();

    expect(await (await roleSelect('2')).getValueText()).toBe('Profesor');
  });

  it('si el servidor lo rechaza, vuelve al rol anterior y lo dice', async () => {
    await (await roleSelect('3')).clickOptions({ text: 'Alumno' });

    http.expectOne(`${API}/users/3/role`).flush({ message: 'No se puede' }, { status: 409, statusText: 'Conflict' });
    await stable();

    expect(await (await roleSelect('3')).getValueText()).toBe('Profesor');
    expect(document.body.textContent).toContain('No se puede');
  });

  describe('desactivar y reactivar (RF-05)', () => {
    function activeButton(name: string): HTMLButtonElement | null {
      return row(name).querySelector('button.toggle-active');
    }

    it('la propia cuenta no se desactiva desde aquí', () => {
      expect(activeButton('Administrador')).toBeNull();
      expect(activeButton('Ana')!.textContent).toContain('Desactivar');
    });

    it('desactivar lo guarda, marca la fila y deja reactivarla', async () => {
      activeButton('Ana')!.click();
      const req = http.expectOne(`${API}/users/2/active`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ active: false });
      req.flush({ ...ANA, active: false });
      await stable();

      expect(row('Ana').textContent).toContain('Desactivada');
      expect(row('Ana').classList).toContain('inactive');
      expect(activeButton('Ana')!.textContent).toContain('Reactivar');

      activeButton('Ana')!.click();
      const again = http.expectOne(`${API}/users/2/active`);
      expect(again.request.body).toEqual({ active: true });
      again.flush(ANA);
      await stable();

      expect(row('Ana').textContent).not.toContain('Desactivada');
    });

    it('si el servidor lo rechaza, la fila no cambia y lo dice', async () => {
      activeButton('Luis')!.click();
      http.expectOne(`${API}/users/3/active`).flush({ message: 'Usuario no encontrado con id 3' },
        { status: 404, statusText: 'Not Found' });
      await stable();

      expect(row('Luis').textContent).not.toContain('Desactivada');
      expect(document.body.textContent).toContain('Usuario no encontrado con id 3');
    });
  });

  describe('restablecer contraseña (RF-35)', () => {
    function resetButton(name: string): HTMLButtonElement | null {
      return row(name).querySelector('button.reset-password');
    }

    function dialog(): HTMLElement | null {
      return document.querySelector('app-reset-password-dialog');
    }

    async function openAndType(name: string, password: string): Promise<void> {
      resetButton(name)!.click();
      await stable();
      const input = dialog()!.querySelector('input') as HTMLInputElement;
      input.value = password;
      input.dispatchEvent(new Event('input'));
      await stable();
      (Array.from(dialog()!.querySelectorAll('button')).find(b => b.textContent?.includes('Restablecer')) as HTMLButtonElement).click();
      await stable();
    }

    it('la propia contraseña no se restablece desde aquí', () => {
      expect(resetButton('Administrador')).toBeNull();
      expect(resetButton('Ana')).not.toBeNull();
    });

    it('guarda la temporal, lo dice y marca la fila', async () => {
      await openAndType('Ana', 'temporal1');

      const req = http.expectOne(`${API}/users/2/password`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ temporaryPassword: 'temporal1' });
      req.flush({ ...ANA, mustChangePassword: true });
      await stable();

      expect(row('Ana').textContent).toContain('Contraseña temporal');
      expect(row('Luis').textContent).not.toContain('Contraseña temporal');
      expect(document.body.textContent).toContain('Ana tendrá que cambiarla al entrar');
    });

    it('una temporal de menos de 6 caracteres no se envía', async () => {
      await openAndType('Ana', '12345');

      http.expectNone(`${API}/users/2/password`);
      expect(dialog()).not.toBeNull();
    });

    it('si el servidor la rechaza, el diálogo sigue abierto con el mensaje', async () => {
      await openAndType('Luis', 'temporal1');

      http.expectOne(`${API}/users/3/password`).flush({ message: 'Usuario no encontrado con id 3' },
        { status: 404, statusText: 'Not Found' });
      await stable();

      expect(dialog()!.textContent).toContain('Usuario no encontrado con id 3');
      expect(row('Luis').textContent).not.toContain('Contraseña temporal');
    });
  });
});
