-- Contraseñas (RF-06, RF-33, RF-35).
-- must_change_password: la cuenta tiene una contraseña temporal puesta por el administrador
-- y no se puede usar hasta cambiarla.
-- credentials_version: sube con cada cambio de contraseña; las sesiones emitidas con una
-- versión anterior dejan de valer (se cierran en todos los dispositivos).
alter table users add column must_change_password boolean not null default false;
alter table users add column credentials_version integer not null default 0;
