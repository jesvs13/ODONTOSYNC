-- Ejecutar una sola vez con root/administrador de MySQL.
-- El usuario técnico ya tiene SELECT sobre odontosync.*; Agenda solo necesita alta y actualización.

GRANT INSERT, UPDATE
ON odontosync.citas
TO 'odontosync_app'@'localhost';

FLUSH PRIVILEGES;

-- Verificación opcional:
-- SHOW GRANTS FOR 'odontosync_app'@'localhost';
