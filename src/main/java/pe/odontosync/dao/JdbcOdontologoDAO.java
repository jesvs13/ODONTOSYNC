package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoDetalle;
import pe.odontosync.model.OdontologoResumen;

/** Acceso JDBC de consulta y CRUD para odontólogos. */
public final class JdbcOdontologoDAO implements OdontologoDAO, OdontologoCrudDAO {
    private static final String FIND_BY_CLINIC = """
        SELECT CONCAT(o.nombres,' ',o.apellidos) AS odontologo,
               o.numero_colegiatura, o.correo, o.estado
        FROM odontologos o
        JOIN usuarios u ON u.id_usuario = o.id_usuario
        JOIN roles r ON r.id_rol = u.id_rol
        WHERE o.id_clinica = ? AND r.nombre = 'ODONTOLOGO'
        ORDER BY o.apellidos, o.nombres
        LIMIT 50
        """;

    private static final String DETAIL_SELECT = """
        SELECT o.id_odontologo, o.id_usuario, o.nombres, o.apellidos,
               o.numero_colegiatura, o.telefono, o.correo, o.fecha_ingreso, o.estado,
               GROUP_CONCAT(DISTINCT e.id_especialidad ORDER BY e.nombre) AS especialidad_ids,
               GROUP_CONCAT(DISTINCT e.nombre ORDER BY e.nombre SEPARATOR '||') AS especialidades
        FROM odontologos o
        JOIN usuarios u ON u.id_usuario = o.id_usuario
        JOIN roles r ON r.id_rol = u.id_rol
        LEFT JOIN odontologo_especialidad oe ON oe.id_odontologo = o.id_odontologo
        LEFT JOIN especialidades e ON e.id_especialidad = oe.id_especialidad
        WHERE o.id_clinica = ? AND r.nombre = 'ODONTOLOGO'
        """;

