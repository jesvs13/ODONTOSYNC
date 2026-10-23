-- Solo para la base de pruebas creada con odontosync.sql.
-- Contraseñas de demostración:
-- admin@odontosync.pe              / OdontoAdmin26!
-- recepcion@odontosync.pe          / OdontoRecep26!
-- carlos.odontologo@odontosync.pe  / OdontoDent26!
-- Cambia estas claves con PasswordAdmin antes de cargar información real.

USE odontosync;

UPDATE usuarios SET password_hash='pbkdf2_sha256$310000$2WQz3TgkJq8dabYf5ylFxA==$ZVYDVwH1S8qZjNP8qI9Hv/HRJ7WXyNOOagpB2pDkS2c='
WHERE id_clinica=1 AND correo='admin@odontosync.pe';

UPDATE usuarios SET password_hash='pbkdf2_sha256$310000$1FbWcWUn6+HdHN9IKb8P0g==$vbJ6Qz7fGSgvGcHhGmFTnrxbpdlN1p0EkvAUKx4Sfto='
WHERE id_clinica=1 AND correo='recepcion@odontosync.pe';

UPDATE usuarios SET password_hash='pbkdf2_sha256$310000$YrhP+AvVkz+W19oowM6GNw==$xU/RLCQ76xm0hBXtxPtelVucZ6f00+diwA3i04BFzIA='
WHERE id_clinica=1 AND correo='carlos.odontologo@odontosync.pe';
