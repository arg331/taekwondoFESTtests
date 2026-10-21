import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { ExamAccessCardComponent } from './exam-access-card.component';
import { ExamResponse, Visibility } from '../../../../core/models/exam.models';
import { StudentInfo } from './exam-access-card.component';

const API = 'http://localhost:8080/api';
const CLUBS = [{ id: 1, name: 'Club Ávila' }, { id: 2, name: 'Club Badajoz' }];

function exam(visibility: Visibility): ExamResponse {
  return {
    id: 7, title: 'Examen de cinturón', ownerId: 1, status: 'PUBLISHED', visibility, accessMode: 'OPEN',
    config: { numberOfQuestions: 10, timeLimitMinutes: null, showScore: true, showAnswers: true, randomizeOptions: false,
      randomizeQuestionOrder: false },
    questionIds: [], generationTags: [], code: 'EXM-1234ABCD', createdAt: '2026-10-12T10:00:00', expiresAt: null, maxAttempts: 1,
    sharedBy: null
  };
}

/** Portada del examen: el alumno sabe si es de prueba u oficial antes de empezar (RF-49). */
describe('ExamAccessCardComponent', () => {
  let fixture: ComponentFixture<ExamAccessCardComponent>;
  let el: HTMLElement;
  let http: HttpTestingController;

  async function render(visibility: Visibility, maxAttempts: number | null = 1): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ExamAccessCardComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExamAccessCardComponent);
    fixture.componentRef.setInput('exam', { ...exam(visibility), maxAttempts });
    el = fixture.nativeElement;
    fixture.detectChanges();
    http.expectOne(`${API}/clubs`).flush(CLUBS);
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function clubSelect(): Promise<MatSelectHarness> {
    return TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
  }

  afterEach(() => http.verify());

  it('con varios intentos, dice cuántas veces se puede hacer (RF-52)', async () => {
    await render('PRIVATE', 3);
    expect(el.textContent).toContain('Puedes hacerlo 3 veces');
  });

  it('sin límite, dice que se puede hacer las veces que quiera', async () => {
    await render('PRIVATE', null);
    expect(el.textContent).toContain('Puedes hacerlo las veces que quieras');
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
    expect(await (await clubSelect()).getValueText()).toBe('Club Badajoz');
    expect(field('studentEmail')).toBe('');
  });

  describe('club de un desplegable (también sin cuenta)', () => {
    async function startWith(club: string | null): Promise<StudentInfo> {
      const started: StudentInfo[] = [];
      fixture.componentInstance.start.subscribe(s => started.push(s));
      const name = el.querySelector('input[formcontrolname="studentName"]') as HTMLInputElement;
      name.value = 'Ana';
      name.dispatchEvent(new Event('input'));
      if (club) await (await clubSelect()).clickOptions({ text: club });
      fixture.detectChanges();
      (Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes('Comenzar examen')) as HTMLButtonElement).click();
      return started[0];
    }

    it('ofrece «Sin club» y los clubes que existen', async () => {
      await render('PRIVATE');
      const select = await clubSelect();
      await select.open();

      const options = await select.getOptions();
      expect(await Promise.all(options.map(o => o.getText()))).toEqual(['Sin club', 'Club Ávila', 'Club Badajoz']);
    });

    it('el anónimo elige su club y empieza con él', async () => {
      await render('PRIVATE');

      expect((await startWith('Club Ávila')).studentClub).toBe('Club Ávila');
    });

    it('sin elegir club, empieza sin club', async () => {
      await render('PRIVATE');

      expect((await startWith(null)).studentClub).toBeNull();
    });
  });

  it('un examen privado se presenta como oficial', async () => {
    await render('PRIVATE');

    expect(el.querySelector('.exam-kind')?.textContent).toContain('Examen oficial');
  });
});
