import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { PublicExamResponse } from '../../core/models/exam.models';
import { UserResponse } from '../../core/models/auth.models';

const API = 'http://localhost:8080/api';

const STUDENT: UserResponse = {
  id: 7, username: 'ana', email: null, displayName: 'Ana', role: 'STUDENT', club: null, active: true,
  mustChangePassword: false, createdAt: '2026-10-08T10:00:00'
};

/** Lo que manda el servidor: sin ids ni datos internos del profesor (DE-22). */
const PRACTICE: PublicExamResponse[] = [
  { title: 'Reglamento de combate', code: 'EXM-AAAA1111', visibility: 'PUBLIC', accessMode: 'OPEN',
    config: { numberOfQuestions: 10, timeLimitMinutes: 15 }, maxAttempts: null, expiresAt: '2026-11-05T20:00:00' },
  { title: 'Puntuación', code: 'EXM-BBBB2222', visibility: 'PUBLIC', accessMode: 'OPEN',
    config: { numberOfQuestions: 5, timeLimitMinutes: null }, maxAttempts: 1, expiresAt: null }
];

/** Panel del alumno: los exámenes de prueba se muestran y se abren por su código. */
describe('DashboardComponent (alumno)', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(async () => {
    sessionStorage.clear();
    sessionStorage.setItem('fest_token', 'jwt-abc');
    sessionStorage.setItem('fest_user', JSON.stringify(STUDENT));
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DashboardComponent);
    el = fixture.nativeElement;
    fixture.detectChanges();
    http.expectOne(`${API}/exams/public`).flush(PRACTICE);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  function cards(): HTMLElement[] {
    return Array.from(el.querySelectorAll<HTMLElement>('.exam-card'));
  }

  it('muestra cada examen de prueba con sus preguntas, su tiempo y cuándo expira', () => {
    expect(cards().map(c => c.querySelector('mat-card-title')?.textContent?.trim()))
      .toEqual(['Reglamento de combate', 'Puntuación']);
    expect(cards()[0].textContent).toContain('10 preguntas');
    expect(cards()[0].textContent).toContain('15 min');
    expect(cards()[0].textContent).toContain('Expira: 05/11/26 20:00');
    expect(cards()[1].textContent).not.toContain('Expira');
  });

  it('al pulsar un examen va a hacerlo por su código', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    cards()[1].click();

    expect(navigate).toHaveBeenCalledWith(['/exam', 'EXM-BBBB2222']);
  });
});
