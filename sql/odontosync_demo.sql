-- ============================================================
-- ODONTOSYNC - BASE DE DATOS PARA CLÍNICAS ODONTOLÓGICAS
-- MySQL 8.0+
-- Modelo relacional normalizado (3FN) - VERSIÓN FINAL B2B + PAGOS
-- ============================================================

-- Ejecutar solo sobre una base nueva de pruebas; no elimina datos existentes.
CREATE DATABASE IF NOT EXISTS odontosync
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE odontosync;

-- ============================================================
-- 1. TABLAS MAESTRAS
-- ============================================================

CREATE TABLE clinicas (
    id_clinica INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    ruc VARCHAR(11) NULL,
    direccion VARCHAR(255) NULL,
    telefono VARCHAR(20) NULL,
    correo VARCHAR(150) NULL,
    estado ENUM('ACTIVA','INACTIVA') NOT NULL DEFAULT 'ACTIVA',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_clinica_ruc UNIQUE (ruc)
) ENGINE=InnoDB;

CREATE TABLE roles (
    id_rol TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255) NULL,
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE estados_cita (
    id_estado_cita TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255) NULL,
    CONSTRAINT uq_estado_cita_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE especialidades (
    id_especialidad SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NULL,
    estado ENUM('ACTIVA','INACTIVA') NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT uq_especialidad_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE categorias_insumos (
    id_categoria SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NULL,
    estado ENUM('ACTIVA','INACTIVA') NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT uq_categoria_insumo_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- ============================================================
-- 2. USUARIOS
-- ============================================================

CREATE TABLE usuarios (
    id_usuario INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_clinica INT UNSIGNED NOT NULL,
    id_rol TINYINT UNSIGNED NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    telefono VARCHAR(20) NULL,
    estado ENUM('ACTIVO','INACTIVO','BLOQUEADO') NOT NULL DEFAULT 'ACTIVO',
    ultimo_acceso DATETIME NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_usuario_clinica
        FOREIGN KEY (id_clinica) REFERENCES clinicas(id_clinica),
    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol) REFERENCES roles(id_rol),
    CONSTRAINT uq_usuario_clinica_correo
        UNIQUE (id_clinica, correo),
    INDEX idx_usuario_rol (id_rol)
) ENGINE=InnoDB;

-- ============================================================
-- 3. PACIENTES Y ODONTÓLOGOS
-- ============================================================

CREATE TABLE pacientes (
    id_paciente INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT UNSIGNED NULL,
    id_clinica INT UNSIGNED NOT NULL,
    tipo_documento ENUM('DNI','CE','PASAPORTE','OTRO') NOT NULL DEFAULT 'DNI',
    numero_documento VARCHAR(20) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    sexo ENUM('M','F','OTRO') NULL,
    direccion VARCHAR(255) NULL,
    telefono VARCHAR(20) NULL,
    correo VARCHAR(150) NULL,
    ocupacion VARCHAR(100) NULL,
    contacto_emergencia VARCHAR(150) NULL,
    telefono_emergencia VARCHAR(20) NULL,
    estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_paciente_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE SET NULL,
    CONSTRAINT fk_paciente_clinica
        FOREIGN KEY (id_clinica) REFERENCES clinicas(id_clinica),
    CONSTRAINT uq_paciente_documento
        UNIQUE (id_clinica, tipo_documento, numero_documento),
    CONSTRAINT uq_paciente_usuario
        UNIQUE (id_usuario),
    INDEX idx_paciente_nombre (apellidos, nombres)
) ENGINE=InnoDB;

CREATE TABLE odontologos (
    id_odontologo INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT UNSIGNED NULL,
    id_clinica INT UNSIGNED NOT NULL,
    numero_colegiatura VARCHAR(50) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NULL,
    correo VARCHAR(150) NULL,
    fecha_ingreso DATE NULL,
    estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_odontologo_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE SET NULL,
    CONSTRAINT fk_odontologo_clinica
        FOREIGN KEY (id_clinica) REFERENCES clinicas(id_clinica),
    CONSTRAINT uq_odontologo_colegiatura
        UNIQUE (id_clinica, numero_colegiatura),
    CONSTRAINT uq_odontologo_usuario
        UNIQUE (id_usuario),
    INDEX idx_odontologo_nombre (apellidos, nombres)
) ENGINE=InnoDB;

CREATE TABLE odontologo_especialidad (
    id_odontologo INT UNSIGNED NOT NULL,
    id_especialidad SMALLINT UNSIGNED NOT NULL,
    fecha_asignacion DATE NOT NULL DEFAULT (CURRENT_DATE),

    PRIMARY KEY (id_odontologo, id_especialidad),

    CONSTRAINT fk_oe_odontologo
        FOREIGN KEY (id_odontologo) REFERENCES odontologos(id_odontologo)
        ON DELETE CASCADE,
    CONSTRAINT fk_oe_especialidad
        FOREIGN KEY (id_especialidad) REFERENCES especialidades(id_especialidad)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 4. HORARIOS Y BLOQUEOS
-- ============================================================

CREATE TABLE horarios (
    id_horario INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_odontologo INT UNSIGNED NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL COMMENT '1=Lunes ... 7=Domingo',
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_horario_odontologo
        FOREIGN KEY (id_odontologo) REFERENCES odontologos(id_odontologo)
        ON DELETE CASCADE,
    CONSTRAINT chk_horario_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_horario_horas CHECK (hora_inicio < hora_fin),
    INDEX idx_horario_odontologo_dia (id_odontologo, dia_semana, hora_inicio)
) ENGINE=InnoDB;

CREATE TABLE bloqueos_agenda (
    id_bloqueo INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_odontologo INT UNSIGNED NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NULL,
    hora_fin TIME NULL,
    motivo VARCHAR(255) NOT NULL,
    estado ENUM('ACTIVO','CANCELADO') NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bloqueo_odontologo
        FOREIGN KEY (id_odontologo) REFERENCES odontologos(id_odontologo)
        ON DELETE CASCADE,
    CONSTRAINT chk_bloqueo_horas
        CHECK (
            (hora_inicio IS NULL AND hora_fin IS NULL)
            OR (hora_inicio IS NOT NULL AND hora_fin IS NOT NULL AND hora_inicio < hora_fin)
        ),
    INDEX idx_bloqueo_agenda (id_odontologo, fecha, hora_inicio, hora_fin)
) ENGINE=InnoDB;

-- ============================================================
-- 5. CITAS
-- ============================================================

CREATE TABLE citas (
    id_cita INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_clinica INT UNSIGNED NOT NULL,
    id_paciente INT UNSIGNED NOT NULL,
    id_odontologo INT UNSIGNED NOT NULL,
    id_especialidad SMALLINT UNSIGNED NULL,
    id_horario INT UNSIGNED NULL,
    id_estado_cita TINYINT UNSIGNED NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    motivo VARCHAR(255) NULL,
    observaciones TEXT NULL,
    creado_por INT UNSIGNED NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_cita_clinica
        FOREIGN KEY (id_clinica) REFERENCES clinicas(id_clinica),
    CONSTRAINT fk_cita_paciente
        FOREIGN KEY (id_paciente) REFERENCES pacientes(id_paciente),
    CONSTRAINT fk_cita_odontologo
        FOREIGN KEY (id_odontologo) REFERENCES odontologos(id_odontologo),
    CONSTRAINT fk_cita_especialidad
        FOREIGN KEY (id_especialidad) REFERENCES especialidades(id_especialidad)
        ON DELETE SET NULL,
    CONSTRAINT fk_cita_horario
        FOREIGN KEY (id_horario) REFERENCES horarios(id_horario)
        ON DELETE SET NULL,
    CONSTRAINT fk_cita_estado
        FOREIGN KEY (id_estado_cita) REFERENCES estados_cita(id_estado_cita),
    CONSTRAINT fk_cita_creado_por
        FOREIGN KEY (creado_por) REFERENCES usuarios(id_usuario)
        ON DELETE SET NULL,

    CONSTRAINT chk_cita_horas CHECK (hora_inicio < hora_fin),

    INDEX idx_cita_odontologo_fecha (id_odontologo, fecha, hora_inicio),
    INDEX idx_cita_paciente_fecha (id_paciente, fecha, hora_inicio),
    INDEX idx_cita_estado (id_estado_cita),
    INDEX idx_cita_clinica_fecha (id_clinica, fecha)
) ENGINE=InnoDB;

-- ============================================================
-- 6. HISTORIA CLÍNICA, PLACAS, RECETAS Y PAGOS
-- ============================================================

CREATE TABLE historias_clinicas (
    id_historia INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT UNSIGNED NOT NULL,
    fecha_apertura DATE NOT NULL,
    observaciones_generales TEXT NULL,
    estado ENUM('ACTIVA','CERRADA') NOT NULL DEFAULT 'ACTIVA',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historia_paciente
        FOREIGN KEY (id_paciente) REFERENCES pacientes(id_paciente)
        ON DELETE CASCADE,
    CONSTRAINT uq_historia_paciente UNIQUE (id_paciente)
) ENGINE=InnoDB;

CREATE TABLE antecedentes_medicos (
    id_antecedente INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT UNSIGNED NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    observaciones TEXT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_antecedente_paciente
        FOREIGN KEY (id_paciente) REFERENCES pacientes(id_paciente)
        ON DELETE CASCADE,
    INDEX idx_antecedente_paciente (id_paciente)
) ENGINE=InnoDB;

CREATE TABLE alergias (
    id_alergia INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT UNSIGNED NOT NULL,
    alergeno VARCHAR(150) NOT NULL,
    reaccion VARCHAR(255) NULL,
    observaciones TEXT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_alergia_paciente
        FOREIGN KEY (id_paciente) REFERENCES pacientes(id_paciente)
        ON DELETE CASCADE,
    INDEX idx_alergia_paciente (id_paciente)
) ENGINE=InnoDB;

CREATE TABLE consultas (
    id_consulta INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_cita INT UNSIGNED NOT NULL,
    id_odontologo INT UNSIGNED NOT NULL,
    fecha_consulta DATETIME NOT NULL,
    motivo_consulta TEXT NULL,
    diagnostico TEXT NULL,
    tratamiento TEXT NULL,
    observaciones TEXT NULL,
    recomendaciones TEXT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_consulta_cita
        FOREIGN KEY (id_cita) REFERENCES citas(id_cita),
    CONSTRAINT fk_consulta_odontologo
        FOREIGN KEY (id_odontologo) REFERENCES odontologos(id_odontologo),
    CONSTRAINT uq_consulta_cita UNIQUE (id_cita),
    INDEX idx_consulta_odontologo_fecha (id_odontologo, fecha_consulta)
) ENGINE=InnoDB;

CREATE TABLE recetas (
    id_receta INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT UNSIGNED NOT NULL,
    id_odontologo INT UNSIGNED NOT NULL,
    medicamentos TEXT NOT NULL,
    indicaciones TEXT NOT NULL,
    fecha_emision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_receta_paciente
        FOREIGN KEY (id_paciente) REFERENCES pacientes(id_paciente)
        ON DELETE CASCADE,
    CONSTRAINT fk_receta_odontologo
        FOREIGN KEY (id_odontologo) REFERENCES odontologos(id_odontologo)
) ENGINE=InnoDB;

CREATE TABLE placas_radiograficas (
    id_placa INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT UNSIGNED NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    url_archivo VARCHAR(255) NOT NULL,
    fecha_subida TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_placa_paciente
        FOREIGN KEY (id_paciente) REFERENCES pacientes(id_paciente)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE pagos (
    id_pago INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_cita INT UNSIGNED NOT NULL,
    id_usuario INT UNSIGNED NOT NULL COMMENT 'Usuario que registra el pago',
    monto DECIMAL(10,2) NOT NULL,
    metodo_pago ENUM('EFECTIVO', 'YAPE', 'PLIN', 'TRANSFERENCIA', 'TARJETA') NOT NULL,
    url_comprobante VARCHAR(255) NULL COMMENT 'Ruta de la captura del voucher',
    fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observaciones TEXT NULL,

    CONSTRAINT fk_pago_cita        FOREIGN KEY (id_cita) REFERENCES citas(id_cita)
        ON DELETE CASCADE,
    CONSTRAINT fk_pago_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

-- ============================================================
-- 7. INVENTARIO
-- ============================================================

CREATE TABLE insumos (
    id_insumo INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_clinica INT UNSIGNED NOT NULL,
    id_categoria SMALLINT UNSIGNED NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255) NULL,
    unidad_medida VARCHAR(30) NOT NULL,
    stock_actual DECIMAL(10,2) NOT NULL DEFAULT 0,
    stock_minimo DECIMAL(10,2) NOT NULL DEFAULT 0,
    precio_referencial DECIMAL(10,2) NULL,
    estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_insumo_clinica
        FOREIGN KEY (id_clinica) REFERENCES clinicas(id_clinica),
    CONSTRAINT fk_insumo_categoria
        FOREIGN KEY (id_categoria) REFERENCES categorias_insumos(id_categoria),
    CONSTRAINT uq_insumo_clinica_nombre UNIQUE (id_clinica, nombre),
    CONSTRAINT chk_insumo_stock CHECK (stock_actual >= 0),
    CONSTRAINT chk_insumo_minimo CHECK (stock_minimo >= 0),
    CONSTRAINT chk_insumo_precio CHECK (precio_referencial IS NULL OR precio_referencial >= 0)
) ENGINE=InnoDB;

CREATE TABLE movimientos_inventario (
    id_movimiento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_insumo INT UNSIGNED NOT NULL,
    id_usuario INT UNSIGNED NOT NULL,
    tipo ENUM('ENTRADA','SALIDA','AJUSTE') NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    fecha_movimiento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movimiento_insumo
        FOREIGN KEY (id_insumo) REFERENCES insumos(id_insumo),
    CONSTRAINT fk_movimiento_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    CONSTRAINT chk_movimiento_cantidad CHECK (cantidad > 0),
    INDEX idx_movimiento_insumo_fecha (id_insumo, fecha_movimiento)
) ENGINE=InnoDB;

-- ============================================================
-- 8. NOTIFICACIONES
-- ============================================================

CREATE TABLE notificaciones (
    id_notificacion INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT UNSIGNED NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje TEXT NOT NULL,
    tipo VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_lectura DATETIME NULL,

    CONSTRAINT fk_notificacion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE,
    INDEX idx_notificacion_usuario_leida (id_usuario, leida, fecha_creacion)
) ENGINE=InnoDB;

-- ============================================================
-- 9. TRIGGERS: VALIDACIONES DE NEGOCIO
-- ============================================================

DELIMITER $$

CREATE TRIGGER trg_cita_validar_clinica_bi
BEFORE INSERT ON citas
FOR EACH ROW
BEGIN
    DECLARE v_clinica_paciente INT;
    DECLARE v_clinica_odontologo INT;

    SELECT id_clinica INTO v_clinica_paciente
    FROM pacientes
    WHERE id_paciente = NEW.id_paciente;

    SELECT id_clinica INTO v_clinica_odontologo
    FROM odontologos
    WHERE id_odontologo = NEW.id_odontologo;

    IF v_clinica_paciente IS NULL OR v_clinica_odontologo IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Paciente u odontologo no encontrado.';
    END IF;

    IF v_clinica_paciente <> NEW.id_clinica
       OR v_clinica_odontologo <> NEW.id_clinica THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Paciente y odontologo deben pertenecer a la misma clinica.';
    END IF;

    IF NEW.fecha < CURRENT_DATE THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se pueden registrar citas en fechas pasadas.';
    END IF;
END$$

CREATE TRIGGER trg_cita_validar_clinica_bu
BEFORE UPDATE ON citas
FOR EACH ROW
BEGIN
    DECLARE v_clinica_paciente INT;
    DECLARE v_clinica_odontologo INT;

    SELECT id_clinica INTO v_clinica_paciente
    FROM pacientes
    WHERE id_paciente = NEW.id_paciente;

    SELECT id_clinica INTO v_clinica_odontologo
    FROM odontologos
    WHERE id_odontologo = NEW.id_odontologo;

    IF v_clinica_paciente IS NULL OR v_clinica_odontologo IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Paciente u odontologo no encontrado.';
    END IF;

    IF v_clinica_paciente <> NEW.id_clinica
       OR v_clinica_odontologo <> NEW.id_clinica THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Paciente y odontologo deben pertenecer a la misma clinica.';
    END IF;
END$$

CREATE TRIGGER trg_cita_no_cruce_bi
BEFORE INSERT ON citas
FOR EACH ROW
BEGIN
    DECLARE v_conflictos INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_conflictos
    FROM citas c
    JOIN estados_cita e ON e.id_estado_cita = c.id_estado_cita
    WHERE c.id_odontologo = NEW.id_odontologo
      AND c.fecha = NEW.fecha
      AND e.nombre NOT IN ('CANCELADA','NO_ASISTIO')
      AND NEW.hora_inicio < c.hora_fin
      AND NEW.hora_fin > c.hora_inicio;

    IF v_conflictos > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El odontologo ya tiene una cita en ese horario.';
    END IF;
END$$

CREATE TRIGGER trg_cita_no_cruce_bu
BEFORE UPDATE ON citas
FOR EACH ROW
BEGIN
    DECLARE v_conflictos INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_conflictos
    FROM citas c
    JOIN estados_cita e ON e.id_estado_cita = c.id_estado_cita
    WHERE c.id_cita <> NEW.id_cita
      AND c.id_odontologo = NEW.id_odontologo
      AND c.fecha = NEW.fecha
      AND e.nombre NOT IN ('CANCELADA','NO_ASISTIO')
      AND NEW.hora_inicio < c.hora_fin
      AND NEW.hora_fin > c.hora_inicio;

    IF v_conflictos > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El odontologo ya tiene una cita en ese horario.';
    END IF;
END$$

CREATE TRIGGER trg_paciente_no_cruce_bi
BEFORE INSERT ON citas
FOR EACH ROW
BEGIN
    DECLARE v_conflictos INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_conflictos
    FROM citas c
    JOIN estados_cita e ON e.id_estado_cita = c.id_estado_cita
    WHERE c.id_paciente = NEW.id_paciente
      AND c.fecha = NEW.fecha
      AND e.nombre NOT IN ('CANCELADA','NO_ASISTIO')
      AND NEW.hora_inicio < c.hora_fin
      AND NEW.hora_fin > c.hora_inicio;

    IF v_conflictos > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El paciente ya tiene una cita en ese horario.';
    END IF;
END$$

CREATE TRIGGER trg_paciente_no_cruce_bu
BEFORE UPDATE ON citas
FOR EACH ROW
BEGIN
    DECLARE v_conflictos INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_conflictos
    FROM citas c
    JOIN estados_cita e ON e.id_estado_cita = c.id_estado_cita
    WHERE c.id_cita <> NEW.id_cita
      AND c.id_paciente = NEW.id_paciente
      AND c.fecha = NEW.fecha
      AND e.nombre NOT IN ('CANCELADA','NO_ASISTIO')
      AND NEW.hora_inicio < c.hora_fin
      AND NEW.hora_fin > c.hora_inicio;

    IF v_conflictos > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El paciente ya tiene una cita en ese horario.';
    END IF;
END$$

CREATE TRIGGER trg_cita_bloqueo_bi
BEFORE INSERT ON citas
FOR EACH ROW
BEGIN
    DECLARE v_bloqueos INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_bloqueos
    FROM bloqueos_agenda b
    WHERE b.id_odontologo = NEW.id_odontologo
      AND b.fecha = NEW.fecha
      AND b.estado = 'ACTIVO'
      AND (
          (b.hora_inicio IS NULL AND b.hora_fin IS NULL)
          OR (NEW.hora_inicio < b.hora_fin AND NEW.hora_fin > b.hora_inicio)
      );

    IF v_bloqueos > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El horario seleccionado esta bloqueado para el odontologo.';
    END IF;
END$$

CREATE TRIGGER trg_cita_bloqueo_bu
BEFORE UPDATE ON citas
FOR EACH ROW
BEGIN
    DECLARE v_bloqueos INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_bloqueos
    FROM bloqueos_agenda b
    WHERE b.id_odontologo = NEW.id_odontologo
      AND b.fecha = NEW.fecha
      AND b.estado = 'ACTIVO'
      AND (
          (b.hora_inicio IS NULL AND b.hora_fin IS NULL)
          OR (NEW.hora_inicio < b.hora_fin AND NEW.hora_fin > b.hora_inicio)
      );

    IF v_bloqueos > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El horario seleccionado esta bloqueado para el odontologo.';
    END IF;
END$$

CREATE TRIGGER trg_movimiento_inventario_ai
AFTER INSERT ON movimientos_inventario
FOR EACH ROW
BEGIN
    DECLARE v_stock DECIMAL(10,2);

    SELECT stock_actual INTO v_stock
    FROM insumos
    WHERE id_insumo = NEW.id_insumo
    FOR UPDATE;

    IF NEW.tipo = 'ENTRADA' THEN
        UPDATE insumos
        SET stock_actual = stock_actual + NEW.cantidad
        WHERE id_insumo = NEW.id_insumo;

    ELSEIF NEW.tipo = 'SALIDA' THEN
        IF v_stock < NEW.cantidad THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Stock insuficiente para realizar la salida.';
        END IF;

        UPDATE insumos
        SET stock_actual = stock_actual - NEW.cantidad
        WHERE id_insumo = NEW.id_insumo;

    ELSEIF NEW.tipo = 'AJUSTE' THEN
        UPDATE insumos
        SET stock_actual = NEW.cantidad
        WHERE id_insumo = NEW.id_insumo;
    END IF;
END$$

DELIMITER ;

-- ============================================================
-- 10. DATOS INICIALES
-- ============================================================

INSERT INTO clinicas
(nombre, ruc, direccion, telefono, correo)
VALUES
('Clínica Odontológica Sonrisa Salud',
 '20601234567',
 'Jr. Principal 245, Bagua',
 '999111222',
 'contacto@sonrisasalud.pe');

INSERT INTO roles (nombre, descripcion) VALUES
('ADMINISTRADOR', 'Administra la clínica y usuarios'),
('ODONTOLOGO', 'Gestiona atención odontológica, horarios y citas'),
('RECEPCIONISTA', 'Gestiona pacientes, citas y pagos');

INSERT INTO estados_cita (nombre, descripcion) VALUES
('PENDIENTE', 'Cita registrada pendiente de confirmación'),
('CONFIRMADA', 'Cita confirmada'),
('ATENDIDA', 'Paciente atendido'),
('CANCELADA', 'Cita cancelada'),
('NO_ASISTIO', 'Paciente no asistió');

INSERT INTO especialidades (nombre, descripcion) VALUES
('Odontología General', 'Atención odontológica general'),
('Ortodoncia', 'Prevención y corrección de alteraciones de posición dental'),
('Endodoncia', 'Tratamiento de conductos radiculares'),
('Periodoncia', 'Prevención y tratamiento de enfermedades periodontales'),
('Odontopediatría', 'Atención odontológica para niños'),
('Cirugía Oral', 'Procedimientos quirúrgicos odontológicos');

INSERT INTO categorias_insumos (nombre, descripcion) VALUES
('Bioseguridad', 'Materiales para protección y bioseguridad'),
('Anestesia', 'Materiales relacionados con anestesia odontológica'),
('Restauración', 'Materiales para restauraciones'),
('Limpieza', 'Materiales para limpieza y profilaxis'),
('Instrumental', 'Instrumental odontológico'),
('Descartables', 'Materiales descartables');

-- ============================================================
-- 11. USUARIOS DE PRUEBA
-- ============================================================
INSERT INTO usuarios
(id_clinica, id_rol, nombres, apellidos, correo, password_hash, telefono)
VALUES
(1, 1, 'Nicolas', 'Meza Cueva', 'admin@odontosync.pe',
 'pbkdf2_sha256$310000$2WQz3TgkJq8dabYf5ylFxA==$ZVYDVwH1S8qZjNP8qI9Hv/HRJ7WXyNOOagpB2pDkS2c=', '999000001'),
(1, 2, 'Carlos', 'Ramirez Torres', 'carlos.odontologo@odontosync.pe',
 'pbkdf2_sha256$310000$YrhP+AvVkz+W19oowM6GNw==$xU/RLCQ76xm0hBXtxPtelVucZ6f00+diwA3i04BFzIA=', '999000002'),
(1, 3, 'Maria', 'Lopez Diaz', 'recepcion@odontosync.pe',
 'pbkdf2_sha256$310000$1FbWcWUn6+HdHN9IKb8P0g==$vbJ6Qz7fGSgvGcHhGmFTnrxbpdlN1p0EkvAUKx4Sfto=', '999000003');

INSERT INTO pacientes
(id_usuario, id_clinica, tipo_documento, numero_documento, nombres, apellidos,
 fecha_nacimiento, sexo, direccion, telefono, correo, ocupacion)
VALUES
(NULL, 1, 'DNI', '70123456', 'Ana', 'Garcia Silva',
 '2001-05-12', 'F', 'Jr. Los Andes 123', '999000004',
 'ana.paciente@odontosync.pe', 'Estudiante'),
(NULL, 1, 'DNI', '70876543', 'Luis', 'Perez Castro',
 '1998-09-20', 'M', 'Av. Amazonas 456', '999000005',
 'luis.paciente@odontosync.pe', 'Ingeniero');

INSERT INTO odontologos
(id_usuario, id_clinica, numero_colegiatura, nombres, apellidos,
 telefono, correo, fecha_ingreso)
VALUES
(2, 1, 'COP-12345', 'Carlos', 'Ramirez Torres',
 '999000002', 'carlos.odontologo@odontosync.pe', '2026-01-10');

INSERT INTO odontologo_especialidad
(id_odontologo, id_especialidad)
VALUES
(1, 1),
(1, 2);

-- ============================================================
-- 12. HORARIOS DE PRUEBA
-- ============================================================

INSERT INTO horarios
(id_odontologo, dia_semana, hora_inicio, hora_fin)
VALUES
(1, 1, '08:00:00', '12:00:00'),
(1, 1, '15:00:00', '19:00:00'),
(1, 2, '08:00:00', '12:00:00'),
(1, 2, '15:00:00', '19:00:00'),
(1, 3, '08:00:00', '12:00:00'),
(1, 4, '08:00:00', '12:00:00'),
(1, 5, '15:00:00', '19:00:00'),
(1, 6, '08:00:00', '12:00:00');

-- ============================================================
-- 13. HISTORIAS CLÍNICAS DE PRUEBA
-- ============================================================

INSERT INTO historias_clinicas
(id_paciente, fecha_apertura, observaciones_generales)
VALUES
(1, CURRENT_DATE, 'Paciente sin observaciones generales registradas.'),
(2, CURRENT_DATE, 'Paciente con controles odontológicos periódicos.');

INSERT INTO antecedentes_medicos
(id_paciente, tipo, descripcion, observaciones)
VALUES
(1, 'Antecedente familiar', 'Sin antecedentes relevantes declarados', NULL),
(2, 'Médico', 'Sin enfermedades crónicas declaradas', NULL);

INSERT INTO alergias
(id_paciente, alergeno, reaccion, observaciones)
VALUES
(1, 'Penicilina', 'Erupción cutánea', 'Paciente refiere alergia desde la infancia.');

-- ============================================================
-- 14. INSUMOS DE PRUEBA
-- ============================================================

INSERT INTO insumos
(id_clinica, id_categoria, nombre, descripcion, unidad_medida,
 stock_actual, stock_minimo, precio_referencial)
VALUES
(1, 1, 'Guantes de nitrilo', 'Guantes descartables para atención', 'caja',
 20, 5, 25.00),
(1, 1, 'Mascarillas quirúrgicas', 'Mascarillas descartables', 'caja',
 15, 5, 18.00),
(1, 2, 'Anestesia dental', 'Cartuchos de anestesia local', 'caja',
 10, 3, 45.00),
(1, 3, 'Resina compuesta', 'Material restaurador', 'unidad',
 12, 4, 35.00),
(1, 6, 'Vasos descartables', 'Vasos para atención odontológica', 'paquete',
 25, 5, 12.00);

-- ============================================================
-- 15. MOVIMIENTOS DE INVENTARIO DE PRUEBA
-- ============================================================

INSERT INTO movimientos_inventario
(id_insumo, id_usuario, tipo, cantidad, motivo)
VALUES
(1, 1, 'ENTRADA', 10, 'Compra de guantes'),
(2, 1, 'SALIDA', 2, 'Uso en atención'),
(3, 1, 'ENTRADA', 5, 'Reposición de anestesia');

-- ============================================================
-- 16. BLOQUEO DE AGENDA DE PRUEBA
-- ============================================================

INSERT INTO bloqueos_agenda
(id_odontologo, fecha, hora_inicio, hora_fin, motivo)
VALUES
(1, DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY),
 '12:00:00', '13:00:00', 'Reunión administrativa');

-- ============================================================
-- 17. CITAS DE PRUEBA
-- ============================================================

INSERT INTO citas
(id_clinica, id_paciente, id_odontologo, id_especialidad, id_horario,
 id_estado_cita, fecha, hora_inicio, hora_fin, motivo, observaciones, creado_por)
VALUES
(1, 1, 1, 1, NULL, 2,
 DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY),
 '09:00:00', '09:45:00',
 'Consulta general', 'Evaluación inicial', 3);

INSERT INTO citas
(id_clinica, id_paciente, id_odontologo, id_especialidad, id_horario,
 id_estado_cita, fecha, hora_inicio, hora_fin, motivo, observaciones, creado_por)
VALUES
(1, 2, 1, 2, NULL, 1,
 DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY),
 '10:00:00', '11:00:00',
 'Evaluación de ortodoncia', 'Primera evaluación', 3);

