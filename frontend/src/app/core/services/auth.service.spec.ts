import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { LoginResponse, UserResponse } from '../models/auth.models';

const API = 'http://localhost:8080/api';

const ADMIN: UserResponse = {
  id: 1, username: 'admin', email: 'admin@x.es', displayName: 'Admin', role: 'ADMIN', club: null, active: true, mustChangePassword: false,
  createdAt: '2026-10-08T10:00:00'
};
const STUDENT: UserResponse = { ...ADMIN, id: 2, username: 'alumno', role: 'STUDENT' };
const TEACHER: UserResponse = { ...ADMIN, id: 3, username: 'profe', role: 'TEACHER' };

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

  beforeEach(() => (localStorage.clear(), sessionStorage.clear()));
  afterEach(() => http.verify());

  it('login: guarda token y usuario solo en esta sesión del navegador, nunca en localStorage (DE-10)', () => {
    const auth = setup();
    const response: LoginResponse = { token: 'jwt-123', user: ADMIN };

    auth.login({ usernameOrEmail: 'admin', plainPassword: 'secreto' }).subscribe();
    const req = http.expectOne(`${API}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ usernameOrEmail: 'admin', plainPassword: 'secreto' });
    req.flush(response);

    expect(sessionStorage.getItem('fest_token')).toBe('jwt-123');
    expect(JSON.parse(sessionStorage.getItem('fest_user')!)).toEqual(ADMIN);
    // Al cerrar el navegador se pierde: hay que volver a iniciar sesión
    expect(localStorage.getItem('fest_token')).toBeNull();
    expect(localStorage.getItem('fest_user')).toBeNull();
    expect(auth.getToken()).toBe('jwt-123');
    expect(auth.currentUser()).toEqual(ADMIN);
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.isAdmin()).toBe(true);
  });

  it('register: no inicia sesión', () => {
    const auth = setup();

    auth.register({ username: 'u', email: 'u@x.es', plainPassword: 'secreto1', displayName: 'U', clubId: null }).subscribe();
    const req = http.expectOne(`${API}/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush(STUDENT);

    expect(auth.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem('fest_token')).toBeNull();
  });

  it('recupera la sesión guardada al arrancar; un alumno no es admin', () => {
    sessionStorage.setItem('fest_token', 't');
    sessionStorage.setItem('fest_user', JSON.stringify(STUDENT));

    const auth = setup();

    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.isAdmin()).toBe(false);
    expect(auth.canTeach()).toBe(false);
  });

  it('un profesor prepara exámenes pero no administra (RF-04)', () => {
    sessionStorage.setItem('fest_token', 't');
    sessionStorage.setItem('fest_user', JSON.stringify(TEACHER));

    const auth = setup();

    expect(auth.canTeach()).toBe(true);
    expect(auth.isAdmin()).toBe(false);
  });

  it('el administrador también puede preparar exámenes', () => {
    sessionStorage.setItem('fest_token', 't');
    sessionStorage.setItem('fest_user', JSON.stringify(ADMIN));

    const auth = setup();

    expect(auth.canTeach()).toBe(true);
    expect(auth.isAdmin()).toBe(true);
  });

  it('logout: borra la sesión y navega al login', () => {
    sessionStorage.setItem('fest_token', 't');
    sessionStorage.setItem('fest_user', JSON.stringify(ADMIN));
    const auth = setup();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    auth.logout();

    expect(sessionStorage.getItem('fest_token')).toBeNull();
    expect(sessionStorage.getItem('fest_user')).toBeNull();
    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('me: consulta el usuario actual', () => {
    const auth = setup();

    auth.me().subscribe();

    http.expectOne(r => r.method === 'GET' && r.url === `${API}/auth/me`).flush(ADMIN);
  });

  it('changePassword: guarda la sesión nueva que devuelve el servidor (RF-06)', () => {
    sessionStorage.setItem('fest_token', 'viejo');
    sessionStorage.setItem('fest_user', JSON.stringify({ ...STUDENT, mustChangePassword: true }));
    const auth = setup();
    expect(auth.mustChangePassword()).toBe(true);

    auth.changePassword({ currentPassword: 'temporal1', newPassword: 'nueva123' }).subscribe();
    const req = http.expectOne(`${API}/auth/me/password`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ currentPassword: 'temporal1', newPassword: 'nueva123' });
    req.flush({ token: 'nuevo', user: STUDENT });

    expect(auth.getToken()).toBe('nuevo');
    expect(auth.currentUser()).toEqual(STUDENT);
    expect(auth.mustChangePassword()).toBe(false);
  });
});
