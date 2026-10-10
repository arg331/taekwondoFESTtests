import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { LoginResponse, UserResponse } from '../models/auth.models';

const API = 'http://localhost:8080/api';

const ADMIN: UserResponse = {
  id: 1, username: 'admin', email: 'admin@x.es', displayName: 'Admin', role: 'ADMIN', active: true,
  createdAt: '2026-10-08T10:00:00'
};
const STUDENT: UserResponse = { ...ADMIN, id: 2, username: 'alumno', role: 'STUDENT' };

/** Caracterización de la sesión: dónde se guarda, qué se expone y cómo se cierra. */
describe('AuthService', () => {
  let http: HttpTestingController;

  function setup(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http.verify());

  it('login: envía credenciales y guarda token y usuario en localStorage', () => {
    const auth = setup();
    const response: LoginResponse = { token: 'jwt-123', user: ADMIN };

    auth.login({ usernameOrEmail: 'admin', plainPassword: 'secreto' }).subscribe();
    const req = http.expectOne(`${API}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ usernameOrEmail: 'admin', plainPassword: 'secreto' });
    req.flush(response);

    expect(localStorage.getItem('fest_token')).toBe('jwt-123');
    expect(JSON.parse(localStorage.getItem('fest_user')!)).toEqual(ADMIN);
    expect(auth.getToken()).toBe('jwt-123');
    expect(auth.currentUser()).toEqual(ADMIN);
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.isAdmin()).toBe(true);
  });

  it('register: no inicia sesión', () => {
    const auth = setup();

    auth.register({ username: 'u', email: 'u@x.es', plainPassword: 'secreto1', displayName: 'U' }).subscribe();
    const req = http.expectOne(`${API}/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush(STUDENT);

    expect(auth.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('fest_token')).toBeNull();
  });

  it('recupera la sesión guardada al arrancar; un alumno no es admin', () => {
    localStorage.setItem('fest_token', 't');
    localStorage.setItem('fest_user', JSON.stringify(STUDENT));

    const auth = setup();

    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.isAdmin()).toBe(false);
  });

  it('logout: borra la sesión y navega al login', () => {
    localStorage.setItem('fest_token', 't');
    localStorage.setItem('fest_user', JSON.stringify(ADMIN));
    const auth = setup();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    auth.logout();

    expect(localStorage.getItem('fest_token')).toBeNull();
    expect(localStorage.getItem('fest_user')).toBeNull();
    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('me: consulta el usuario actual', () => {
    const auth = setup();

    auth.me().subscribe();

    http.expectOne(r => r.method === 'GET' && r.url === `${API}/auth/me`).flush(ADMIN);
  });
});
