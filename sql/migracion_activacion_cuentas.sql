-- OdontoSync - Issue #35: DNI y activacion de cuentas.
-- MySQL 8.0. Ejecutar UNA SOLA VEZ como administrador, con Tomcat
-- detenido y respaldo previo de la base. ALTER TABLE hace commit implicito.
-- No se modifican contrasenas, correos, roles ni estados de cuentas existentes.
USE odontosync;

-- Comprobacion previa: ambas columnas deben estar ausentes antes de ejecutar
-- el ALTER. Si alguna ya existe, detenerse y revisar el esquema.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuarios'
  AND COLUMN_NAME IN ('dni', 'debe_cambiar_password');

SELECT COUNT(*) AS usuarios_antes FROM usuarios;

ALTER TABLE usuarios
    ADD COLUMN dni VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL AFTER apellidos,
    ADD COLUMN debe_cambiar_password TINYINT(1) NOT NULL DEFAULT 0 AFTER password_hash;

-- Verificacion posterior (sin exponer datos personales ni hashes).
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuarios'
  AND COLUMN_NAME IN ('dni', 'debe_cambiar_password')
ORDER BY ORDINAL_POSITION;

SELECT COUNT(*) AS usuarios_total,
       COUNT(CASE WHEN dni IS NULL THEN 1 END) AS usuarios_sin_dni,
       COUNT(CASE WHEN debe_cambiar_password = 0 THEN 1 END) AS cambio_no_requerido,
       COUNT(CASE WHEN debe_cambiar_password = 1 THEN 1 END) AS cambio_requerido
FROM usuarios;
