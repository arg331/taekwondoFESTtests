import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { ChangePasswordComponent } from './change-password.component';
import { AuthService } from '../../../core/services/auth.service';
import { UserResponse } from '../../../core/models/auth.models';

const API = 'http://localhost:8080/api';
const ME: UserResponse = {
  id: 5, username: 'ana', email: null, displayName: 'Ana', role: 'STUDENT', club: null, active: true,
  mustChangePassword: false, createdAt: '2026-10-14T10:00:00'
};

/** Cambiar la propia contraseña (RF-06) y la temporal obligatoria (RF-33). */
describe('ChangePasswordComponent', () => {
  let fixture: ComponentFixture<ChangePasswordComponent>;
  let http: HttpTestingController;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;
  let el: HTMLElement;

  async function render(options: { temporary: boolean; returnUrl?: string }): Promise<void> {
    localStorage.setItem('fest_token', 'viejo');
    localStorage.setItem('fest_user', JSON.stringify({ ...ME, mustChangePassword: options.temporary }));
    TestBed.configureTestingModule({
      imports: [ChangePasswordComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    const params = options.returnUrl ? { returnUrl: options.returnUrl } : {};
    TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { queryParamMap: convertToParamMap(params) } } });
    http = TestBed.inject(HttpTestingController);
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(ChangePasswordComponent);
    el = fixture.nativeElement;
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function fill(current: string, password: string, repeat: string): Promise<void> {
    const inputs = el.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    [current, password, repeat].forEach((value, i) => {
      inputs[i].value = value;
      inputs[i].dispatchEvent(new Event('input'));
      inputs[i].dispatchEvent(new Event('blur'));
    });
    await stable();
  }

  function button(text: string): HTMLButtonElement | undefined {
    return Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes(text));
  }

  async function submit(): Promise<void> {
    (el.querySelector('button[type=submit]') as HTMLButtonElement).click();
    await stable();
  }

  afterEach(() => {
    http.verify();
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('cambio voluntario: envía la actual y la nueva, guarda la sesión nueva y vuelve', async () => {
    await render({ temporary: false, returnUrl: '/results' });
    await fill('actual12', 'nueva123', 'nueva123');
    await submit();

    const req = http.expectOne(`${API}/auth/me/password`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ currentPassword: 'actual12', newPassword: 'nueva123' });
    req.flush({ token: 'nuevo', user: ME });
    await stable();

    expect(TestBed.inject(AuthService).getToken()).toBe('nuevo');
    expect(navigateByUrl).toHaveBeenCalledWith('/results');
  });

  it('cambio voluntario: se puede cancelar y no hay aviso de contraseña temporal', async () => {
    await render({ temporary: false });

    expect(el.textContent).not.toContain('temporal');
    button('Cancelar')!.click();

    expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('contraseña temporal: avisa, no deja cancelar y permite cerrar sesión (RF-33)', async () => {
    await render({ temporary: true });

    expect(el.textContent).toContain('contraseña temporal');
    expect(button('Cancelar')).toBeUndefined();
    const logout = vi.spyOn(TestBed.inject(AuthService), 'logout').mockImplementation(() => {});
    button('Cerrar sesión')!.click();

    expect(logout).toHaveBeenCalled();
  });

  it('contraseña temporal: tras cambiarla va al dashboard; ignora returnUrl externos', async () => {
    await render({ temporary: true, returnUrl: '//malicioso.com' });
    await fill('temporal1', 'nueva123', 'nueva123');
    await submit();
    http.expectOne(`${API}/auth/me/password`).flush({ token: 'nuevo', user: ME });
    await stable();

    expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('si la repetición no coincide, avisa y no envía', async () => {
    await render({ temporary: false });
    await fill('actual12', 'nueva123', 'otra1234');
    await submit();

    http.expectNone(`${API}/auth/me/password`);
    expect(el.textContent).toContain('Las contraseñas no coinciden');
  });

  it('una nueva de menos de 6 caracteres no se envía', async () => {
    await render({ temporary: false });
    await fill('actual12', '12345', '12345');
    await submit();

    http.expectNone(`${API}/auth/me/password`);
  });

  it('si el servidor la rechaza, muestra su mensaje y se puede reintentar', async () => {
    await render({ temporary: false });
    await fill('mala1234', 'nueva123', 'nueva123');
    await submit();

    http.expectOne(`${API}/auth/me/password`).flush({ message: 'La contraseña actual no es correcta' },
      { status: 409, statusText: 'Conflict' });
    await stable();

    expect(el.textContent).toContain('La contraseña actual no es correcta');
    expect(navigateByUrl).not.toHaveBeenCalled();
    expect((el.querySelector('button[type=submit]') as HTMLButtonElement).disabled).toBe(false);
  });
});
