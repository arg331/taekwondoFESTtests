-- Cuándo se cerró a mano un examen (DE-07): los intentos empezados antes siguen hasta que se les acaba el
-- tiempo. null = no está cerrado (o se cerró antes de esta versión: entonces se rechazan, como hasta ahora).
alter table exams add column closed_at timestamp;
