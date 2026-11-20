package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.PacienteOpcion;
import pe.odontosync.model.RecetaResumen;

/** Acceso JDBC a recetas con consulta y registro seguro por clínica y odontólogo. */
public final class JdbcRecetaDAO implements RecetaCrudDAO {
    private static final int QUERY_TIMEOUT_SECONDS = 8;
    private static final String FIND_BY_DENTIST = """
        SELECT r.id_receta,
               r.id_paciente,
               DATE_FORMAT(r.fecha_emision,'%Y-%m-%d %H:%i') AS fecha,
               CONCAT(p.nombres,' ',p.apellidos) AS paciente,
               r.medicamentos,
               r.indicaciones
        FROM recetas r
        JOIN pacientes p ON p.id_paciente=r.id_paciente
        JOIN odontologos o ON o.id_odontologo=r.id_odontologo
        WHERE p.id_clinica=? AND o.id_clinica=? AND o.id_usuario=?
        ORDER BY r.fecha_emision DESC
        LIMIT 100
        """;


    private static final String FIND_BY_ID = """
        SELECT r.id_receta,
               r.id_paciente,
               DATE_FORMAT(r.fecha_emision,'%Y-%m-%d %H:%i') AS fecha,
               CONCAT(p.nombres,' ',p.apellidos) AS paciente,
               r.medicamentos,
               r.indicaciones
        FROM recetas r
        JOIN pacientes p
          ON p.id_paciente=r.id_paciente
        JOIN odontologos autor
          ON autor.id_odontologo=r.id_odontologo
         AND autor.id_clinica=p.id_clinica
        WHERE r.id_receta=?
          AND p.id_clinica=?
          AND EXISTS (
              SELECT 1
              FROM citas c
              JOIN odontologos visor
                ON visor.id_odontologo=c.id_odontologo
               AND visor.id_clinica=c.id_clinica
              WHERE c.id_clinica=p.id_clinica
                AND c.id_paciente=p.id_paciente
                AND visor.id_usuario=?
                AND visor.estado='ACTIVO'
          )
        LIMIT 1
        """;


    private static final String FIND_BY_PATIENT = """
        SELECT r.id_receta,
               r.id_paciente,
               DATE_FORMAT(r.fecha_emision,'%Y-%m-%d %H:%i') AS fecha,
               CONCAT(p.nombres,' ',p.apellidos) AS paciente,
               r.medicamentos,
               r.indicaciones
        FROM recetas r
        JOIN pacientes p
          ON p.id_paciente=r.id_paciente
        JOIN odontologos autor
          ON autor.id_odontologo=r.id_odontologo
         AND autor.id_clinica=p.id_clinica
        WHERE p.id_clinica=?
          AND p.id_paciente=?
          AND EXISTS (
              SELECT 1
              FROM citas c
              JOIN odontologos visor
                ON visor.id_odontologo=c.id_odontologo
               AND visor.id_clinica=c.id_clinica
              WHERE c.id_clinica=p.id_clinica
                AND c.id_paciente=p.id_paciente
                AND visor.id_usuario=?
                AND visor.estado='ACTIVO'
          )
        ORDER BY r.fecha_emision DESC
        LIMIT 100
        """;

    private static final String FIND_PATIENTS = """
        SELECT DISTINCT p.id_paciente,
               CONCAT(p.nombres,' ',p.apellidos) AS nombre,
               p.numero_documento AS documento,
               p.estado
        FROM pacientes p
        JOIN citas c
          ON c.id_paciente=p.id_paciente
         AND c.id_clinica=p.id_clinica
        JOIN odontologos o
          ON o.id_odontologo=c.id_odontologo
         AND o.id_clinica=c.id_clinica
        JOIN estados_cita ec ON ec.id_estado_cita=c.id_estado_cita
        WHERE p.id_clinica=?
          AND c.id_clinica=?
          AND o.id_clinica=?
          AND o.id_usuario=?
          AND p.estado='ACTIVO'
          AND o.estado='ACTIVO'
          AND ec.nombre IN ('CONFIRMADA','ATENDIDA')
        ORDER BY nombre
        LIMIT 100
        """;

    private static final String FIND_DENTIST = """
        SELECT id_odontologo
        FROM odontologos
        WHERE id_clinica=? AND id_usuario=? AND estado='ACTIVO'
        LIMIT 1
        """;

