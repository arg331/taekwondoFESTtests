-- RN-08 (decisión del 18/10/2026): un anónimo se reconoce por su nombre sin distinguir mayúsculas,
-- tildes ni espacios de más. La aplicación calcula la clave (StudentIdentity.nameKey); aquí se rellena
-- la de los intentos existentes con las letras con tilde del español.
alter table exam_attempts add column student_key varchar(100);

update exam_attempts
set student_key = regexp_replace(
        trim(translate(lower(student_name),
                       'áàäâãéèëêíìïîóòöôõúùüûñç',
                       'aaaaaeeeeiiiiooooouuuunc')),
        '\s+', ' ', 'g');

alter table exam_attempts alter column student_key set not null;
create index idx_exam_attempts_exam_key on exam_attempts (exam_id, student_key);
