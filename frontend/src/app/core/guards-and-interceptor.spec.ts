import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { jwtInterceptor } from './interceptors/jwt.interceptor';
import { AuthService } from './services/auth.service';
import { UserResponse } from './models/auth.models';

const USER: UserResponse = {
  id: 1, username: 'u', email: 'u@x.es', displayName: 'U', role: 'STUDENT', active: true,
  createdAt: '2026-10-08T10:00:00'
};

function login(role: 'ADMIN' | 'STUDENT'): void {
  localStorage.setItem('fest_token', 'jwt-abc');
  localStorage.setItem('fest_user', JSON.stringify({ ...USER, role }));
}

/**
 * Caracterización de la protección de rutas y del interceptor JWT.
 * Las rutas se prueban navegando con la configuración real de la app, sin depender
 * de cómo estén escritos los guards (canActivate, canMatch…).
 */
describe('Guards y jwtInterceptor', () => {
  let harness: RouterTestingHarness | null;

  beforeEach(() => {
    localStorage.clear();
    harness = null;
  });

  function setup(appRoutes = routes): void {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(appRoutes),
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting()
      ]
    });
  }

  /** Navega como lo haría el usuario y devuelve la URL en la que acaba. */
  async function navigate(url: string): Promise<string> {
    harness ??= await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    return TestBed.inject(Router).url;
  }

  it('sin sesión, una ruta privada lleva al login con returnUrl', async () => {
    setup();

    expect(await navigate('/exams?select=3')).toBe('/auth/login?returnUrl=%2Fexams%3Fselect%3D3');
  });

  it('con sesión de alumno, las rutas privadas comunes se abren', async () => {
    login('STUDENT');
    setup();

    expect(await navigate('/results')).toBe('/results');
  });

  it('un alumno que entra en una ruta de profesor vuelve al dashboard', async () => {
    login('STUDENT');
    setup();

    expect(await navigate('/questions')).toBe('/dashboard');
    expect(await navigate('/users')).toBe('/dashboard');
  });

  it('un profesor entra en las rutas de profesor', async () => {
    login('ADMIN');
    setup();

    expect(await navigate('/questions')).toBe('/questions');
  });

  it('las rutas públicas no piden sesión', async () => {
    setup();

    expect(await navigate('/auth/register')).toBe('/auth/register');
  });

  it('interceptor: añade Authorization si hay token', () => {
    login('STUDENT');
    setup();
    const http = TestBed.inject(HttpTestingController);

    TestBed.inject(HttpClient).get('/api/x').subscribe();

    const req = http.expectOne('/api/x');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-abc');
    req.flush({});
  });

  it('interceptor: sin token no añade cabecera', () => {
    setup();
    const http = TestBed.inject(HttpTestingController);

    TestBed.inject(HttpClient).get('/api/x').subscribe();

    const req = http.expectOne('/api/x');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('interceptor: un 401 con token cierra la sesión', () => {
    login('STUDENT');
    setup();
    const logout = vi.spyOn(TestBed.inject(AuthService), 'logout').mockImplementation(() => {});

    TestBed.inject(HttpClient).get('/api/x').subscribe({ error: () => {} });
    TestBed.inject(HttpTestingController).expectOne('/api/x')
      .flush({ message: 'no' }, { status: 401, statusText: 'Unauthorized' });

    expect(logout).toHaveBeenCalled();
  });

  it('interceptor: un 403 con token no cierra la sesión', () => {
    login('STUDENT');
    setup();
    const logout = vi.spyOn(TestBed.inject(AuthService), 'logout').mockImplementation(() => {});

    TestBed.inject(HttpClient).get('/api/x').subscribe({ error: () => {} });
    TestBed.inject(HttpTestingController).expectOne('/api/x')
      .flush(null, { status: 403, statusText: 'Forbidden' });

    expect(logout).not.toHaveBeenCalled();
  });
});
