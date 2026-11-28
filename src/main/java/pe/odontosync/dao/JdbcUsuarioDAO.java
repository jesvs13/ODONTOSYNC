package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.LoginAccount;
import pe.odontosync.model.UsuarioDetalle;
import pe.odontosync.model.UsuarioResumen;

/** Implementación JDBC del acceso a usuarios y de su CRUD administrativo. */
public final class JdbcUsuarioDAO implements UsuarioDAO, UsuarioCrudDAO {
    private static final String FIND_BY_EMAIL = """
        SELECT u.id_usuario, u.id_clinica, u.nombres, u.apellidos, u.password_hash,
               u.estado, r.nombre AS rol, c.nombre AS clinica, c.estado AS estado_clinica,
               u.debe_cambiar_password, u.version_credencial
        FROM usuarios u
        JOIN roles r ON r.id_rol = u.id_rol
        JOIN clinicas c ON c.id_clinica = u.id_clinica
        WHERE u.correo = ?
        """;

    private static final String FIND_BY_CLINIC = """
        SELECT CONCAT(u.nombres,' ',u.apellidos) AS nombre,
               u.correo, r.nombre AS rol, u.estado
        FROM usuarios u
        JOIN roles r ON r.id_rol = u.id_rol
        WHERE u.id_clinica = ?
        ORDER BY u.apellidos, u.nombres
        LIMIT 50
        """;

    private static final String FIND_DETAILED_BY_CLINIC = """
        SELECT u.id_usuario, u.nombres, u.apellidos, u.correo, u.telefono,
               r.nombre AS rol, u.estado, o.numero_colegiatura
        FROM usuarios u
        JOIN roles r ON r.id_rol = u.id_rol
        LEFT JOIN odontologos o ON o.id_usuario = u.id_usuario AND o.id_clinica = u.id_clinica
        WHERE u.id_clinica = ?
        ORDER BY u.apellidos, u.nombres
        LIMIT 100
        """;

    private static final String FIND_DETAILED_BY_ID = """
        SELECT u.id_usuario, u.nombres, u.apellidos, u.correo, u.telefono,
               r.nombre AS rol, u.estado, o.numero_colegiatura
        FROM usuarios u
        JOIN roles r ON r.id_rol = u.id_rol
        LEFT JOIN odontologos o ON o.id_usuario = u.id_usuario AND o.id_clinica = u.id_clinica
        WHERE u.id_clinica = ? AND u.id_usuario = ?
        """;

