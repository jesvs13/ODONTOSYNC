package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.Optional;

/** Persistencia mínima necesaria para que un usuario cambie su propia contraseña. */
public interface PasswordChangeDAO {
    Optional<String> findPasswordHash(long clinicId, long userId) throws SQLException;

    void updatePasswordHash(long clinicId, long userId, String passwordHash) throws SQLException;
}