    private static final String VALIDATE_PATIENT = """
        SELECT p.id_paciente
        FROM pacientes p
        WHERE p.id_paciente=?
          AND p.id_clinica=?
          AND p.estado='ACTIVO'
          AND EXISTS (
              SELECT 1
              FROM citas c
              JOIN estados_cita ec ON ec.id_estado_cita=c.id_estado_cita
              WHERE c.id_clinica=?
                AND c.id_paciente=p.id_paciente
                AND c.id_odontologo=?
                AND ec.nombre IN ('CONFIRMADA','ATENDIDA')
          )
        LIMIT 1
        """;

    private static final String INSERT_RECIPE = """
        INSERT INTO recetas (id_paciente,id_odontologo,medicamentos,indicaciones)
        VALUES (?,?,?,?)
        """;

    @Override
    public List<RecetaResumen> findByDentist(long clinicId, long dentistUserId) throws SQLException {
        List<RecetaResumen> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_DENTIST)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, clinicId);
            ps.setLong(2, clinicId);
            ps.setLong(3, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRecipe(rs));
                }
            }
        }
        return items;
    }


    @Override
    public RecetaResumen findById(long clinicId, long dentistUserId, long recipeId)
        throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_ID)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, recipeId);
            ps.setLong(2, clinicId);
            ps.setLong(3, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapRecipe(rs);
            }
        }
    }


    @Override
    public List<RecetaResumen> findByPatient(long clinicId, long dentistUserId, long patientId)
        throws SQLException {
        List<RecetaResumen> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_PATIENT)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, clinicId);
            ps.setLong(2, patientId);
            ps.setLong(3, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRecipe(rs));
                }
            }
        }
        return items;
    }

    @Override
    public List<PacienteOpcion> findPatientsByDentist(long clinicId, long dentistUserId)
        throws SQLException {
        List<PacienteOpcion> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_PATIENTS)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, clinicId);
            ps.setLong(2, clinicId);
            ps.setLong(3, clinicId);
            ps.setLong(4, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new PacienteOpcion(
                        rs.getLong("id_paciente"),
                        rs.getString("nombre"),
                        rs.getString("documento"),
                        rs.getString("estado")
                    ));
                }
            }
        }
        return items;
    }

    @Override
    public long create(
        long clinicId,
        long dentistUserId,
        long patientId,
        String medications,
        String indications
    ) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                long dentistId = findDentistId(cn, clinicId, dentistUserId);
                validatePatient(cn, clinicId, patientId, dentistId);

                long recipeId;
                try (PreparedStatement ps = cn.prepareStatement(
                    INSERT_RECIPE, Statement.RETURN_GENERATED_KEYS
                )) {
                    ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    ps.setLong(1, patientId);
                    ps.setLong(2, dentistId);
                    ps.setString(3, medications);
                    ps.setString(4, indications);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("MySQL no devolvió el identificador de la receta.");
                        }
                        recipeId = keys.getLong(1);
                    }
                }

                cn.commit();
                return recipeId;
            } catch (SQLException | RuntimeException ex) {
                try {
                    cn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                throw ex;
            }
        }
    }

    private RecetaResumen mapRecipe(ResultSet rs) throws SQLException {
        return new RecetaResumen(
            rs.getLong("id_receta"),
            rs.getLong("id_paciente"),
            rs.getString("fecha"),
            rs.getString("paciente"),
            rs.getString("medicamentos"),
            rs.getString("indicaciones")
        );
    }

    private long findDentistId(Connection cn, long clinicId, long dentistUserId)
        throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(FIND_DENTIST)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, clinicId);
            ps.setLong(2, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException(
                        "Tu cuenta no tiene un odontólogo activo asociado en esta clínica."
                    );
                }
                return rs.getLong("id_odontologo");
            }
        }
    }

    private void validatePatient(Connection cn, long clinicId, long patientId, long dentistId)
        throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(VALIDATE_PATIENT)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, patientId);
            ps.setLong(2, clinicId);
            ps.setLong(3, clinicId);
            ps.setLong(4, dentistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException(
                        "El paciente no pertenece a tu clínica o no está vinculado a tus citas."
                    );
                }
            }
        }
    }
}
