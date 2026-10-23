-- Ejecutar UNA VEZ en MySQL Workbench con una cuenta administradora (por ejemplo root).
-- Amplía únicamente los permisos necesarios para el primer CRUD de OdontoSync.

GRANT SELECT ON odontosync.* TO 'odontosync_app'@'localhost';
GRANT INSERT, UPDATE, DELETE ON odontosync.usuarios TO 'odontosync_app'@'localhost';
GRANT INSERT, UPDATE ON odontosync.odontologos TO 'odontosync_app'@'localhost';
FLUSH PRIVILEGES;
