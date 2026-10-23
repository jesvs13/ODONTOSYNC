-- OdontoSync · Entrega 3 · Atención clínica + Historia clínica
-- Ejecutar una sola vez con un usuario administrador de MySQL.
-- La aplicación ya cuenta con SELECT global; aquí solo se agregan escrituras clínicas necesarias.
-- estados_cita permanece como catálogo de solo lectura: el DAO bloquea únicamente la fila de citas.

GRANT INSERT, UPDATE ON odontosync.historias_clinicas TO 'odontosync_app'@'localhost';
GRANT INSERT, UPDATE ON odontosync.consultas TO 'odontosync_app'@'localhost';
GRANT UPDATE ON odontosync.citas TO 'odontosync_app'@'localhost';
GRANT INSERT ON odontosync.antecedentes_medicos TO 'odontosync_app'@'localhost';
GRANT INSERT ON odontosync.alergias TO 'odontosync_app'@'localhost';

FLUSH PRIVILEGES;
