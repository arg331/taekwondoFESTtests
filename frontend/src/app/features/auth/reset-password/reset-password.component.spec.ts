import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ResetPasswordComponent } from './reset-password.component';

const API = 'http://localhost:8080/api';

/** La contraseña nueva desde el enlace del correo (RF-34). */
describe('ResetPasswordComponent', () => {
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function render(token: string | undefined): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ResetPasswordComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ResetPasswordComponent);
    // Llega de la ruta: /auth/reset-password?token=...
    fixture.componentRef.setInput('token', token);
    el = fixture.nativeElement;
    await stable();
  }

  afterEach(() => http.verify());

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function fill(password: string, repeat: string): Promise<void> {
    const inputs = el.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    inputs[0].value = password;
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = repeat;
    inputs[1].dispatchEvent(new Event('input'));
    await stable();
  }

  function submitButton(): HTMLButtonElement {
    return el.querySelector('button[type=submit]') as HTMLButtonElement;
  }

  it('pone la contraseña nueva con el código del enlace y lleva a iniciar sesión', async () => {
    await render('codigo-del-enlace');
    await fill('nueva123', 'nueva123');
    submitButton().click();

    const req = http.expectOne(`${API}/auth/recovery/reset`);
    expect(req.request.body).toEqual({ token: 'codigo-del-enlace', newPassword: 'nueva123' });
    req.flush(null, { status: 204, statusText: 'No Content' });
    await stable();

    expect(el.querySelector('.done')?.textContent).toContain('Contraseña cambiada');
    expect(el.querySelector('.done a')?.getAttribute('href')).toBe('/auth/login');
  });

  it('si no coinciden, o es corta, no deja enviar', async () => {
    await render('codigo');
    await fill('nueva123', 'otra1234');
    expect(submitButton().disabled).toBe(true);

    await fill('123', '123');
    expect(submitButton().disabled).toBe(true);
  });

  it('enlace caducado o usado: el mensaje del servidor y cómo pedir otro', async () => {
    await render('codigo');
    await fill('nueva123', 'nueva123');
    submitButton().click();
    http.expectOne(`${API}/auth/recovery/reset`)
      .flush({ message: 'El enlace no es válido o ha caducado: pide otro' }, { status: 409, statusText: 'Conflict' });
    await stable();

    expect(el.querySelector('.error-msg')?.textContent).toContain('El enlace no es válido o ha caducado');
    expect(el.querySelector('a[href="/auth/recover"]')).not.toBeNull();
  });

  it('sin código en el enlace, lo dice y no muestra el formulario', async () => {
    await render(undefined);

    expect(el.textContent).toContain('Este enlace está incompleto');
    expect(el.querySelector('form')).toBeNull();
    expect(el.querySelector('a[href="/auth/recover"]')).not.toBeNull();
  });
});
