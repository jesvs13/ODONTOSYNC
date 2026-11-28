package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.EspecialidadCatalogo;

/** Catálogo global y propio; nunca modifica especialidades de otras clínicas. */
public final class JdbcEspecialidadDAO implements EspecialidadCrudDAO {
    private static final String LIST = """
        SELECT e.id_especialidad, e.id_clinica, e.nombre,
               %s AS activa
        FROM especialidades e
        %s
        WHERE %s
        ORDER BY e.nombre, e.id_especialidad
        """.formatted(EspecialidadCatalogoSql.ACTIVE,
            EspecialidadCatalogoSql.availabilityJoin("?"),
            EspecialidadCatalogoSql.visibleTo("?"));
    private static final String DUPLICATE = "Ya existe una especialidad con ese nombre en tu clínica.";

    @Override
    public List<EspecialidadCatalogo> listByClinic(long clinicId) throws SQLException {
        requireClinic(clinicId);
        try (Connection c = DatabaseConnection.getConnection()) {
            return listByClinic(c, clinicId);
        }
    }

    static List<EspecialidadCatalogo> listByClinic(Connection c, long clinicId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(LIST)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                List<EspecialidadCatalogo> result = new ArrayList<>();
                while (rs.next()) {
                    long owner = rs.getLong("id_clinica");
                    Long ownerId = rs.wasNull() ? null : owner;
                    result.add(new EspecialidadCatalogo(rs.getLong("id_especialidad"),
                        ownerId, rs.getString("nombre"), rs.getBoolean("activa")));
                }
                return List.copyOf(result);
            }
        }
    }

    @Override
    public long create(long clinicId, String nombre) throws SQLException {
        return write(clinicId, c -> {
            ensureUnique(c, clinicId, nombre, 0);
            String sql = "INSERT INTO especialidades (id_clinica, nombre, estado) VALUES (?, ?, 'ACTIVA')";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, clinicId);
                ps.setString(2, nombre);
                if (ps.executeUpdate() != 1) throw new SQLException("No se creó la especialidad.");
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se obtuvo el identificador de especialidad.");
                    return keys.getLong(1);
                }
            }
        });
    }

    @Override
    public void rename(long clinicId, long id, String nombre) throws SQLException {
        write(clinicId, c -> {
            if (isGlobal(c, clinicId, id)) {
                throw new IllegalArgumentException("No puedes modificar el nombre de una especialidad global.");
            }
            ensureUnique(c, clinicId, nombre, id);
            try (PreparedStatement ps = c.prepareStatement(
                "UPDATE especialidades SET nombre = ? WHERE id_especialidad = ? AND id_clinica = ?")) {
                ps.setString(1, nombre);
                ps.setLong(2, id);
                ps.setLong(3, clinicId);
                ps.executeUpdate(); // Una edición sin cambios también es válida.
            }
            return null;
        });
    }

    @Override
    public void setActive(long clinicId, long id, boolean active) throws SQLException {
        write(clinicId, c -> {
            if (isGlobal(c, clinicId, id)) {
                // Solo globales admiten excepciones de disponibilidad por clínica.
                String sql = """
                    INSERT INTO especialidad_clinica (id_clinica, id_especialidad, activa)
                    SELECT ?, id_especialidad, ? FROM especialidades
                    WHERE id_especialidad = ? AND id_clinica IS NULL
                    ON DUPLICATE KEY UPDATE activa = ?
                    """;
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setLong(1, clinicId);
                    ps.setBoolean(2, active);
                    ps.setLong(3, id);
                    ps.setBoolean(4, active);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE especialidades SET estado = ? WHERE id_especialidad = ? AND id_clinica = ?")) {
                    ps.setString(1, active ? "ACTIVA" : "INACTIVA");
                    ps.setLong(2, id);
                    ps.setLong(3, clinicId);
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    private static boolean isGlobal(Connection c, long clinicId, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
            SELECT id_clinica FROM especialidades
            WHERE id_especialidad = ? AND (id_clinica IS NULL OR id_clinica = ?)
            """)) {
            ps.setLong(1, id);
            ps.setLong(2, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("La especialidad no existe en tu clínica.");
                return rs.getObject(1) == null;
            }
        }
    }

    private static void ensureUnique(Connection c, long clinicId, String nombre, long excludeId)
        throws SQLException {
        // El servicio normaliza los espacios. La collation del nombre compara acentos y mayúsculas.
        try (PreparedStatement ps = c.prepareStatement("""
            SELECT 1 FROM especialidades
            WHERE (id_clinica IS NULL OR id_clinica = ?) AND nombre = ?
              AND id_especialidad <> ? LIMIT 1
            """)) {
            ps.setLong(1, clinicId);
            ps.setString(2, nombre);
            ps.setLong(3, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new IllegalArgumentException(DUPLICATE);
            }
        }
    }

    @FunctionalInterface
    private interface Write<T> { T run(Connection c) throws SQLException; }

    private static <T> T write(long clinicId, Write<T> operation) throws SQLException {
        requireClinic(clinicId);
        // GET_LOCK serializa altas y cambios de nombre de una misma clínica incluso
        // cuando su catálogo propio todavía está vacío. DatabaseConnection abre una
        // conexión física (sin pool); al cerrarla MySQL libera el bloqueo de sesión.
        try (Connection c = DatabaseConnection.getConnection()) {
            lockCatalog(c, clinicId);
            c.setAutoCommit(false);
            try {
                T result = operation.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException ex) {
                try { c.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                if (ex instanceof SQLException sql && sql.getErrorCode() == 1062) {
                    throw new IllegalArgumentException(DUPLICATE, ex);
                }
                throw ex;
            }
        }
    }

    // Compartido con las altas y ediciones profesionales para serializar disponibilidad y asignaciones.
    static void lockCatalog(Connection c, long clinicId) throws SQLException {
        requireClinic(clinicId);
        try (PreparedStatement ps = c.prepareStatement("SELECT GET_LOCK(?, 5)")) {
            ps.setString(1, "odontosync.especialidades." + clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt(1) != 1) {
                    throw new SQLException("No se pudo obtener el bloqueo del catálogo.");
                }
            }
        }
    }

    private static void requireClinic(long clinicId) {
        if (clinicId <= 0) throw new IllegalArgumentException("Clínica no válida.");
    }
}