-- ============================================================
-- 18. VISTAS Y OTRAS CONSULTAS
-- ============================================================

CREATE VIEW vw_pacientes AS
SELECT
    p.id_paciente,
    p.id_clinica,
    p.tipo_documento,
    p.numero_documento,
    CONCAT(p.nombres, ' ', p.apellidos) AS paciente,
    p.fecha_nacimiento,
    p.sexo,
    p.telefono,
    p.correo,
    p.estado,
    p.fecha_registro
FROM pacientes p;

CREATE VIEW vw_odontologos AS
SELECT
    o.id_odontologo,
    o.id_clinica,
    o.numero_colegiatura,
    CONCAT(o.nombres, ' ', o.apellidos) AS odontologo,
    GROUP_CONCAT(e.nombre ORDER BY e.nombre SEPARATOR ', ') AS especialidades,
    o.telefono,
    o.correo,
    o.estado
FROM odontologos o
LEFT JOIN odontologo_especialidad oe
    ON oe.id_odontologo = o.id_odontologo
LEFT JOIN especialidades e
    ON e.id_especialidad = oe.id_especialidad
GROUP BY
    o.id_odontologo, o.id_clinica, o.numero_colegiatura,
    o.nombres, o.apellidos, o.telefono, o.correo, o.estado;

