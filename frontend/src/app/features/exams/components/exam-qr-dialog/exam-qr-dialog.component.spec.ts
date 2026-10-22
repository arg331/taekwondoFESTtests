import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ExamQrDialogComponent, ExamQrDialogData } from './exam-qr-dialog.component';
import { QrCodeService } from '../../../../core/services/qr-code.service';
import { QrColorService } from '../../../../core/services/qr-color.service';

const DATA: ExamQrDialogData = {
  title: 'Examen de árbitros',
  code: 'EXM-1234ABCD',
  url: 'http://localhost:4200/exam/EXM-1234ABCD'
};

describe('ExamQrDialogComponent', () => {
  let fixture: ComponentFixture<ExamQrDialogComponent>;
  let el: HTMLElement;
  let close: ReturnType<typeof vi.fn>;
  let toDataUrl: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    close = vi.fn();
    toDataUrl = vi.fn().mockResolvedValue('data:image/png;base64,QR');
    TestBed.configureTestingModule({
      imports: [ExamQrDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: DATA },
        { provide: MatDialogRef, useValue: { close } },
        { provide: QrCodeService, useValue: { toDataUrl } }
      ]
    });
    fixture = TestBed.createComponent(ExamQrDialogComponent);
    el = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  function button(text: string): HTMLElement {
    return Array.from(el.querySelectorAll('button, a')).find(b => b.textContent?.includes(text)) as HTMLElement;
  }

  it('muestra el QR grande, el título, el código y el enlace', () => {
    expect(toDataUrl).toHaveBeenCalledWith(DATA.url, 1024, '#000000');
    expect((el.querySelector('.projection-qr') as HTMLImageElement).src).toBe('data:image/png;base64,QR');
    expect(el.textContent).toContain('Examen de árbitros');
    expect(el.textContent).toContain('EXM-1234ABCD');
    expect(el.textContent).toContain(DATA.url);
  });

  it('descarga el QR como PNG con el código como nombre', () => {
    const link = button('Descargar PNG') as HTMLAnchorElement;

    expect(link.getAttribute('href')).toBe('data:image/png;base64,QR');
    expect(link.getAttribute('download')).toBe('EXM-1234ABCD.png');
  });

  it('«Pantalla completa» pide al navegador pantalla completa', () => {
    const requestFullscreen = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(el, 'requestFullscreen', { value: requestFullscreen, configurable: true });

    button('Pantalla completa').click();

    expect(requestFullscreen).toHaveBeenCalled();
  });

  describe('color del QR de este examen (RF-50)', () => {
    async function stable(): Promise<void> {
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
    }

    it('elegir un color regenera el QR con ese color', async () => {
      const red = el.querySelector('.color-swatch[aria-label="Rojo"]') as HTMLButtonElement;
      red.click();
      await stable();

      expect(toDataUrl).toHaveBeenLastCalledWith(DATA.url, 1024, '#c62828');
      expect(red.getAttribute('aria-pressed')).toBe('true');
      expect(el.querySelector('.projection-warning')).toBeNull();
    });

    it('el color elegido es solo de este examen', async () => {
      (el.querySelector('.color-swatch[aria-label="Azul"]') as HTMLButtonElement).click();
      await stable();

      const colors = TestBed.inject(QrColorService);
      expect(colors.colorOf(DATA.code)).toBe('#0d47a1');
      expect(colors.colorOf('EXM-OTRO0000')).toBe('#000000');
    });

    it('un color propio demasiado claro avisa de que puede no leerse', async () => {
      const picker = el.querySelector('input[type=color]') as HTMLInputElement;
      picker.value = '#ffeb3b';
      picker.dispatchEvent(new Event('input'));
      await stable();

      expect(toDataUrl).toHaveBeenLastCalledWith(DATA.url, 1024, '#ffeb3b');
      expect(el.querySelector('.projection-warning')?.textContent).toContain('demasiado claro');
    });
  });

  it('«Cerrar» cierra el diálogo', () => {
    button('Cerrar').click();

    expect(close).toHaveBeenCalled();
  });
});
