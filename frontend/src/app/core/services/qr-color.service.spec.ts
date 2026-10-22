import { TestBed } from '@angular/core/testing';
import { QR_COLOR_PRESETS, QR_DEFAULT_COLOR, QrColorService, isLowContrast } from './qr-color.service';

const [BLACK, RED, BLUE] = QR_COLOR_PRESETS.map(p => p.value);

/**
 * Color del QR de cada examen (RF-50): distingue en el aula qué QR hay que escanear.
 * Se recuerda en el navegador y, al publicar, se propone uno que no usen los otros exámenes abiertos.
 */
describe('QrColorService', () => {
  function create(): QrColorService {
    TestBed.resetTestingModule();
    return TestBed.inject(QrColorService);
  }

  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('un examen sin color elegido usa el negro', () => {
    expect(create().colorOf('EXM-AAAA1111')).toBe(QR_DEFAULT_COLOR);
  });

  it('cada examen recuerda su propio color la próxima vez', () => {
    const service = create();
    service.setColor('EXM-AAAA1111', '#C62828');
    service.setColor('EXM-BBBB2222', '#0d47a1');

    const later = create();
    expect(later.colorOf('EXM-AAAA1111')).toBe('#c62828');
    expect(later.colorOf('EXM-BBBB2222')).toBe('#0d47a1');
  });

  it('ignora lo que no sea un color hexadecimal', () => {
    const service = create();
    service.setColor('EXM-AAAA1111', 'red; background:url(x)');

    expect(service.colorOf('EXM-AAAA1111')).toBe(QR_DEFAULT_COLOR);
  });

  it('ignora lo guardado si está corrupto', () => {
    localStorage.setItem('qr-colors', '{"EXM-AAAA1111":"javascript:x"');

    expect(create().colorOf('EXM-AAAA1111')).toBe(QR_DEFAULT_COLOR);
  });

  it('avisa si el color es demasiado claro para escanearlo', () => {
    expect(isLowContrast('#ffeb3b')).toBe(true);
    expect(QR_COLOR_PRESETS.every(p => !isLowContrast(p.value))).toBe(true);
  });

  it('sin acceso al almacenamiento del navegador sigue funcionando', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('bloqueado'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('bloqueado'); });
    const service = create();

    expect(service.colorOf('EXM-AAAA1111')).toBe(QR_DEFAULT_COLOR);
    service.setColor('EXM-AAAA1111', '#0d47a1');
    expect(service.colorOf('EXM-AAAA1111')).toBe('#0d47a1');
  });

  describe('color distinto al publicar', () => {
    it('elige el primero de la paleta que no usan los otros exámenes abiertos', () => {
      const service = create();
      service.setColor('EXM-AAAA1111', BLACK);
      service.setColor('EXM-BBBB2222', RED);

      service.assignDistinct('EXM-CCCC3333', ['EXM-AAAA1111', 'EXM-BBBB2222']);

      expect(service.colorOf('EXM-CCCC3333')).toBe(BLUE);
    });

    it('los exámenes abiertos sin color elegido cuentan como negros', () => {
      const service = create();

      service.assignDistinct('EXM-CCCC3333', ['EXM-AAAA1111']);

      expect(service.colorOf('EXM-CCCC3333')).toBe(RED);
    });

    it('si se agota la paleta, repite el color menos usado', () => {
      const service = create();
      const open = QR_COLOR_PRESETS.map((p, i) => {
        const code = `EXM-OPEN000${i}`;
        service.setColor(code, p.value);
        return code;
      });
      service.setColor('EXM-EXTRA001', BLACK);

      service.assignDistinct('EXM-CCCC3333', [...open, 'EXM-EXTRA001']);

      expect(service.colorOf('EXM-CCCC3333')).toBe(RED);
    });

    it('respeta el color que el examen ya tenía', () => {
      const service = create();
      service.setColor('EXM-CCCC3333', BLUE);

      service.assignDistinct('EXM-CCCC3333', []);

      expect(service.colorOf('EXM-CCCC3333')).toBe(BLUE);
    });
  });
});
