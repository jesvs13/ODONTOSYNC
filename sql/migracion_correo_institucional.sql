-- OdontoSync - Issue #33: unicidad global del correo institucional.
-- MySQL 8.0. Ejecutar una sola vez como administrador, tras el respaldo
-- y la comprobacion de duplicados. Mantener Tomcat detenido.
-- No modifica filas, contrasenas, permisos ni indices existentes.
-- ALTER TABLE realiza commit implicito; no se puede deshacer con ROLLBACK.
-- Si existen duplicados segun la collation de correo, ALTER falla.
USE odontosync;

ALTER TABLE usuarios
    ADD CONSTRAINT uq_usuario_correo_global UNIQUE (correo);

SHOW INDEX FROM usuarios
WHERE Key_name = 'uq_usuario_correo_global';
