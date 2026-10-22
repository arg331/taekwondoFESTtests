import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ExamAccessCardComponent } from './exam-access-card.component';
import { ExamResponse, Visibility } from '../../../../core/models/exam.models';

function exam(visibility: Visibility): ExamResponse {
  return {
    id: 7, title: 'Examen de cinturón', ownerId: 1, status: 'PUBLISHED', visibility, accessMode: 'OPEN',
    config: { numberOfQuestions: 10, timeLimitMinutes: null, showScore: true, randomizeOptions: false,
      randomizeQuestionOrder: false },
    questionIds: [], generationTags: [], code: 'EXM-1234ABCD', createdAt: '2026-10-12T10:00:00', expiresAt: null, maxAttempts: 1
  };
}

/** Portada del examen: el alumno sabe si es de prueba u oficial antes de empezar (RF-49). */
describe('ExamAccessCardComponent', () => {
  let fixture: ComponentFixture<ExamAccessCardComponent>;
  let el: HTMLElement;

  async function render(visibility: Visibility, maxAttempts = 1): Promise<void> {
    TestBed.configureTestingModule({ imports: [ExamAccessCardComponent], providers: [provideRouter([])] });
    fixture = TestBed.createComponent(ExamAccessCardComponent);
    fixture.componentRef.setInput('exam', { ...exam(visibility), maxAttempts });
    el = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('con varios intentos, dice cuántas veces se puede hacer (RF-52)', async () => {
    await render('PRIVATE', 3);
    expect(el.textContent).toContain('Puedes hacerlo 3 veces');
  });

  it('con un solo intento, avisa de que solo hay uno', async () => {
    await render('PRIVATE');
    expect(el.textContent).toContain('Solo puedes hacerlo una vez');
  });

  it('un examen público se presenta como examen de prueba', async () => {
    await render('PUBLIC');

    const kind = el.querySelector('.exam-kind');
    expect(kind?.textContent).toContain('Examen de prueba');
    expect(kind?.textContent).toContain('no es un examen oficial');
  });

  it('con sesión rellena nombre, club y email de la cuenta; sin email ni club los deja vacíos', async () => {
    await render('PRIVATE');
    const field = (name: string) => (el.querySelector(`input[formcontrolname="${name}"]`) as HTMLInputElement).value;

    fixture.componentRef.setInput('user', {
      id: 3, username: 'ana', email: null, displayName: 'Ana López', role: 'STUDENT',
      club: { id: 2, name: 'Club Badajoz' }, active: true, mustChangePassword: false, createdAt: '2026-10-13T10:00:00'
    });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(field('studentName')).toBe('Ana López');
    expect(field('studentClub')).toBe('Club Badajoz');
    expect(field('studentEmail')).toBe('');
  });

  it('un examen privado se presenta como oficial', async () => {
    await render('PRIVATE');

    expect(el.querySelector('.exam-kind')?.textContent).toContain('Examen oficial');
  });
});