    @Override
    public List<LoginAccount> findLoginAccountsByEmail(String email) throws SQLException {
        List<LoginAccount> accounts = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_EMAIL)) {
            ps.setQueryTimeout(8);
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(new LoginAccount(
                        rs.getLong("id_usuario"), rs.getLong("id_clinica"),
                        rs.getString("nombres"), rs.getString("apellidos"),
                        rs.getString("password_hash"), rs.getString("estado"),
                        rs.getString("rol"), rs.getString("clinica"),
                        rs.getString("estado_clinica"), rs.getBoolean("debe_cambiar_password"), rs.getLong("version_credencial")
                    ));
                }
            }
        }
        return accounts;
    }

    /** Comprueba la vigencia contra MySQL, incluyendo el estado y rol actuales. */
    public boolean isCredentialCurrent(User user) throws SQLException {
        String sql = """
            SELECT 1 FROM usuarios u
            JOIN clinicas c ON c.id_clinica = u.id_clinica
            JOIN roles r ON r.id_rol = u.id_rol
            WHERE u.id_clinica = ? AND u.id_usuario = ? AND u.version_credencial = ?
              AND u.estado = 'ACTIVO' AND c.estado = 'ACTIVA' AND r.nombre = ?
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, user.clinicId());
            ps.setLong(2, user.id());
            ps.setLong(3, user.credentialVersion());
            ps.setString(4, user.role().name());
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public boolean resetTemporaryPassword(long clinicId, long userId, String newHash) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                String update = """
                    UPDATE usuarios SET password_hash = ?, debe_cambiar_password = 1,
                        version_credencial = version_credencial + 1
                    WHERE id_clinica = ? AND id_usuario = ?
                    """;
                try (PreparedStatement ps = cn.prepareStatement(update)) {
                    ps.setQueryTimeout(8);
                    ps.setString(1, newHash);
                    ps.setLong(2, clinicId);
                    ps.setLong(3, userId);
                    if (ps.executeUpdate() != 1) { cn.rollback(); return false; }
                }
                try (PreparedStatement ps = cn.prepareStatement("""
                    UPDATE password_reset_tokens SET usado_en = NOW()
                    WHERE id_clinica = ? AND id_usuario = ? AND usado_en IS NULL
                    """)) {
                    ps.setQueryTimeout(8);
                    ps.setLong(1, clinicId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }
                cn.commit();
                return true;
            } catch (SQLException | RuntimeException ex) {
                try { cn.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        }
    }

    @Override
    public void updateLastAccess(long userId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "UPDATE usuarios SET ultimo_acceso = NOW() WHERE id_usuario = ?")) {
            ps.setQueryTimeout(8);
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<UsuarioResumen> findByClinic(long clinicId) throws SQLException {
        List<UsuarioResumen> users = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_CLINIC)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new UsuarioResumen(
                        rs.getString("nombre"), rs.getString("correo"),
                        rs.getString("rol"), rs.getString("estado")
                    ));
                }
            }
        }
        return users;
    }

    @Override
    public List<UsuarioDetalle> findDetailedByClinic(long clinicId) throws SQLException {
        List<UsuarioDetalle> users = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_DETAILED_BY_CLINIC)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(readDetailed(rs));
            }
        }
        return users;
    }

    @Override
    public Optional<UsuarioDetalle> findDetailedById(long clinicId, long userId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_DETAILED_BY_ID)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(readDetailed(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public boolean emailExistsGlobally(String email) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM usuarios WHERE correo = ? LIMIT 1")) {
            ps.setQueryTimeout(8);
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public long countActiveAdministrators(long clinicId) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol
            WHERE u.id_clinica = ? AND u.estado = 'ACTIVO' AND r.nombre = 'ADMINISTRADOR'
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    @Override
    public long create(
        long clinicId, Role role, String nombres, String apellidos, String correo,
        String telefono, String passwordHash, String estado, String dni
    ) throws SQLException {
        if (role == Role.ODONTOLOGO) throw new IllegalArgumentException("Registra al profesional desde Odontólogos.");
        Connection cn = DatabaseConnection.getConnection();
        try {
            cn.setAutoCommit(false);
            long roleId = roleId(cn, role);
            long userId;
            String sql = """
                INSERT INTO usuarios
                    (id_clinica, id_rol, nombres, apellidos, correo, password_hash, telefono, estado, dni, debe_cambiar_password)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """;
            try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, clinicId);
                ps.setLong(2, roleId);
                ps.setString(3, nombres);
                ps.setString(4, apellidos);
                ps.setString(5, correo);
                ps.setString(6, passwordHash);
                ps.setString(7, telefono);
                ps.setString(8, estado);
                ps.setString(9, pe.odontosync.security.DniPolicy.normalize(dni));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("MySQL no devolvió el id del usuario creado.");
                    userId = keys.getLong(1);
                }
            }
            cn.commit();
            return userId;
        } catch (SQLException | RuntimeException ex) {
            rollbackQuietly(cn);
            throw ex;
        } finally {
            closeQuietly(cn);
        }
    }

    @Override
    public void update(
        long clinicId, long userId, Role role, String nombres, String apellidos, String correo,
        String telefono, String passwordHashOrNull, String estado, String numeroColegiatura
    ) throws SQLException {
        Connection cn = DatabaseConnection.getConnection();
        try {
            cn.setAutoCommit(false);
            if (role == Role.ODONTOLOGO) {
                updateDentistAccess(cn, clinicId, userId, estado, passwordHashOrNull);
                cn.commit();
                return;
            }
            long roleId = roleId(cn, role);
            String sql = passwordHashOrNull == null ? """
                UPDATE usuarios
                SET id_rol=?, nombres=?, apellidos=?, correo=?, telefono=?, estado=?
                WHERE id_clinica=? AND id_usuario=?
                  AND NOT EXISTS (SELECT 1 FROM odontologos o WHERE o.id_usuario=usuarios.id_usuario)
                  AND id_rol<>(SELECT id_rol FROM roles WHERE nombre='ODONTOLOGO')
                """ : """
                UPDATE usuarios
                SET id_rol=?, nombres=?, apellidos=?, correo=?, telefono=?, estado=?, password_hash=?
                WHERE id_clinica=? AND id_usuario=?
                  AND NOT EXISTS (SELECT 1 FROM odontologos o WHERE o.id_usuario=usuarios.id_usuario)
                  AND id_rol<>(SELECT id_rol FROM roles WHERE nombre='ODONTOLOGO')
                """;
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                int i = 1;
                ps.setLong(i++, roleId);
                ps.setString(i++, nombres);
                ps.setString(i++, apellidos);
                ps.setString(i++, correo);
                ps.setString(i++, telefono);
                ps.setString(i++, estado);
                if (passwordHashOrNull != null) ps.setString(i++, passwordHashOrNull);
                ps.setLong(i++, clinicId);
                ps.setLong(i, userId);
                if (ps.executeUpdate() != 1) throw new SQLException("No se encontró el usuario a actualizar.");
            }

            cn.commit();
        } catch (SQLException | RuntimeException ex) {
            rollbackQuietly(cn);
            throw ex;
        } finally {
            closeQuietly(cn);
        }
    }

    @Override
    public boolean delete(long clinicId, long userId) throws SQLException {
        Connection cn = DatabaseConnection.getConnection();
        try {
            cn.setAutoCommit(false);
            if (hasProtectedReferences(cn, userId)) {
                cn.rollback();
                return false;
            }
            try (PreparedStatement ps = cn.prepareStatement(
                """
                DELETE FROM usuarios WHERE id_clinica=? AND id_usuario=?
                  AND NOT EXISTS (SELECT 1 FROM odontologos o WHERE o.id_usuario=usuarios.id_usuario)
                  AND id_rol<>(SELECT id_rol FROM roles WHERE nombre='ODONTOLOGO')
                """)) {
                ps.setLong(1, clinicId);
                ps.setLong(2, userId);
                boolean deleted = ps.executeUpdate() == 1;
                if (!deleted) {
                    cn.rollback();
                    return false;
                }
            }
            cn.commit();
            return true;
        } catch (SQLException ex) {
            rollbackQuietly(cn);
            if ("23000".equals(ex.getSQLState())) return false;
            throw ex;
        } finally {
            closeQuietly(cn);
        }
    }

    private UsuarioDetalle readDetailed(ResultSet rs) throws SQLException {
        return new UsuarioDetalle(
            rs.getLong("id_usuario"), rs.getString("nombres"), rs.getString("apellidos"),
            rs.getString("correo"), rs.getString("telefono"),
            Role.fromDatabase(rs.getString("rol")), rs.getString("estado"),
            rs.getString("numero_colegiatura")
        );
    }

    private long roleId(Connection cn, Role role) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT id_rol FROM roles WHERE nombre=?")) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("No existe el rol " + role.name() + " en la base de datos.");
                return rs.getLong(1);
            }
        }
    }

    /** Gestiona solo credenciales/estado, sin alterar el perfil profesional. */
    private void updateDentistAccess(Connection cn, long clinicId, long userId, String estado,
                                     String passwordHashOrNull) throws SQLException {
        String sql = "UPDATE usuarios SET estado=?"
            + (passwordHashOrNull == null ? "" : ", password_hash=?")
            + " WHERE id_clinica=? AND id_usuario=?"
            + " AND id_rol=(SELECT id_rol FROM roles WHERE nombre='ODONTOLOGO')"
            + " AND (? <> 'ACTIVO' OR EXISTS (SELECT 1 FROM odontologos o"
            + " WHERE o.id_usuario=usuarios.id_usuario AND o.id_clinica=usuarios.id_clinica AND o.estado='ACTIVO'))";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, estado);
            if (passwordHashOrNull != null) ps.setString(i++, passwordHashOrNull);
            ps.setLong(i++, clinicId);
            ps.setLong(i++, userId);
            ps.setString(i, estado);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("No se pudo actualizar la cuenta. Para habilitarla, el perfil profesional debe estar activo.");
            }
        }
    }

    private boolean hasProtectedReferences(Connection cn, long userId) throws SQLException {
        String sql = """
            SELECT
                EXISTS(SELECT 1 FROM pagos WHERE id_usuario=? LIMIT 1)
                OR EXISTS(SELECT 1 FROM movimientos_inventario WHERE id_usuario=? LIMIT 1)
            """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBoolean(1); }
        }
    }

    private void rollbackQuietly(Connection cn) {
        try { cn.rollback(); } catch (SQLException ignored) { }
    }

    private void closeQuietly(Connection cn) {
        try { cn.close(); } catch (SQLException ignored) { }
    }
}
