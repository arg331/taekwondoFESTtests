-- Rol de profesor (RF-04): el administrador deja de ser el único que puede preparar exámenes.
-- Las cuentas ADMIN existentes se quedan como están para no perder permisos.
alter table users drop constraint users_role_check;
alter table users add constraint ck_users_role check (role in ('ADMIN', 'TEACHER', 'STUDENT'));
