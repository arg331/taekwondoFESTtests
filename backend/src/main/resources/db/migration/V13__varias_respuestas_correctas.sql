-- RF-09 (prueba del 18/10/2026): una pregunta puede tener varias respuestas correctas y el alumno
-- puede marcar varias opciones. El índice único pasa a ser un conjunto de índices.

-- Preguntas del banco
create table question_correct_options (
    question_id  bigint  not null references questions (id) on delete cascade,
    option_index integer not null,
    primary key (question_id, option_index)
);
insert into question_correct_options (question_id, option_index)
select id, correct_answer from questions;
alter table questions drop column correct_answer;

-- Copias publicadas (DE-03)
create table published_question_correct_options (
    published_question_id bigint  not null references published_questions (id) on delete cascade,
    option_index          integer not null,
    primary key (published_question_id, option_index)
);
insert into published_question_correct_options (published_question_id, option_index)
select id, correct_answer from published_questions;
alter table published_questions drop column correct_answer;

-- Respuestas de cada entrega: índices separados por comas («» = en blanco)
alter table result_answers add column student_options varchar(100);
alter table result_answers add column correct_options varchar(100);
update result_answers
set student_options = coalesce(student_answer::text, ''),
    correct_options = correct_answer::text;
alter table result_answers alter column student_options set not null;
alter table result_answers alter column correct_options set not null;
alter table result_answers drop column student_answer;
alter table result_answers drop column correct_answer;
