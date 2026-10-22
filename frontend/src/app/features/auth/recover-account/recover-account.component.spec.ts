import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RecoverAccountComponent } from './recover-account.component';

const API = 'http://localhost:8080/api';
const SENT = 'Si ese email está registrado, te hemos enviado un correo con tu usuario y un enlace para cambiar la contraseña.';

/** Recuperar el usuario o la contraseña sin sesión (RF-34). */
describe('RecoverAccountComponent', () => {
  let fixture: ComponentFixture<RecoverAccountComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [RecoverAccountComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RecoverAccountComponent);
    el = fixture.nativeElement;
    await stable();
  });

  afterEach(() => http.verify());

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function typeEmail(email: string): Promise<void> {
    const input = el.querySelector('input') as HTMLInputElement;
    input.value = email;
    input.dispatchEvent(new Event('input'));
    await stable();
  }

  function submitButton(): HTMLButtonElement {
    return el.querySelector('button[type=submit]') as HTMLButtonElement;
  }

  it('envía el email y muestra la respuesta del servidor, que es la misma exista o no la cuenta', async () => {
    await typeEmail(' ana@x.es ');
    submitButton().click();

    const req = http.expectOne(`${API}/auth/recovery`);
    expect(req.request.body).toEqual({ email: 'ana@x.es' });
    req.flush({ message: SENT }, { status: 202, statusText: 'Accepted' });
    await stable();

    expect(el.querySelector('.sent')?.textContent).toContain(SENT);
    expect(el.querySelector('form')).toBeNull();
  });

  it('con un email mal escrito no deja enviar', async () => {
    await typeEmail('ana');

    expect(submitButton().disabled).toBe(true);
  });

  it('recuerda que sin email en la cuenta se lo pide al administrador', () => {
    expect(el.textContent).toContain('Si tu cuenta no tiene email, pídele al administrador');
  });

  it('demasiados intentos (RNF-07): muestra el aviso del servidor', async () => {
    await typeEmail('ana@x.es');
    submitButton().click();
    http.expectOne(`${API}/auth/recovery`).flush({ message: 'Demasiados intentos. Espera un minuto y vuelve a intentarlo.' },
      { status: 429, statusText: 'Too Many Requests' });
    await stable();

    expect(el.querySelector('.error-msg')?.textContent).toContain('Demasiados intentos');
  });
});
