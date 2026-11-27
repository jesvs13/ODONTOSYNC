package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.PlacaDetalle;

/** Gestion radiografica con autorizacion por paciente y clinica en cada sentencia. */
public final class JdbcPlacaCrudDAO implements PlacaCrudDAO {
    private static final int QUERY_TIMEOUT_SECONDS = 8;

    // Mismo vinculo por cita que Historia clinica; exige un perfil de odontologo activo.
    // La tabla pacientes siempre tiene alias p; parametros: clinica, usuario.
    private static final String VISIBLE = """
        p.id_clinica=? AND EXISTS (
            SELECT 1 FROM citas c
            JOIN odontologos o ON o.id_odontologo=c.id_odontologo
                AND o.id_clinica=c.id_clinica
            JOIN usuarios u ON u.id_usuario=o.id_usuario
                AND u.id_clinica=o.id_clinica
            WHERE c.id_paciente=p.id_paciente AND c.id_clinica=p.id_clinica
              AND o.id_usuario=? AND o.estado='ACTIVO' AND u.estado='ACTIVO'
        )
        """;

    private static final String PATIENT_VISIBLE = """
        SELECT 1 FROM pacientes p WHERE p.id_paciente=? AND
        """ + VISIBLE + " LIMIT 1";

    private static final String INSERT = """
        INSERT INTO placas_radiograficas
            (id_paciente,id_usuario,tipo_radiografia,fecha_radiografia,region_dental,
             observaciones,nombre_archivo,url_archivo,tipo_mime,tamano_bytes,estado)
        SELECT p.id_paciente,?,?,?,?,?,?,?,?,?,'ACTIVA'
        FROM pacientes p WHERE p.id_paciente=? AND
        """ + VISIBLE;

    private static final String SELECT = """
        SELECT r.id_placa,r.id_paciente,r.id_usuario,r.tipo_radiografia,
               r.fecha_radiografia,r.region_dental,r.observaciones,r.nombre_archivo,
               r.url_archivo,r.tipo_mime,r.tamano_bytes,r.estado
        FROM placas_radiograficas r
        JOIN pacientes p ON p.id_paciente=r.id_paciente
        WHERE r.estado IN ('ACTIVA','ANULADA') AND
        """;

    private static final String FIND_PATIENT = SELECT + " r.id_paciente=? AND " + VISIBLE
        + " ORDER BY r.fecha_radiografia DESC,r.id_placa DESC";

    private static final String FIND_ID = SELECT + " r.id_placa=? AND " + VISIBLE + " LIMIT 1";

    private static final String ANNUL = """
        UPDATE placas_radiograficas r
        JOIN pacientes p ON p.id_paciente=r.id_paciente
        SET r.estado='ANULADA',r.motivo_anulacion=?,
            r.fecha_anulacion=CURRENT_TIMESTAMP,r.id_usuario_anulacion=?
        WHERE r.id_placa=? AND r.id_usuario=? AND r.estado='ACTIVA' AND
        """ + VISIBLE;

    @Override
    public boolean patientVisible(long clinicId, long dentistUserId, long patientId)
        throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(PATIENT_VISIBLE)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, patientId);
            bindVisibility(ps, 2, clinicId, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public long create(long clinicId, long dentistUserId, PlacaDetalle data) throws SQLException {
        if (data == null || data.idUsuario() != dentistUserId || !"ACTIVA".equals(data.estado())) {
            throw new SQLException("Metadatos de registro radiografico no validos.", "45000");
        }
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            long id;
            try (PreparedStatement ps = cn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                ps.setLong(1, dentistUserId);
                ps.setString(2, data.tipoRadiografia());
                ps.setObject(3, data.fechaRadiografia());
                ps.setString(4, data.regionDental());
                ps.setString(5, data.observaciones());
                ps.setString(6, data.nombreArchivo());
                ps.setString(7, data.claveArchivo());
                ps.setString(8, data.tipoMime());
                ps.setLong(9, data.tamanoBytes());
                ps.setLong(10, data.idPaciente());
                bindVisibility(ps, 11, clinicId, dentistUserId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Paciente no autorizado para registrar radiografias.", "45000");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next() || (id = keys.getLong(1)) <= 0) {
                        throw new SQLException("No se obtuvo el identificador de la radiografia.");
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                rollback(cn, ex);
                throw ex;
            }
            try {
                cn.commit();
            } catch (SQLException ex) {
                rollback(cn, ex);
                throw ex;
            }
            return id;
        }
    }

    @Override
    public List<PlacaDetalle> findByPatient(long clinicId, long dentistUserId, long patientId)
        throws SQLException {
        List<PlacaDetalle> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_PATIENT)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, patientId);
            bindVisibility(ps, 2, clinicId, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(map(rs));
            }
        }
        return List.copyOf(items);
    }

    @Override
    public PlacaDetalle findById(long clinicId, long dentistUserId, long plateId)
        throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_ID)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, plateId);
            bindVisibility(ps, 2, clinicId, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    @Override
    public boolean annul(long clinicId, long dentistUserId, long plateId, String reason)
        throws SQLException {
        if (reason == null || reason.isBlank() || reason.length() > 500) return false;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(ANNUL)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setString(1, reason.trim());
            ps.setLong(2, dentistUserId);
            ps.setLong(3, plateId);
            ps.setLong(4, dentistUserId);
            bindVisibility(ps, 5, clinicId, dentistUserId);
            return ps.executeUpdate() == 1;
        }
    }

    /** El predicado VISIBLE siempre recibe clínica y usuario, en ese orden. */
    private static void bindVisibility(PreparedStatement ps, int first,
                                       long clinicId, long dentistUserId) throws SQLException {
        ps.setLong(first, clinicId);
        ps.setLong(first + 1, dentistUserId);
    }

    private static PlacaDetalle map(ResultSet rs) throws SQLException {
        return new PlacaDetalle(rs.getLong("id_placa"),rs.getLong("id_paciente"),
            rs.getLong("id_usuario"),rs.getString("tipo_radiografia"),
            rs.getObject("fecha_radiografia", LocalDate.class),rs.getString("region_dental"),
            rs.getString("observaciones"),rs.getString("nombre_archivo"),
            rs.getString("url_archivo"),rs.getString("tipo_mime"),
            rs.getLong("tamano_bytes"),rs.getString("estado"));
    }

    private static void rollback(Connection cn, Exception cause) {
        try { cn.rollback(); } catch (SQLException ex) { cause.addSuppressed(ex); }
    }
}
