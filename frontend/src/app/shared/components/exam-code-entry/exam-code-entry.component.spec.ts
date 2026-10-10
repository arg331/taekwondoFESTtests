import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ExamCodeEntryComponent } from './exam-code-entry.component';

describe('ExamCodeEntryComponent', () => {
  let fixture: ComponentFixture<ExamCodeEntryComponent>;
  let el: HTMLElement;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    TestBed.configureTestingModule({ imports: [ExamCodeEntryComponent], providers: [provideRouter([])] });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(ExamCodeEntryComponent);
    el = fixture.nativeElement;
    await stable();
  });

  afterEach(() => vi.restoreAllMocks());

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function submit(code: string): Promise<void> {
    const input = el.querySelector('input') as HTMLInputElement;
    input.value = code;
    input.dispatchEvent(new Event('input'));
    (el.querySelector('button[type=submit]') as HTMLButtonElement).click();
    await stable();
  }

  it('lleva al examen con el código normalizado', async () => {
    await submit(' exm-1a2b3c4d ');

    expect(navigate).toHaveBeenCalledWith(['/exam', 'EXM-1A2B3C4D']);
    expect(el.querySelector('.code-error')).toBeNull();
  });

  it('un código con otro formato avisa y no navega', async () => {
    await submit('hola');

    expect(navigate).not.toHaveBeenCalled();
    expect(el.querySelector('.code-error')?.textContent).toContain('EXM-1A2B3C4D');
  });

  it('el aviso desaparece al corregir el código', async () => {
    await submit('hola');
    await submit('1a2b3c4d');

    expect(el.querySelector('.code-error')).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/exam', 'EXM-1A2B3C4D']);
  });
});
