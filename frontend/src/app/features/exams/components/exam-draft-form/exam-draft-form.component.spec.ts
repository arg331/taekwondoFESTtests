import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { ExamDraftFormComponent } from './exam-draft-form.component';

const API = 'http://localhost:8080/api';

/** Crear un borrador: qué ve el alumno al entregar (RF-53). */
describe('ExamDraftFormComponent', () => {
  let fixture: ComponentFixture<ExamDraftFormComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ExamDraftFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExamDraftFormComponent);
    fixture.componentRef.setInput('mode', 'manual');
    el = fixture.nativeElement;
    await stable();
  });

  afterEach(() => http.verify());

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function createWith(display: string | null): Promise<Record<string, unknown>> {
    const title = el.querySelector('input[formcontrolname="title"]') as HTMLInputElement;
    title.value = 'Examen de árbitros';
    title.dispatchEvent(new Event('input'));
    if (display) {
      await (await TestbedHarnessEnvironment.loader(fixture)
        .getHarness(MatSelectHarness.with({ selector: '.result-display-select' }))).clickOptions({ text: display });
    }
    await stable();
    (Array.from(el.querySelectorAll('button')).find(b => b.classList.contains('create-btn')) as HTMLButtonElement).click();
    const req = http.expectOne(`${API}/exams/drafts`);
    const body = req.request.body;
    req.flush(null);
    return body;
  }

  it('por defecto, el alumno ve la nota y sus respuestas', async () => {
    expect(await createWith(null)).toEqual(expect.objectContaining({ showScore: true, showAnswers: true }));
  });

  it('«Nota»: solo la nota', async () => {
    expect(await createWith('Nota')).toEqual(expect.objectContaining({ showScore: true, showAnswers: false }));
  });

  it('«Nada»: ni nota ni respuestas', async () => {
    expect(await createWith('Nada')).toEqual(expect.objectContaining({ showScore: false, showAnswers: false }));
  });

  describe('aprobado (DE-13)', () => {
    function setPassingScore(value: string): void {
      const input = el.querySelector('input[formcontrolname="passingScore"]') as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }

    it('por defecto es el 50 %', async () => {
      expect(await createWith(null)).toEqual(expect.objectContaining({ passingScore: 50 }));
    });

    it('se puede elegir otro', async () => {
      setPassingScore('70');
      expect(await createWith(null)).toEqual(expect.objectContaining({ passingScore: 70 }));
    });

    it('fuera de 1 a 100 no deja crear', async () => {
      const title = el.querySelector('input[formcontrolname="title"]') as HTMLInputElement;
      title.value = 'Examen de árbitros';
      title.dispatchEvent(new Event('input'));
      await stable();
      expect((el.querySelector('.create-btn') as HTMLButtonElement).disabled).toBe(false);

      for (const invalid of ['0', '101']) {
        setPassingScore(invalid);
        await stable();
        expect((el.querySelector('.create-btn') as HTMLButtonElement).disabled).toBe(true);
      }
    });
  });
});
