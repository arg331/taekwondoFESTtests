import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { UsersComponent } from './users.component';
import { UserResponse, UserRole } from '../../core/models/auth.models';

const API = 'http://localhost:8080/api';

function user(id: number, role: UserRole, displayName: string): UserResponse {
  return {
    id, username: `u${id}`, email: `u${id}@x.es`, displayName, role, active: true, createdAt: '2026-10-13T10:00:00'
  };
}

const ME = user(1, 'ADMIN', 'Administrador');
const ANA = user(2, 'STUDENT', 'Ana');
const LUIS = user(3, 'TEACHER', 'Luis');

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
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(UsersComponent);
    el = fixture.nativeElement;
    loader = TestbedHarnessEnvironment.loader(fixture);
    await stable();
    http.expectOne(`${API}/users`).flush([ME, ANA, LUIS]);
    await stable();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
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
});
