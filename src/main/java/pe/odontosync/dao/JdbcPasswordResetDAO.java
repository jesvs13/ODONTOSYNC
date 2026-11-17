package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.PasswordResetAccount;
import pe.odontosync.model.PasswordResetResult;

/** Implementacion JDBC de recuperacion de contrasena. */
public final class JdbcPasswordResetDAO implements PasswordResetDAO {
    private static final String FIND_ACCOUNTS = """
        SELECT u.id_usuario, u.id_clinica, u.correo, u.nombres, c.nombre AS clinica_nombre
        FROM usuarios u
        INNER JOIN clinicas c ON c.id_clinica = u.id_clinica
        WHERE LOWER(u.correo) = ?
          AND u.estado = 'ACTIVO'
          AND c.estado = 'ACTIVA'
        ORDER BY u.id_usuario
        """;

    private static final String INVALIDATE_UNUSED = """
        UPDATE password_reset_tokens
        SET usado_en = ?
        WHERE id_usuario = ? AND usado_en IS NULL
        """;

    private static final String INSERT_TOKEN = """
        INSERT INTO password_reset_tokens
            (id_usuario, id_clinica, token_hash, expira_en)
        VALUES (?, ?, ?, ?)
        """;

    private static final String TOKEN_USABLE = """
        SELECT 1
        FROM password_reset_tokens t
        INNER JOIN usuarios u ON u.id_usuario = t.id_usuario AND u.id_clinica = t.id_clinica
        INNER JOIN clinicas c ON c.id_clinica = t.id_clinica
        WHERE t.token_hash = ?
          AND t.usado_en IS NULL
          AND t.expira_en > ?
          AND u.estado = 'ACTIVO'
          AND c.estado = 'ACTIVA'
        LIMIT 1
        """;

    private static final String LOCK_VALID_TOKEN = """
        SELECT t.id_reset, t.id_usuario, t.id_clinica
        FROM password_reset_tokens t
        INNER JOIN usuarios u ON u.id_usuario = t.id_usuario AND u.id_clinica = t.id_clinica
        INNER JOIN clinicas c ON c.id_clinica = t.id_clinica
        WHERE t.token_hash = ?
          AND t.usado_en IS NULL
          AND t.expira_en > ?
          AND u.estado = 'ACTIVO'
          AND c.estado = 'ACTIVA'
        LIMIT 1
        FOR UPDATE
        """;

    private static final String UPDATE_PASSWORD = """
        UPDATE usuarios
        SET password_hash = ?
        WHERE id_usuario = ? AND id_clinica = ? AND estado = 'ACTIVO'
        """;

    private static final String CONSUME_ALL_USER_TOKENS = """
        UPDATE password_reset_tokens
        SET usado_en = ?
        WHERE id_usuario = ? AND usado_en IS NULL
        """;

    @Override
    public List<PasswordResetAccount> findActiveAccountsByEmail(String email) throws SQLException {
        List<PasswordResetAccount> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_ACCOUNTS)) {
            ps.setQueryTimeout(8);
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new PasswordResetAccount(
                        rs.getLong("id_usuario"),
                        rs.getLong("id_clinica"),
                        rs.getString("correo"),
                        rs.getString("nombres"),
                        rs.getString("clinica_nombre")
                    ));
                }
            }
        }
        return result;
    }

    @Override
    public void invalidateUnusedTokens(long userId, Instant usedAt) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(INVALIDATE_UNUSED)) {
            ps.setQueryTimeout(8);
            ps.setTimestamp(1, Timestamp.from(usedAt));
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void saveToken(long userId, long clinicId, String tokenHash, Instant expiresAt)
        throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(INSERT_TOKEN)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, userId);
            ps.setLong(2, clinicId);
            ps.setString(3, tokenHash);
            ps.setTimestamp(4, Timestamp.from(expiresAt));
            ps.executeUpdate();
        }
    }

    @Override
    public boolean isTokenUsable(String tokenHash, Instant now) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(TOKEN_USABLE)) {
            ps.setQueryTimeout(8);
            ps.setString(1, tokenHash);
            ps.setTimestamp(2, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public Optional<PasswordResetResult> consumeTokenAndUpdatePassword(
        String tokenHash,
        String newPasswordHash,
        Instant now
    ) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            boolean previousAutoCommit = cn.getAutoCommit();
            cn.setAutoCommit(false);
            try {
                long resetId;
                long userId;
                long clinicId;
                try (PreparedStatement ps = cn.prepareStatement(LOCK_VALID_TOKEN)) {
                    ps.setQueryTimeout(8);
                    ps.setString(1, tokenHash);
                    ps.setTimestamp(2, Timestamp.from(now));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            cn.rollback();
                            return Optional.empty();
                        }
                        resetId = rs.getLong("id_reset");
                        userId = rs.getLong("id_usuario");
                        clinicId = rs.getLong("id_clinica");
                    }
                }

                try (PreparedStatement ps = cn.prepareStatement(UPDATE_PASSWORD)) {
                    ps.setQueryTimeout(8);
                    ps.setString(1, newPasswordHash);
                    ps.setLong(2, userId);
                    ps.setLong(3, clinicId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("La cuenta activa ya no esta disponible.");
                    }
                }

                // Invalida el token usado y cualquier otro pendiente del mismo usuario.
                try (PreparedStatement ps = cn.prepareStatement(CONSUME_ALL_USER_TOKENS)) {
                    ps.setQueryTimeout(8);
                    ps.setTimestamp(1, Timestamp.from(now));
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }

                // resetId se obtiene bajo FOR UPDATE: conserva explicitamente la fila bloqueada
                // hasta terminar la transaccion y evita doble consumo concurrente.
                if (resetId <= 0) throw new SQLException("Identificador de token no valido.");

                cn.commit();
                return Optional.of(new PasswordResetResult(userId, clinicId));
            } catch (SQLException | RuntimeException ex) {
                try {
                    cn.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }
                throw ex;
            } finally {
                cn.setAutoCommit(previousAutoCommit);
            }
        }
    }
}
