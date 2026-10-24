-- Preguntas eliminadas: salen del banco pero no se borran, porque los exámenes publicados las siguen
-- usando. Se pueden recuperar. null = en el banco.
alter table questions add column deleted_at timestamp;
