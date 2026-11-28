package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.Optional;

/** Persistencia mínima necesaria para que un usuario cambie su propia contraseña. */
public interface PasswordChangeDAO {
    Optional<String> findPasswordHash(long clinicId, long userId) throws SQLException;

    /** Cambia el hash y elimina la obligación de activación si la credencial sigue vigente. */
    void updatePasswordHashAndClearRequirement(long clinicId, long userId,
        String expectedHash, String passwordHash) throws SQLException;
}
