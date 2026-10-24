import { normalizeExamCode } from './exam-code';

describe('normalizeExamCode', () => {
  it.each([
    ['EXM-1A2B3C4D', 'EXM-1A2B3C4D'],
    ['exm-1a2b3c4d', 'EXM-1A2B3C4D'],
    ['  EXM-1A2B3C4D  ', 'EXM-1A2B3C4D'],
    ['exm 1a2b 3c4d', 'EXM-1A2B3C4D'],
    ['EXM1A2B3C4D', 'EXM-1A2B3C4D'],
    ['1a2b3c4d', 'EXM-1A2B3C4D'],
    ['https://fest.example/exam/EXM-1A2B3C4D', 'EXM-1A2B3C4D'],
    ['http://localhost:4200/exam/exm-1a2b3c4d/', 'EXM-1A2B3C4D']
  ])('"%s" → %s', (input, expected) => {
    expect(normalizeExamCode(input)).toBe(expected);
  });

  it.each(['', '   ', 'hola', 'EXM-1A2B3C4', 'EXM-1A2B3C4DE', 'EXM-ZZZZZZZZ'])('"%s" no es un código', input => {
    expect(normalizeExamCode(input)).toBeNull();
  });
});
