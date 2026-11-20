-- Issue 6 · Permisos mínimos para Inventario funcional.
-- Ejecutar como root o usuario administrador de MySQL.

GRANT SELECT
ON odontosync.categorias_insumos
TO 'odontosync_app'@'localhost';

GRANT SELECT
ON odontosync.insumos
TO 'odontosync_app'@'localhost';

GRANT INSERT (id_clinica, id_categoria, nombre, descripcion, unidad_medida, stock_minimo, precio_referencial, estado)
ON odontosync.insumos
TO 'odontosync_app'@'localhost';

GRANT UPDATE (id_categoria, nombre, descripcion, unidad_medida, stock_minimo, precio_referencial, estado)
ON odontosync.insumos
TO 'odontosync_app'@'localhost';

GRANT SELECT, INSERT
ON odontosync.movimientos_inventario
TO 'odontosync_app'@'localhost';

GRANT SELECT
ON odontosync.usuarios
TO 'odontosync_app'@'localhost';

FLUSH PRIVILEGES;
SHOW GRANTS FOR 'odontosync_app'@'localhost';
