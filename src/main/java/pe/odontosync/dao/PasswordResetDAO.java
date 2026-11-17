package pe.odontosync.dao;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import pe.odontosync.model.PasswordResetAccount;
import pe.odontosync.model.PasswordResetResult;

/** Persistencia necesaria para solicitar y consumir tokens de recuperacion. */
public interface PasswordResetDAO {
    List<PasswordResetAccount> findActiveAccountsByEmail(String email) throws SQLException;

    void invalidateUnusedTokens(long userId, Instant usedAt) throws SQLException;

    void saveToken(
        long userId,
        long clinicId,
        String tokenHash,
        Instant expiresAt
    ) throws SQLException;

    boolean isTokenUsable(String tokenHash, Instant now) throws SQLException;

    Optional<PasswordResetResult> consumeTokenAndUpdatePassword(
        String tokenHash,
        String newPasswordHash,
        Instant now
    ) throws SQLException;
}
