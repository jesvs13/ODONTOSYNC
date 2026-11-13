package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import pe.odontosync.config.DatabaseConnection;

/** Implementación JDBC para el cambio de contraseña de la cuenta autenticada. */
public final class JdbcPasswordChangeDAO implements PasswordChangeDAO {
    private static final String FIND_HASH = """
        SELECT password_hash
        FROM usuarios
        WHERE id_clinica = ? AND id_usuario = ? AND estado = 'ACTIVO'
        """;

    private static final String UPDATE_HASH = """
        UPDATE usuarios
        SET password_hash = ?
        WHERE id_clinica = ? AND id_usuario = ? AND estado = 'ACTIVO'
        """;

    @Override
    public Optional<String> findPasswordHash(long clinicId, long userId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_HASH)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString("password_hash")) : Optional.empty();
            }
        }
    }

    @Override
    public void updatePasswordHash(long clinicId, long userId, String passwordHash) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(UPDATE_HASH)) {
            ps.setQueryTimeout(8);
            ps.setString(1, passwordHash);
            ps.setLong(2, clinicId);
            ps.setLong(3, userId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new SQLException("No se pudo actualizar la contraseña de la cuenta activa.");
            }
        }
    }
}
