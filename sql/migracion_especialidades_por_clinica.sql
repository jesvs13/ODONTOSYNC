-- OdontoSync | Issue #37 | MySQL 8.0+
-- Ejecutar UNA SOLA VEZ con una cuenta administradora, respaldo previo
-- y Tomcat detenido. ALTER/CREATE TABLE y GRANT generan commits implicitos.
-- No ejecutar el SQL de reinicio de datos demo anterior a esta migracion.
-- Conserva las especialidades, sus identificadores y todos sus vinculos.
USE odontosync;

-- 1. Linea base: guardar los resultados para compararlos al final.
SELECT (SELECT COUNT(*) FROM especialidades) AS especialidades_total,
       (SELECT COUNT(*) FROM odontologo_especialidad) AS asignaciones_total,
       (SELECT COUNT(*) FROM citas) AS citas_total,
       (SELECT COUNT(*) FROM citas WHERE id_especialidad IS NOT NULL) AS citas_con_especialidad,
       (SELECT COUNT(*) FROM historias_clinicas) AS historias_total;

-- 2. Catalogo global (id_clinica NULL) y propio (id_clinica de su titular).
-- La clave de ambito 0 representa globales: UNIQUE con NULL directamente
-- permitiria duplicados globales. Los nombres conservan su collation original
-- utf8mb4_unicode_ci del esquema base (insensible a mayusculas y acentos).
-- La normalizacion de espacios y el choque global/propio se validan en Java
-- y se volveran a comprobar en JDBC. El indice protege duplicados del mismo ambito.
ALTER TABLE especialidades
    ADD COLUMN id_clinica INT UNSIGNED NULL AFTER id_especialidad,
    ADD COLUMN ambito_clinica INT UNSIGNED
        GENERATED ALWAYS AS (IFNULL(id_clinica, 0)) STORED,
    ADD CONSTRAINT fk_especialidad_clinica
        FOREIGN KEY (id_clinica) REFERENCES clinicas(id_clinica),
    ADD UNIQUE KEY uq_especialidad_ambito_nombre (ambito_clinica, nombre),
    DROP INDEX uq_especialidad_nombre;

-- 3. Disponibilidad particular de especialidades globales.
-- Sin fila: se hereda estado de especialidades. Con fila: se usa activa.
-- Las especialidades propias mantienen su estado en especialidades.
-- JDBC debe impedir crear excepciones para especialidades propias o ajenas.
CREATE TABLE especialidad_clinica (
    id_clinica INT UNSIGNED NOT NULL,
    id_especialidad SMALLINT UNSIGNED NOT NULL,
    activa TINYINT(1) NOT NULL,
    PRIMARY KEY (id_clinica, id_especialidad),
    CONSTRAINT fk_ec_clinica FOREIGN KEY (id_clinica)
        REFERENCES clinicas(id_clinica),
    CONSTRAINT fk_ec_especialidad FOREIGN KEY (id_especialidad)
        REFERENCES especialidades(id_especialidad),
    CONSTRAINT chk_ec_activa CHECK (activa IN (0, 1))
) ENGINE=InnoDB;

-- 4. Permisos minimos para la siguiente integracion JDBC.
-- No se conceden DELETE, ALTER, DROP ni permisos administrativos.
GRANT SELECT, INSERT, UPDATE ON odontosync.especialidades
TO 'odontosync_app'@'localhost';
GRANT SELECT, INSERT, UPDATE ON odontosync.especialidad_clinica
TO 'odontosync_app'@'localhost';

-- 5. Verificaciones posteriores (solo lectura).
SHOW COLUMNS FROM especialidades;
SHOW INDEX FROM especialidades;
SHOW CREATE TABLE especialidad_clinica;

SELECT COUNT(*) AS especialidades_total,
       COUNT(CASE WHEN id_clinica IS NULL THEN 1 END) AS globales,
       COUNT(CASE WHEN id_clinica IS NOT NULL THEN 1 END) AS propias,
       (SELECT COUNT(*) FROM especialidad_clinica) AS excepciones
FROM especialidades;

SELECT c.id_clinica, c.nombre AS clinica,
       COUNT(e.id_especialidad) AS catalogo_total,
       COUNT(CASE WHEN e.id_especialidad IS NOT NULL AND
           (CASE WHEN e.id_clinica IS NULL
               THEN COALESCE(ec.activa, e.estado = 'ACTIVA')
               ELSE e.estado = 'ACTIVA' END) = 1 THEN 1 END) AS activas
FROM clinicas c
LEFT JOIN especialidades e ON (e.id_clinica IS NULL OR e.id_clinica = c.id_clinica)
LEFT JOIN especialidad_clinica ec ON ec.id_clinica = c.id_clinica
    AND ec.id_especialidad = e.id_especialidad
GROUP BY c.id_clinica, c.nombre
ORDER BY c.id_clinica;

SELECT (SELECT COUNT(*) FROM especialidades) AS especialidades_total,
       (SELECT COUNT(*) FROM odontologo_especialidad) AS asignaciones_total,
       (SELECT COUNT(*) FROM citas) AS citas_total,
       (SELECT COUNT(*) FROM citas WHERE id_especialidad IS NOT NULL) AS citas_con_especialidad,
       (SELECT COUNT(*) FROM historias_clinicas) AS historias_total;

SHOW GRANTS FOR 'odontosync_app'@'localhost';
