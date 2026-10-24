import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { RegisterComponent } from './register.component';

const API = 'http://localhost:8080/api';
const CLUBS = [{ id: 1, name: 'Club Ávila' }, { id: 2, name: 'Club Badajoz' }];

/** Registro público (RF-01): email y club opcionales. */
describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RegisterComponent);
    el = fixture.nativeElement;
    await stable();
    http.expectOne(`${API}/clubs`).flush(CLUBS);
    await stable();
  });

  afterEach(() => http.verify());

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function type(name: string, value: string): void {
    const input = el.querySelector(`input[formcontrolname="${name}"]`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function fillRequired(): Promise<void> {
    type('displayName', 'Ana López');
    type('username', 'ana_lopez');
    type('plainPassword', 'secreto1');
    type('confirmPassword', 'secreto1');
    await stable();
  }

  function submitButton(): HTMLButtonElement {
    return Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes('Crear cuenta')) as HTMLButtonElement;
  }

  it('sin email ni club se puede registrar: se envían como null', async () => {
    await fillRequired();

    expect(submitButton().disabled).toBe(false);
    submitButton().click();
    const req = http.expectOne(`${API}/auth/register`);
    expect(req.request.body).toEqual({
      displayName: 'Ana López', username: 'ana_lopez', email: null, plainPassword: 'secreto1', clubId: null
    });
    req.flush({});
    await stable();

    expect(el.textContent).toContain('Registro completado');
  });

  it('con email y club se envían los dos', async () => {
    await fillRequired();
    type('email', '  ana@correo.es ');
    await (await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness)).clickOptions({ text: 'Club Badajoz' });
    await stable();

    submitButton().click();
    const req = http.expectOne(`${API}/auth/register`);
    expect(req.request.body.email).toBe('ana@correo.es');
    expect(req.request.body.clubId).toBe(2);
    req.flush({});
  });

  it('el desplegable ofrece «Sin club» y los clubes que existen', async () => {
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
    await select.open();

    expect(await Promise.all((await select.getOptions()).map(o => o.getText()))).toEqual(['Sin club', 'Club Ávila', 'Club Badajoz']);
  });

  it('un email mal escrito no deja enviar', async () => {
    await fillRequired();
    type('email', 'no-es-un-email');
    await stable();

    expect(submitButton().disabled).toBe(true);
  });

  it('un usuario o email repetido lo dice', async () => {
    await fillRequired();
    submitButton().click();
    http.expectOne(`${API}/auth/register`).flush({ message: 'x' }, { status: 409, statusText: 'Conflict' });
    await stable();

    expect(el.textContent).toContain('El usuario o email ya existe');
  });

  it('demasiados intentos (RNF-07): muestra el aviso del servidor', async () => {
    await fillRequired();
    submitButton().click();
    http.expectOne(`${API}/auth/register`).flush({ message: 'Demasiados intentos. Espera un minuto y vuelve a intentarlo.' }, { status: 429, statusText: 'Too Many Requests' });
    await stable();

    expect(el.textContent).toContain('Demasiados intentos. Espera un minuto y vuelve a intentarlo.');
  });
});
