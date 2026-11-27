-- OdontoSync | Issue #31 | MySQL 8.0.46
-- Ejecutar una sola vez, como administrador, con la aplicacion detenida.
-- Realizar respaldo antes de ejecutar: ALTER TABLE produce commit implicito.
-- No elimina registros. Las filas anteriores quedan LEGADO hasta su regularizacion.
USE odontosync;

ALTER TABLE placas_radiograficas
    DROP FOREIGN KEY fk_placa_paciente,
    ADD COLUMN id_usuario INT UNSIGNED NULL AFTER id_paciente,
    ADD COLUMN tipo_radiografia ENUM('PANORAMICA','PERIAPICAL','BITEWING','OCLUSAL','CEFALOMETRICA','OTRA') NULL,
    ADD COLUMN fecha_radiografia DATE NULL,
    ADD COLUMN region_dental VARCHAR(100) NULL,
    ADD COLUMN observaciones TEXT NULL,
    ADD COLUMN tipo_mime ENUM('image/jpeg','image/png','application/pdf') NULL,
    ADD COLUMN tamano_bytes INT UNSIGNED NULL,
    ADD COLUMN estado ENUM('ACTIVA','ANULADA','LEGADO') NOT NULL DEFAULT 'LEGADO',
    ADD COLUMN motivo_anulacion VARCHAR(500) NULL,
    ADD COLUMN fecha_anulacion TIMESTAMP NULL DEFAULT NULL,
    ADD COLUMN id_usuario_anulacion INT UNSIGNED NULL,
    ADD INDEX idx_placa_paciente_estado_fecha (id_paciente, estado, fecha_radiografia),
    ADD INDEX idx_placa_usuario (id_usuario),
    ADD INDEX idx_placa_usuario_anulacion (id_usuario_anulacion),
    ADD CONSTRAINT fk_placa_paciente_restrict
        FOREIGN KEY (id_paciente) REFERENCES pacientes (id_paciente) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_placa_usuario_registro
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_placa_usuario_anulacion
        FOREIGN KEY (id_usuario_anulacion) REFERENCES usuarios (id_usuario) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_placa_metadatos CHECK (
        estado = 'LEGADO' OR (
            tipo_radiografia IS NOT NULL AND fecha_radiografia IS NOT NULL
            AND tipo_mime IS NOT NULL AND tamano_bytes IS NOT NULL
            AND tamano_bytes BETWEEN 1 AND 10485760
        )
    ),
    ADD CONSTRAINT chk_placa_anulacion CHECK (
        (estado IN ('ACTIVA', 'LEGADO') AND motivo_anulacion IS NULL AND fecha_anulacion IS NULL)
        OR (estado = 'ANULADA' AND motivo_anulacion IS NOT NULL
            AND CHAR_LENGTH(TRIM(motivo_anulacion)) > 0 AND fecha_anulacion IS NOT NULL)
    );

-- El DAO especializado debe insertar explicitamente ACTIVA, el usuario autenticado
-- y una clave privada rx-* en url_archivo. Nunca entregar filas LEGADO como archivos.
-- Los usuarios de registro/anulacion son obligatorios por contrato del DAO/servicio
-- para operaciones nuevas; se conservan NULL en el esquema para datos historicos.
-- La autorizacion por clinica y cita debe verificarse en cada operacion JDBC.

-- Verificaciones (solo lectura).
SHOW CREATE TABLE placas_radiograficas;
SELECT COUNT(*) AS total_placas,
       COUNT(CASE WHEN estado = 'LEGADO' THEN 1 END) AS placas_legado,
       COUNT(CASE WHEN estado = 'ACTIVA' THEN 1 END) AS placas_activas,
       COUNT(CASE WHEN estado = 'ANULADA' THEN 1 END) AS placas_anuladas
FROM placas_radiograficas;
