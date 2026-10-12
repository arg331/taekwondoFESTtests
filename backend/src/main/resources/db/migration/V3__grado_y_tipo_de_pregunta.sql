-- Bloque 2 de la entrevista (11/10/2026): el grado sustituye a la dificultad (RF-43)
-- y cada pregunta puede ser de tipo C o D (RF-44). Las opciones dejan de tener
-- máximo de 4 (RF-09); question_options ya admite cualquier número.

alter table questions add column grade varchar(20);

-- Se conserva el orden de dificultad: fácil → blanco, medio → verde, difícil → rojo
update questions set grade = case difficulty
    when 'FACIL'   then 'BLANCO'
    when 'MEDIO'   then 'VERDE'
    when 'DIFICIL' then 'ROJO'
end;

alter table questions alter column grade set not null;
alter table questions add constraint ck_questions_grade check (grade in (
    'BLANCO', 'BLANCO_AMARILLO', 'AMARILLO', 'AMARILLO_VERDE', 'VERDE', 'VERDE_AZUL',
    'AZUL', 'AZUL_ROJO', 'ROJO', 'ROJO_NEGRO', 'NEGRO'));

alter table questions drop column difficulty;

alter table questions add column question_type varchar(10)
    constraint ck_questions_type check (question_type in ('C', 'D'));