    @Override
    public List<OdontologoResumen> findByClinic(long clinicId) throws SQLException {
        List<OdontologoResumen> dentists = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_CLINIC)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dentists.add(new OdontologoResumen(
                        rs.getString("odontologo"),
                        rs.getString("numero_colegiatura"),
                        rs.getString("correo"),
                        rs.getString("estado")
                    ));
                }
            }
        }
        return dentists;
    }

    @Override
    public List<OdontologoDetalle> searchByClinic(long clinicId, String query) throws SQLException {
        String q = query == null ? "" : query.trim();
        String sql = DETAIL_SELECT + (q.isBlank() ? "" : """
             AND (o.nombres LIKE ? OR o.apellidos LIKE ? OR o.numero_colegiatura LIKE ?
                  OR o.correo LIKE ? OR EXISTS (
                      SELECT 1 FROM odontologo_especialidad oex
                      JOIN especialidades ex ON ex.id_especialidad = oex.id_especialidad
                      WHERE oex.id_odontologo = o.id_odontologo AND ex.nombre LIKE ?
                  ))
            """) + """
            GROUP BY o.id_odontologo, o.id_usuario, o.nombres, o.apellidos,
                     o.numero_colegiatura, o.telefono, o.correo, o.fecha_ingreso, o.estado
            ORDER BY o.apellidos, o.nombres
            LIMIT 100
            """;
        List<OdontologoDetalle> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            if (!q.isBlank()) {
                String like = "%" + q + "%";
                for (int i = 2; i <= 6; i++) ps.setString(i, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(readDetailed(rs));
            }
        }
        return result;
    }

    @Override
    public Optional<OdontologoDetalle> findDetailedById(long clinicId, long dentistId) throws SQLException {
        String sql = DETAIL_SELECT + """
             AND o.id_odontologo = ?
            GROUP BY o.id_odontologo, o.id_usuario, o.nombres, o.apellidos,
                     o.numero_colegiatura, o.telefono, o.correo, o.fecha_ingreso, o.estado
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setLong(2, dentistId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(readDetailed(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<EspecialidadOpcion> findActiveSpecialties() throws SQLException {
        String sql = "SELECT id_especialidad, nombre, estado FROM especialidades WHERE estado='ACTIVA' ORDER BY nombre";
        List<EspecialidadOpcion> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new EspecialidadOpcion(
                    rs.getLong("id_especialidad"), rs.getString("nombre"), rs.getString("estado")
                ));
            }
        }
        return result;
    }

    @Override
    public boolean emailExists(long clinicId, String email, Long excludeUserId) throws SQLException {
        String sql = "SELECT 1 FROM usuarios WHERE id_clinica=? AND correo=?"
            + (excludeUserId == null ? "" : " AND id_usuario<>?") + " LIMIT 1";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setString(2, email);
            if (excludeUserId != null) ps.setLong(3, excludeUserId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public boolean licenseExists(long clinicId, String license, Long excludeDentistId) throws SQLException {
        String sql = "SELECT 1 FROM odontologos WHERE id_clinica=? AND numero_colegiatura=?"
            + (excludeDentistId == null ? "" : " AND id_odontologo<>?") + " LIMIT 1";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setString(2, license);
            if (excludeDentistId != null) ps.setLong(3, excludeDentistId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public long create(
        long clinicId, String nombres, String apellidos, String correo, String telefono,
        String passwordHash, String colegiatura, LocalDate fechaIngreso, String estado,
        List<Long> especialidadIds
    ) throws SQLException {
        Connection cn = DatabaseConnection.getConnection();
        try {
            cn.setAutoCommit(false);
            long roleId = dentistRoleId(cn);
            long userId;
            try (PreparedStatement ps = cn.prepareStatement("""
                INSERT INTO usuarios
                    (id_clinica,id_rol,nombres,apellidos,correo,password_hash,telefono,estado)
                VALUES (?,?,?,?,?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, clinicId);
                ps.setLong(2, roleId);
                ps.setString(3, nombres);
                ps.setString(4, apellidos);
                ps.setString(5, correo);
                ps.setString(6, passwordHash);
                ps.setString(7, telefono);
                ps.setString(8, userStatus(estado));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("MySQL no devolvió el id del usuario odontólogo.");
                    userId = keys.getLong(1);
                }
            }

            long dentistId;
            try (PreparedStatement ps = cn.prepareStatement("""
                INSERT INTO odontologos
                    (id_usuario,id_clinica,numero_colegiatura,nombres,apellidos,telefono,correo,fecha_ingreso,estado)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setLong(2, clinicId);
                ps.setString(3, colegiatura);
                ps.setString(4, nombres);
                ps.setString(5, apellidos);
                ps.setString(6, telefono);
                ps.setString(7, correo);
                ps.setDate(8, Date.valueOf(fechaIngreso));
                ps.setString(9, estado);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("MySQL no devolvió el id del odontólogo.");
                    dentistId = keys.getLong(1);
                }
            }
            replaceSpecialties(cn, dentistId, especialidadIds);
            cn.commit();
            return dentistId;
        } catch (SQLException | RuntimeException ex) {
            rollbackQuietly(cn);
            throw ex;
        } finally {
            closeQuietly(cn);
        }
    }

    @Override
    public void update(
        long clinicId, long dentistId, String nombres, String apellidos, String correo,
        String telefono, String passwordHashOrNull, String colegiatura,
        LocalDate fechaIngreso, String estado, List<Long> especialidadIds
    ) throws SQLException {
        Connection cn = DatabaseConnection.getConnection();
        try {
            cn.setAutoCommit(false);
            long userId = userIdForDentist(cn, clinicId, dentistId);
            long roleId = dentistRoleId(cn);

            String userSql = passwordHashOrNull == null ? """
                UPDATE usuarios
                SET id_rol=?,nombres=?,apellidos=?,correo=?,telefono=?,estado=?
                WHERE id_clinica=? AND id_usuario=?
                """ : """
                UPDATE usuarios
                SET id_rol=?,nombres=?,apellidos=?,correo=?,telefono=?,estado=?,password_hash=?
                WHERE id_clinica=? AND id_usuario=?
                """;
            try (PreparedStatement ps = cn.prepareStatement(userSql)) {
                int i = 1;
                ps.setLong(i++, roleId);
                ps.setString(i++, nombres);
                ps.setString(i++, apellidos);
                ps.setString(i++, correo);
                ps.setString(i++, telefono);
                ps.setString(i++, userStatus(estado));
                if (passwordHashOrNull != null) ps.setString(i++, passwordHashOrNull);
                ps.setLong(i++, clinicId);
                ps.setLong(i, userId);
                if (ps.executeUpdate() != 1) throw new SQLException("No se encontró la cuenta vinculada del odontólogo.");
            }

            try (PreparedStatement ps = cn.prepareStatement("""
                UPDATE odontologos
                SET numero_colegiatura=?,nombres=?,apellidos=?,telefono=?,correo=?,fecha_ingreso=?,estado=?
                WHERE id_clinica=? AND id_odontologo=?
                """)) {
                ps.setString(1, colegiatura);
                ps.setString(2, nombres);
                ps.setString(3, apellidos);
                ps.setString(4, telefono);
                ps.setString(5, correo);
                ps.setDate(6, Date.valueOf(fechaIngreso));
                ps.setString(7, estado);
                ps.setLong(8, clinicId);
                ps.setLong(9, dentistId);
                if (ps.executeUpdate() != 1) throw new SQLException("No se encontró el odontólogo a actualizar.");
            }
            replaceSpecialties(cn, dentistId, especialidadIds);
            cn.commit();
        } catch (SQLException | RuntimeException ex) {
            rollbackQuietly(cn);
            throw ex;
        } finally {
            closeQuietly(cn);
        }
    }

    @Override
    public void updateStatus(long clinicId, long dentistId, String estado) throws SQLException {
        Connection cn = DatabaseConnection.getConnection();
        try {
            cn.setAutoCommit(false);
            long userId = userIdForDentist(cn, clinicId, dentistId);
            try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE odontologos SET estado=? WHERE id_clinica=? AND id_odontologo=?")) {
                ps.setString(1, estado);
                ps.setLong(2, clinicId);
                ps.setLong(3, dentistId);
                if (ps.executeUpdate() != 1) throw new SQLException("No se encontró el odontólogo.");
            }
            try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE usuarios SET estado=? WHERE id_clinica=? AND id_usuario=?")) {
                ps.setString(1, userStatus(estado));
                ps.setLong(2, clinicId);
                ps.setLong(3, userId);
                ps.executeUpdate();
            }
            if ("INACTIVO".equals(estado)) {
                try (PreparedStatement ps = cn.prepareStatement(
                    "UPDATE horarios SET estado='INACTIVO' WHERE id_odontologo=?")) {
                    ps.setLong(1, dentistId);
                    ps.executeUpdate();
                }
            }
            cn.commit();
        } catch (SQLException | RuntimeException ex) {
            rollbackQuietly(cn);
            throw ex;
        } finally {
            closeQuietly(cn);
        }
    }

    private OdontologoDetalle readDetailed(ResultSet rs) throws SQLException {
        List<Long> ids = splitLongs(rs.getString("especialidad_ids"));
        List<String> names = splitNames(rs.getString("especialidades"));
        Date date = rs.getDate("fecha_ingreso");
        return new OdontologoDetalle(
            rs.getLong("id_odontologo"), rs.getLong("id_usuario"),
            rs.getString("nombres"), rs.getString("apellidos"),
            rs.getString("numero_colegiatura"), rs.getString("telefono"), rs.getString("correo"),
            date == null ? null : date.toLocalDate(), rs.getString("estado"), ids, names
        );
    }

    private List<Long> splitLongs(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<Long> result = new ArrayList<>();
        for (String part : raw.split(",")) {
            try { result.add(Long.parseLong(part.trim())); } catch (NumberFormatException ignored) { }
        }
        return Collections.unmodifiableList(result);
    }

    private List<String> splitNames(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return List.of(raw.split("\\|\\|"));
    }

    private long dentistRoleId(Connection cn) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT id_rol FROM roles WHERE nombre='ODONTOLOGO'");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("No existe el rol ODONTOLOGO en la base de datos.");
            return rs.getLong(1);
        }
    }

    private long userIdForDentist(Connection cn, long clinicId, long dentistId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT id_usuario FROM odontologos WHERE id_clinica=? AND id_odontologo=? AND id_usuario IS NOT NULL")) {
            ps.setLong(1, clinicId);
            ps.setLong(2, dentistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("El odontólogo no tiene una cuenta de usuario vinculada.");
                return rs.getLong(1);
            }
        }
    }

    private void replaceSpecialties(Connection cn, long dentistId, List<Long> ids) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
            "DELETE FROM odontologo_especialidad WHERE id_odontologo=?")) {
            ps.setLong(1, dentistId);
            ps.executeUpdate();
        }
        if (ids == null || ids.isEmpty()) return;
        try (PreparedStatement ps = cn.prepareStatement("""
            INSERT INTO odontologo_especialidad (id_odontologo,id_especialidad)
            SELECT ?, id_especialidad FROM especialidades
            WHERE id_especialidad=? AND estado='ACTIVA'
            """)) {
            for (Long id : ids) {
                ps.setLong(1, dentistId);
                ps.setLong(2, id);
                if (ps.executeUpdate() != 1) throw new SQLException("Especialidad no válida: " + id);
            }
        }
    }

    private String userStatus(String dentistStatus) {
        return "ACTIVO".equals(dentistStatus) ? "ACTIVO" : "INACTIVO";
    }

    private void rollbackQuietly(Connection cn) {
        try { cn.rollback(); } catch (SQLException ignored) { }
    }

    private void closeQuietly(Connection cn) {
        try { cn.close(); } catch (SQLException ignored) { }
    }
}
