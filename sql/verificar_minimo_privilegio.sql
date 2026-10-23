-- Semana 10 · Verificación de principio de mínimo privilegio.
-- Ejecutar con una cuenta administradora. No modifica permisos ni datos.
SHOW GRANTS FOR 'odontosync_app'@'localhost';

-- Criterio esperado en OdontoSync:
-- 1) SELECT para consultar catálogos y datos necesarios.
-- 2) INSERT/UPDATE/DELETE solo en las tablas que cada módulo necesita modificar.
-- 3) No debe existir GRANT ALL PRIVILEGES para odontosync_app.
