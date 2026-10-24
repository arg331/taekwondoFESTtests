-- Registro con email opcional (RF-01). El índice único se mantiene:
-- PostgreSQL permite varios usuarios con email null.
alter table users alter column email drop not null;
