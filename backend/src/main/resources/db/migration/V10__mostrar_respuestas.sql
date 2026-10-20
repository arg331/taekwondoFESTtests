-- RF-53: el profesor elige qué ve el alumno al entregar: nada, la nota, o la nota y sus respuestas.
-- Hasta ahora, mostrar la nota mostraba también el detalle: se conserva así en los exámenes existentes.
alter table exams add column show_answers boolean not null default false;
update exams set show_answers = show_score;
