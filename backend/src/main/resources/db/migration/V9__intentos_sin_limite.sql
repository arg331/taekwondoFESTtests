-- RF-52 (prueba del 18/10/2026): los intentos por alumno son 1, 2, 3, 4 o sin límite (null).
alter table exams drop constraint if exists exams_max_attempts_check;
update exams set max_attempts = 4 where max_attempts > 4;
alter table exams alter column max_attempts drop not null;
alter table exams add constraint exams_max_attempts_check
    check (max_attempts is null or max_attempts between 1 and 4);
