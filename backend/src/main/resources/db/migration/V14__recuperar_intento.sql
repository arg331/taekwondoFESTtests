-- RF-24 y DE-05: el intento guarda qué preguntas vio el alumno, en qué orden y con qué orden de
-- opciones, y lo que va contestando. Así recargar la página o perder la conexión no borra el examen.
create table attempt_questions (
    attempt_id     bigint       not null references exam_attempts (id) on delete cascade,
    position       integer      not null,
    question_id    bigint       not null,
    -- Índices originales de las opciones, en el orden en que se le muestran: «2,0,3,1»
    option_order   varchar(100) not null,
    -- Opciones marcadas; «» = sin responder
    chosen_options varchar(100) not null default '',
    primary key (attempt_id, position)
);

-- Un intento que caduca sin entregar se corrige solo con lo guardado
alter table results add column auto_submitted boolean not null default false;
