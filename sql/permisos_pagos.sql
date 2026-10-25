-- Permisos mínimos para el módulo Pagos.
-- Ejecutar como root o como un usuario administrador de MySQL.
GRANT SELECT, INSERT ON odontosync.pagos TO 'odontosync_app'@'localhost';

-- Verificación opcional:
SHOW GRANTS FOR 'odontosync_app'@'localhost';
