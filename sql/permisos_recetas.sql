-- Permisos mínimos para el módulo funcional de Recetas.
-- Ejecutar una sola vez como root/administrador de MySQL.

GRANT SELECT, INSERT
ON odontosync.recetas
TO 'odontosync_app'@'localhost';

GRANT SELECT
ON odontosync.pacientes
TO 'odontosync_app'@'localhost';

GRANT SELECT
ON odontosync.odontologos
TO 'odontosync_app'@'localhost';

GRANT SELECT
ON odontosync.citas
TO 'odontosync_app'@'localhost';

GRANT SELECT
ON odontosync.estados_cita
TO 'odontosync_app'@'localhost';

FLUSH PRIVILEGES;

SHOW GRANTS FOR 'odontosync_app'@'localhost';