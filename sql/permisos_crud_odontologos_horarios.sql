-- Permisos mínimos para CRUD de Odontólogos + Horarios.
-- Ejecutar una sola vez con root/administrador de MySQL.

-- El alta/edición de odontólogos mantiene sincronizada su cuenta de acceso.
GRANT INSERT, UPDATE
ON odontosync.usuarios
TO 'odontosync_app'@'localhost';

GRANT INSERT, UPDATE
ON odontosync.odontologos
TO 'odontosync_app'@'localhost';

-- Se reemplazan las especialidades seleccionadas al editar un perfil profesional.
GRANT INSERT, DELETE
ON odontosync.odontologo_especialidad
TO 'odontosync_app'@'localhost';

-- Los horarios se gestionan con alta, edición y baja lógica.
GRANT INSERT, UPDATE
ON odontosync.horarios
TO 'odontosync_app'@'localhost';

FLUSH PRIVILEGES;
SHOW GRANTS FOR 'odontosync_app'@'localhost';
