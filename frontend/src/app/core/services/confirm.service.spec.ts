import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { ConfirmService } from './confirm.service';

/** Confirmaciones con un diálogo de Material en lugar del confirm() del navegador. */
describe('ConfirmService', () => {
  let service: ConfirmService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } }]
    });
    service = TestBed.inject(ConfirmService);
  });

  afterEach(() => document.querySelectorAll('.cdk-overlay-container').forEach(c => c.remove()));

  function dialog(): HTMLElement | null {
    return document.querySelector('app-confirm-dialog');
  }

  function button(label: string): HTMLButtonElement {
    return Array.from(dialog()!.querySelectorAll('button')).find(b => b.textContent?.trim() === label) as HTMLButtonElement;
  }

  async function open(answers: boolean[]): Promise<void> {
    service.ask({ title: 'Borrar tag', message: '¿Borrar «Combate»?', confirmLabel: 'Borrar' })
      .subscribe(ok => answers.push(ok));
    await new Promise(resolve => setTimeout(resolve));
  }

  it('enseña el título, la pregunta y los dos botones; el foco empieza en «Cancelar»', async () => {
    await open([]);

    expect(dialog()!.querySelector('h2')?.textContent).toContain('Borrar tag');
    expect(dialog()!.textContent).toContain('¿Borrar «Combate»?');
    expect(button('Borrar')).toBeTruthy();
    // jsdom no pinta, así que no puede mover el foco: se comprueba la marca que lo pone en «Cancelar»
    expect(button('Cancelar').hasAttribute('cdkfocusinitial')).toBe(true);
    expect(document.querySelector('[role="alertdialog"]')).not.toBeNull();
  });

  it('confirmar responde que sí y cierra', async () => {
    const answers: boolean[] = [];
    await open(answers);

    button('Borrar').click();
    await new Promise(resolve => setTimeout(resolve));

    expect(answers).toEqual([true]);
    expect(dialog()).toBeNull();
  });

  it('cancelar, o cerrarlo de otra forma, responde que no', async () => {
    const answers: boolean[] = [];
    await open(answers);

    button('Cancelar').click();
    await new Promise(resolve => setTimeout(resolve));

    expect(answers).toEqual([false]);
  });
});
