import { copyMessage } from './community-exam-detail.component';

/** El aviso al copiar preguntas a mi banco (DE-12). */
describe('copyMessage', () => {
  it('cuenta las copiadas, en singular o plural', () => {
    expect(copyMessage({ copied: 1, alreadyInBank: 0 })).toBe('1 pregunta copiada a tu banco');
    expect(copyMessage({ copied: 4, alreadyInBank: 0 })).toBe('4 preguntas copiadas a tu banco');
  });

  it('añade las que ya tenía', () => {
    expect(copyMessage({ copied: 2, alreadyInBank: 1 })).toBe('2 preguntas copiadas a tu banco; 1 ya la tenías');
    expect(copyMessage({ copied: 0, alreadyInBank: 3 })).toBe('No se ha copiado ninguna pregunta; 3 ya las tenías');
  });
});
