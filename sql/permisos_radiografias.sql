-- OdontoSync | Issue #31 | ejecutar despues de migracion_radiografias.sql
-- Ejecutar como administrador de MySQL. No crea cuentas ni cambia contrasenas.
-- Cuenta de aplicacion utilizada por los scripts existentes del proyecto.
SHOW GRANTS FOR 'odontosync_app'@'localhost';

GRANT SELECT, INSERT ON odontosync.placas_radiograficas
TO 'odontosync_app'@'localhost';

GRANT UPDATE (estado, motivo_anulacion, fecha_anulacion, id_usuario_anulacion)
ON odontosync.placas_radiograficas TO 'odontosync_app'@'localhost';

-- SELECT de tablas relacionadas: la cuenta ya dispone de estas lecturas segun los
-- scripts existentes; se explicitan para el nuevo DAO, sin conceder escrituras.
GRANT SELECT ON odontosync.pacientes TO 'odontosync_app'@'localhost';
GRANT SELECT ON odontosync.odontologos TO 'odontosync_app'@'localhost';
GRANT SELECT ON odontosync.citas TO 'odontosync_app'@'localhost';

-- No se concede DELETE, ALTER, DROP, ni UPDATE general de radiografias.
-- Los GRANT son acumulativos: revisar que no existan privilegios previos mas amplios.
-- La autorizacion por clinica/odontologo corresponde al DAO y al servicio Java.
SHOW GRANTS FOR 'odontosync_app'@'localhost';
