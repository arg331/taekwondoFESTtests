import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { NavbarComponent } from './navbar.component';
import { UserRole } from '../../../core/models/auth.models';

/** Enlaces de la barra según el rol (RF-04). */
describe('NavbarComponent', () => {
  let fixture: ComponentFixture<NavbarComponent>;

  function render(role: UserRole): string[] {
    localStorage.setItem('fest_token', 't');
    localStorage.setItem('fest_user', JSON.stringify({
      id: 1, username: 'u', email: 'u@x.es', displayName: 'U', role, club: null, active: true, mustChangePassword: false, createdAt: '2026-10-13T10:00:00'
    }));
    TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [provideRouter([]), provideHttpClient()]
    });
    fixture = TestBed.createComponent(NavbarComponent);
    fixture.detectChanges();
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.nav-links a'))
      .map(a => a.textContent?.replace(/\s+/g, ' ').trim().replace(/^\S+ /, '') ?? '');
  }

  afterEach(() => localStorage.clear());

  it('el alumno ve su panel y sus resultados', () => {
    expect(render('STUDENT')).toEqual(['Dashboard', 'Mis resultados']);
  });

  it('el profesor ve preguntas, exámenes, resultados y clubes, pero no usuarios', () => {
    expect(render('TEACHER')).toEqual(['Dashboard', 'Preguntas', 'Exámenes', 'Resultados', 'Clubes']);
  });

  it('el administrador ve además la gestión de usuarios', () => {
    expect(render('ADMIN')).toEqual(['Dashboard', 'Preguntas', 'Exámenes', 'Resultados', 'Clubes', 'Usuarios']);
  });

  it('el menú de usuario lleva a cambiar la contraseña (RF-06)', async () => {
    render('STUDENT');
    ((fixture.nativeElement as HTMLElement).querySelector('.user-btn') as HTMLButtonElement).click();
    fixture.detectChanges();
    await fixture.whenStable();

    const link = Array.from(document.querySelectorAll('.mat-mdc-menu-panel a'))
      .find(a => a.textContent?.includes('Cambiar contraseña'));
    expect(link?.getAttribute('href')).toBe('/auth/change-password');
  });
});
