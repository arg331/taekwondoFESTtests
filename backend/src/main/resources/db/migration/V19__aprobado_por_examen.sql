-- Aprobado de cada examen, en % de aciertos (DE-13, decidido el 31/10/2026): el 50 % salvo que el profesor elija
-- otro. Hasta ahora era un 70 % fijo; como aún no hay exámenes reales, todos pasan al 50 %.
alter table exams add column passing_score integer not null default 50;
