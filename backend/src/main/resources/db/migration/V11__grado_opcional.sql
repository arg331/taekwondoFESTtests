-- RF-43 (prueba del 18/10/2026): el grado mínimo de una pregunta es opcional.
-- La restricción de valores (ck_questions_grade) sigue valiendo: un null la cumple.
alter table questions alter column grade drop not null;