CREATE VIEW vw_citas AS
SELECT
    c.id_cita,
    c.id_clinica,
    c.fecha,
    c.hora_inicio,
    c.hora_fin,
    CONCAT(p.nombres, ' ', p.apellidos) AS paciente,
    p.numero_documento,
    CONCAT(o.nombres, ' ', o.apellidos) AS odontologo,
    e.nombre AS especialidad,
    ec.nombre AS estado,
    c.motivo,
    c.observaciones
FROM citas c
INNER JOIN pacientes p ON p.id_paciente = c.id_paciente
INNER JOIN odontologos o ON o.id_odontologo = c.id_odontologo
LEFT JOIN especialidades e ON e.id_especialidad = c.id_especialidad
INNER JOIN estados_cita ec ON ec.id_estado_cita = c.id_estado_cita;

CREATE VIEW vw_inventario AS
SELECT
    i.id_insumo,
    i.id_clinica,
    i.nombre AS insumo,
    ci.nombre AS categoria,
    i.unidad_medida,
    i.stock_actual,
    i.stock_minimo,
    i.precio_referencial,
    CASE
        WHEN i.stock_actual <= i.stock_minimo THEN 'STOCK BAJO'
        ELSE 'NORMAL'
    END AS alerta_stock,
    i.estado
FROM insumos i
INNER JOIN categorias_insumos ci
    ON ci.id_categoria = i.id_categoria;

-- FIN DEL SCRIPT
