-- OdontoSync - Issue #35: version de credencial para revocacion de acceso.
-- MySQL 8.0. Ejecutar UNA SOLA VEZ, con respaldo previo y Tomcat detenido.
-- Antes de ejecutar, confirmar que existe debe_cambiar_password y que
-- version_credencial no existe. ALTER TABLE produce commit implicito.
-- No se modifican hashes, DNI, correos, roles ni estados de activacion.
USE odontosync;

SELECT COUNT(*) AS usuarios_antes FROM usuarios;

ALTER TABLE usuarios
    ADD COLUMN version_credencial BIGINT NOT NULL DEFAULT 0
    AFTER debe_cambiar_password;

-- Verificacion posterior sin mostrar datos personales ni hashes.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuarios'
  AND COLUMN_NAME IN ('dni', 'debe_cambiar_password', 'version_credencial')
ORDER BY ORDINAL_POSITION;

SELECT COUNT(*) AS usuarios_total,
       COUNT(CASE WHEN version_credencial = 0 THEN 1 END) AS version_inicial,
       COUNT(CASE WHEN version_credencial <> 0 THEN 1 END) AS version_modificada,
       COUNT(CASE WHEN debe_cambiar_password = 0 THEN 1 END) AS cambio_no_requerido,
       COUNT(CASE WHEN debe_cambiar_password = 1 THEN 1 END) AS cambio_requerido
FROM usuarios;
