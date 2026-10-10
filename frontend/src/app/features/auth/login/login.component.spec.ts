import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { LoginComponent } from './login.component';

const API = 'http://localhost:8080/api';

/** Caracterización del login: a dónde lleva tras entrar y qué muestra si falla. */
describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let http: HttpTestingController;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;
  let el: HTMLElement;

  async function render(returnUrl: string | null): Promise<void> {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: { snapshot: { queryParamMap: convertToParamMap(returnUrl ? { returnUrl } : {}) } }
    });
    http = TestBed.inject(HttpTestingController);
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(LoginComponent);
    el = fixture.nativeElement;
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function submit(user: string, password: string): Promise<void> {
    const inputs = el.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    inputs[0].value = user;
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = password;
    inputs[1].dispatchEvent(new Event('input'));
    await stable();
    (el.querySelector('button[type=submit]') as HTMLButtonElement).click();
    await stable();
  }

  function loginOk(): void {
    http.expectOne(`${API}/auth/login`).flush({
      token: 't', user: { id: 1, username: 'u', email: 'u@x.es', displayName: 'U', role: 'STUDENT', active: true,
        createdAt: '2026-10-08T10:00:00' }
    });
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  it('sin returnUrl va al dashboard', async () => {
    await render(null);
    await submit('u', 'secreto1');
    loginOk();

    expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('con returnUrl interno vuelve a esa ruta', async () => {
    await render('/exam/EXM-ABC');
    await submit('u', 'secreto1');
    loginOk();

    expect(navigateByUrl).toHaveBeenCalledWith('/exam/EXM-ABC');
  });

  it('ignora returnUrl externos (//otro-sitio o http://)', async () => {
    await render('//malicioso.com');
    await submit('u', 'secreto1');
    loginOk();
    expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('credenciales incorrectas: muestra el error y no navega', async () => {
    await render(null);
    await submit('u', 'malamala');

    http.expectOne(`${API}/auth/login`).flush({ message: 'Credenciales inválidas' },
      { status: 401, statusText: 'Unauthorized' });
    await stable();

    expect(el.textContent).toContain('Usuario o contraseña incorrectos');
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('contraseña de menos de 6 caracteres: no envía', async () => {
    await render(null);
    await submit('u', '12345');

    http.expectNone(`${API}/auth/login`);
  });
});
