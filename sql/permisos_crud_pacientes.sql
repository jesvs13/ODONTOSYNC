-- Permisos mínimos para el CRUD administrativo de pacientes.
-- Ejecutar una sola vez con root/administrador de MySQL.

GRANT INSERT, UPDATE
ON odontosync.pacientes
TO 'odontosync_app'@'localhost';

FLUSH PRIVILEGES;

SHOW GRANTS FOR 'odontosync_app'@'localhost';
