package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.AlergiaClinicaDetalle;
import pe.odontosync.model.AntecedenteClinicoDetalle;
import pe.odontosync.model.AtencionCitaDetalle;
import pe.odontosync.model.ConsultaClinicaDetalle;
import pe.odontosync.model.ConsultaHistorialDetalle;
import pe.odontosync.model.HistoriaClinicaDetalle;
import pe.odontosync.model.HistoriaPacienteResumen;
import pe.odontosync.model.HistoriaPacienteVista;
import pe.odontosync.model.PacienteDetalle;

/** JDBC para atención clínica e historia. No contiene reglas de interfaz. */
public final class JdbcClinicaAtencionDAO implements ClinicaAtencionDAO {
    @Override
    public Optional<AtencionCitaDetalle> findAppointmentForDentist(
        long clinicId, long dentistUserId, long appointmentId
    ) throws SQLException {
        String sql = """
            SELECT c.id_cita, c.id_paciente,
                   CONCAT(p.nombres,' ',p.apellidos) AS paciente,
                   CONCAT(p.tipo_documento,' ',p.numero_documento) AS documento,
                   c.id_odontologo, CONCAT(o.nombres,' ',o.apellidos) AS odontologo,
                   COALESCE(es.nombre,'') AS especialidad, ec.nombre AS estado,
                   c.fecha, c.hora_inicio, c.hora_fin,
                   COALESCE(c.motivo,'') AS motivo_cita,
                   COALESCE(c.observaciones,'') AS observaciones_cita,
                   co.id_consulta, DATE_FORMAT(co.fecha_consulta,'%Y-%m-%d %H:%i') AS fecha_consulta,
                   COALESCE(co.motivo_consulta,'') AS motivo_consulta,
                   COALESCE(co.diagnostico,'') AS diagnostico,
                   COALESCE(co.tratamiento,'') AS tratamiento,
                   COALESCE(co.observaciones,'') AS observaciones_clinicas,
                   COALESCE(co.recomendaciones,'') AS recomendaciones
            FROM citas c
            JOIN pacientes p ON p.id_paciente=c.id_paciente AND p.id_clinica=c.id_clinica
            JOIN odontologos o ON o.id_odontologo=c.id_odontologo AND o.id_clinica=c.id_clinica
            JOIN estados_cita ec ON ec.id_estado_cita=c.id_estado_cita
            LEFT JOIN especialidades es ON es.id_especialidad=c.id_especialidad
            LEFT JOIN consultas co ON co.id_cita=c.id_cita
            WHERE c.id_clinica=? AND c.id_cita=? AND o.id_usuario=?
            LIMIT 1
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, appointmentId);
            ps.setLong(3, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Long consultationId = (Long) rs.getObject("id_consulta");
                Optional<ConsultaClinicaDetalle> consultation = consultationId == null
                    ? Optional.empty()
                    : Optional.of(new ConsultaClinicaDetalle(
                        consultationId,
                        rs.getString("fecha_consulta"),
                        rs.getString("motivo_consulta"),
                        rs.getString("diagnostico"),
                        rs.getString("tratamiento"),
                        rs.getString("observaciones_clinicas"),
                        rs.getString("recomendaciones")
                    ));
                return Optional.of(new AtencionCitaDetalle(
                    rs.getLong("id_cita"), rs.getLong("id_paciente"), rs.getString("paciente"),
                    rs.getString("documento"), rs.getLong("id_odontologo"), rs.getString("odontologo"),
                    rs.getString("especialidad"), rs.getString("estado"), rs.getDate("fecha").toLocalDate(),
                    rs.getTime("hora_inicio").toLocalTime(), rs.getTime("hora_fin").toLocalTime(),
                    rs.getString("motivo_cita"), rs.getString("observaciones_cita"), consultation
                ));
            }
        }
    }

    @Override
    public long saveAttention(
        long clinicId, long dentistUserId, long appointmentId,
        String motivoConsulta, String diagnostico, String tratamiento,
        String observaciones, String recomendaciones
    ) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                long patientId;
                long dentistId = dentistIdForUser(cn, clinicId, dentistUserId);
                long statusId;
                long confirmedStatusId = appointmentStatusId(cn, "CONFIRMADA");
                long attendedStatusId = appointmentStatusId(cn, "ATENDIDA");

                /*
                 * Bloqueamos únicamente la fila de citas que será atendida.
                 * Los catálogos y el perfil del odontólogo se consultan antes con SELECT
                 * normal para conservarlos como datos de solo lectura.
                 */
                String lockSql = """
                    SELECT id_paciente, id_odontologo, id_estado_cita
                    FROM citas
                    WHERE id_clinica=? AND id_cita=? AND id_odontologo=?
                    FOR UPDATE
                    """;
                try (PreparedStatement ps = cn.prepareStatement(lockSql)) {
                    ps.setLong(1, clinicId);
                    ps.setLong(2, appointmentId);
                    ps.setLong(3, dentistId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("La cita no pertenece al odontólogo conectado.", "45000");
                        patientId = rs.getLong("id_paciente");
                        statusId = rs.getLong("id_estado_cita");
                    }
                }
                if (!(statusId == confirmedStatusId || statusId == attendedStatusId)) {
                    throw new SQLException("Solo una cita confirmada o atendida puede registrar atención clínica.", "45000");
                }

                try (PreparedStatement ps = cn.prepareStatement("""
                    INSERT INTO historias_clinicas (id_paciente, fecha_apertura, observaciones_generales, estado)
                    SELECT ?, CURRENT_DATE, NULL, 'ACTIVA'
                    WHERE NOT EXISTS (SELECT 1 FROM historias_clinicas WHERE id_paciente=?)
                    """)) {
                    ps.setLong(1, patientId);
                    ps.setLong(2, patientId);
                    ps.executeUpdate();
                }

                Long existingId = null;
                try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT id_consulta FROM consultas WHERE id_cita=? LIMIT 1")) {
                    ps.setLong(1, appointmentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) existingId = rs.getLong(1);
                    }
                }

                long consultationId;
                if (existingId == null) {
                    String insert = """
                        INSERT INTO consultas
                          (id_cita,id_odontologo,fecha_consulta,motivo_consulta,diagnostico,
                           tratamiento,observaciones,recomendaciones)
                        VALUES (?,?,CURRENT_TIMESTAMP,?,?,?,?,?)
                        """;
                    try (PreparedStatement ps = cn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setLong(1, appointmentId);
                        ps.setLong(2, dentistId);
                        ps.setString(3, motivoConsulta);
                        ps.setString(4, diagnostico);
                        ps.setString(5, tratamiento);
                        ps.setString(6, observationsOrNull(observaciones));
                        ps.setString(7, observationsOrNull(recomendaciones));
                        if (ps.executeUpdate() != 1) throw new SQLException("No se pudo registrar la consulta clínica.");
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("MySQL no devolvió el id de la consulta.");
                            consultationId = keys.getLong(1);
                        }
                    }
                } else {
                    consultationId = existingId;
                    String update = """
                        UPDATE consultas
                        SET motivo_consulta=?, diagnostico=?, tratamiento=?, observaciones=?, recomendaciones=?
                        WHERE id_consulta=? AND id_cita=? AND id_odontologo=?
                        """;
                    try (PreparedStatement ps = cn.prepareStatement(update)) {
                        ps.setString(1, motivoConsulta);
                        ps.setString(2, diagnostico);
                        ps.setString(3, tratamiento);
                        ps.setString(4, observationsOrNull(observaciones));
                        ps.setString(5, observationsOrNull(recomendaciones));
                        ps.setLong(6, consultationId);
                        ps.setLong(7, appointmentId);
                        ps.setLong(8, dentistId);
                        if (ps.executeUpdate() != 1) throw new SQLException("No se pudo actualizar la consulta clínica.");
                    }
                }

                if (statusId != attendedStatusId) {
                    String closeSql = """
                        UPDATE citas
                        SET id_estado_cita=?
                        WHERE id_clinica=? AND id_cita=?
                        """;
                    try (PreparedStatement ps = cn.prepareStatement(closeSql)) {
                        ps.setLong(1, attendedStatusId);
                        ps.setLong(2, clinicId);
                        ps.setLong(3, appointmentId);
                        if (ps.executeUpdate() != 1) throw new SQLException("No se pudo cerrar la cita como ATENDIDA.");
                    }
                }

                cn.commit();
                return consultationId;
            } catch (SQLException | RuntimeException ex) {
                try { cn.rollback(); } catch (SQLException ignored) { }
                throw ex;
            } finally {
                try { cn.setAutoCommit(true); } catch (SQLException ignored) { }
            }
        }
    }

    @Override
    public List<HistoriaPacienteResumen> searchHistories(long clinicId, long dentistUserId, String query)
        throws SQLException {
        String q = query == null ? "" : query.trim();
        String like = "%" + q + "%";
        String sql = """
            SELECT p.id_paciente, CONCAT(p.nombres,' ',p.apellidos) AS paciente,
                   CONCAT(p.tipo_documento,' ',p.numero_documento) AS documento,
                   COALESCE(DATE_FORMAT(h.fecha_apertura,'%Y-%m-%d'),'Sin abrir') AS apertura,
                   COALESCE(h.estado,'PENDIENTE') AS estado,
                   COALESCE(DATE_FORMAT(MAX(co.fecha_consulta),'%Y-%m-%d %H:%i'),'—') AS ultima_consulta
            FROM pacientes p
            JOIN citas c ON c.id_paciente=p.id_paciente AND c.id_clinica=p.id_clinica
            JOIN odontologos o ON o.id_odontologo=c.id_odontologo AND o.id_clinica=p.id_clinica
            LEFT JOIN historias_clinicas h ON h.id_paciente=p.id_paciente
            LEFT JOIN citas cc ON cc.id_paciente=p.id_paciente AND cc.id_clinica=p.id_clinica
            LEFT JOIN consultas co ON co.id_cita=cc.id_cita
            WHERE p.id_clinica=? AND o.id_usuario=?
              AND (?='' OR p.nombres LIKE ? OR p.apellidos LIKE ? OR p.numero_documento LIKE ?)
            GROUP BY p.id_paciente,p.nombres,p.apellidos,p.tipo_documento,p.numero_documento,
                     h.fecha_apertura,h.estado
            ORDER BY MAX(co.fecha_consulta) IS NULL, MAX(co.fecha_consulta) DESC, paciente
            LIMIT 100
            """;
        List<HistoriaPacienteResumen> out = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, dentistUserId);
            ps.setString(3, q);
            ps.setString(4, like);
            ps.setString(5, like);
            ps.setString(6, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new HistoriaPacienteResumen(
                    rs.getLong("id_paciente"), rs.getString("paciente"), rs.getString("documento"),
                    rs.getString("apertura"), rs.getString("estado"), rs.getString("ultima_consulta")
                ));
            }
        }
        return out;
    }

    @Override
    public Optional<HistoriaPacienteVista> loadHistory(long clinicId, long dentistUserId, long patientId)
        throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            Optional<PacienteDetalle> patient = readVisiblePatient(cn, clinicId, dentistUserId, patientId);
            if (patient.isEmpty()) return Optional.empty();
            return Optional.of(new HistoriaPacienteVista(
                patient.get(), readHistory(cn, patientId), readAntecedents(cn, patientId),
                readAllergies(cn, patientId), readConsultations(cn, clinicId, patientId)
            ));
        }
    }

    @Override
    public void updateGeneralNotes(long clinicId, long dentistUserId, long patientId, String notes)
        throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                requireVisiblePatient(cn, clinicId, dentistUserId, patientId);
                ensureHistory(cn, patientId);
                try (PreparedStatement ps = cn.prepareStatement(
                    "UPDATE historias_clinicas SET observaciones_generales=? WHERE id_paciente=?")) {
                    ps.setString(1, observationsOrNull(notes));
                    ps.setLong(2, patientId);
                    if (ps.executeUpdate() != 1) throw new SQLException("No se pudo actualizar la historia clínica.");
                }
                cn.commit();
            } catch (SQLException | RuntimeException ex) {
                try { cn.rollback(); } catch (SQLException ignored) { }
                throw ex;
            } finally {
                try { cn.setAutoCommit(true); } catch (SQLException ignored) { }
            }
        }
    }

    @Override
    public long addMedicalHistory(
        long clinicId, long dentistUserId, long patientId,
        String type, String description, String observations
    ) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            requireVisiblePatient(cn, clinicId, dentistUserId, patientId);
            ensureHistory(cn, patientId);
            try (PreparedStatement ps = cn.prepareStatement("""
                INSERT INTO antecedentes_medicos (id_paciente,tipo,descripcion,observaciones)
                VALUES (?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, patientId);
                ps.setString(2, type);
                ps.setString(3, description);
                ps.setString(4, observationsOrNull(observations));
                if (ps.executeUpdate() != 1) throw new SQLException("No se pudo registrar el antecedente.");
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("MySQL no devolvió el id del antecedente.");
                    return keys.getLong(1);
                }
            }
        }
    }

    @Override
    public long addAllergy(
        long clinicId, long dentistUserId, long patientId,
        String allergen, String reaction, String observations
    ) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            requireVisiblePatient(cn, clinicId, dentistUserId, patientId);
            ensureHistory(cn, patientId);
            try (PreparedStatement ps = cn.prepareStatement("""
                INSERT INTO alergias (id_paciente,alergeno,reaccion,observaciones)
                VALUES (?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, patientId);
                ps.setString(2, allergen);
                ps.setString(3, observationsOrNull(reaction));
                ps.setString(4, observationsOrNull(observations));
                if (ps.executeUpdate() != 1) throw new SQLException("No se pudo registrar la alergia.");
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("MySQL no devolvió el id de la alergia.");
                    return keys.getLong(1);
                }
            }
        }
    }

    private Optional<PacienteDetalle> readVisiblePatient(
        Connection cn, long clinicId, long dentistUserId, long patientId
    ) throws SQLException {
        String sql = """
            SELECT p.id_paciente,p.tipo_documento,p.numero_documento,p.nombres,p.apellidos,
                   p.fecha_nacimiento,p.sexo,p.direccion,p.telefono,p.correo,p.ocupacion,
                   p.contacto_emergencia,p.telefono_emergencia,p.estado
            FROM pacientes p
            WHERE p.id_clinica=? AND p.id_paciente=?
              AND EXISTS (
                SELECT 1 FROM citas c
                JOIN odontologos o ON o.id_odontologo=c.id_odontologo AND o.id_clinica=c.id_clinica
                WHERE c.id_clinica=p.id_clinica AND c.id_paciente=p.id_paciente AND o.id_usuario=?
              )
            LIMIT 1
            """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, patientId);
            ps.setLong(3, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Date birth = rs.getDate("fecha_nacimiento");
                return Optional.of(new PacienteDetalle(
                    rs.getLong("id_paciente"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
                    rs.getString("nombres"), rs.getString("apellidos"), birth == null ? null : birth.toLocalDate(),
                    rs.getString("sexo"), rs.getString("direccion"), rs.getString("telefono"), rs.getString("correo"),
                    rs.getString("ocupacion"), rs.getString("contacto_emergencia"), rs.getString("telefono_emergencia"),
                    rs.getString("estado")
                ));
            }
        }
    }

    private void requireVisiblePatient(Connection cn, long clinicId, long dentistUserId, long patientId)
        throws SQLException {
        if (readVisiblePatient(cn, clinicId, dentistUserId, patientId).isEmpty()) {
            throw new SQLException("El paciente no está asignado al odontólogo conectado.", "45000");
        }
    }

    private Optional<HistoriaClinicaDetalle> readHistory(Connection cn, long patientId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
            SELECT id_historia,DATE_FORMAT(fecha_apertura,'%Y-%m-%d') AS apertura,
                   COALESCE(observaciones_generales,'') AS observaciones,estado
            FROM historias_clinicas WHERE id_paciente=? LIMIT 1
            """)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new HistoriaClinicaDetalle(
                    rs.getLong("id_historia"), rs.getString("apertura"),
                    rs.getString("observaciones"), rs.getString("estado")
                ));
            }
        }
    }

    private List<AntecedenteClinicoDetalle> readAntecedents(Connection cn, long patientId) throws SQLException {
        List<AntecedenteClinicoDetalle> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement("""
            SELECT id_antecedente,tipo,descripcion,COALESCE(observaciones,'') AS observaciones,
                   DATE_FORMAT(fecha_registro,'%Y-%m-%d') AS fecha
            FROM antecedentes_medicos WHERE id_paciente=? ORDER BY fecha_registro DESC,id_antecedente DESC
            """)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new AntecedenteClinicoDetalle(
                    rs.getLong("id_antecedente"), rs.getString("tipo"), rs.getString("descripcion"),
                    rs.getString("observaciones"), rs.getString("fecha")
                ));
            }
        }
        return out;
    }

    private List<AlergiaClinicaDetalle> readAllergies(Connection cn, long patientId) throws SQLException {
        List<AlergiaClinicaDetalle> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement("""
            SELECT id_alergia,alergeno,COALESCE(reaccion,'') AS reaccion,
                   COALESCE(observaciones,'') AS observaciones,
                   DATE_FORMAT(fecha_registro,'%Y-%m-%d') AS fecha
            FROM alergias WHERE id_paciente=? ORDER BY fecha_registro DESC,id_alergia DESC
            """)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new AlergiaClinicaDetalle(
                    rs.getLong("id_alergia"), rs.getString("alergeno"), rs.getString("reaccion"),
                    rs.getString("observaciones"), rs.getString("fecha")
                ));
            }
        }
        return out;
    }

    private List<ConsultaHistorialDetalle> readConsultations(Connection cn, long clinicId, long patientId)
        throws SQLException {
        List<ConsultaHistorialDetalle> out = new ArrayList<>();
        String sql = """
            SELECT co.id_consulta,co.id_cita,DATE_FORMAT(co.fecha_consulta,'%Y-%m-%d %H:%i') AS fecha,
                   CONCAT(o.nombres,' ',o.apellidos) AS odontologo,COALESCE(es.nombre,'') AS especialidad,
                   COALESCE(co.motivo_consulta,'') AS motivo,COALESCE(co.diagnostico,'') AS diagnostico,
                   COALESCE(co.tratamiento,'') AS tratamiento,COALESCE(co.observaciones,'') AS observaciones,
                   COALESCE(co.recomendaciones,'') AS recomendaciones
            FROM consultas co
            JOIN citas c ON c.id_cita=co.id_cita
            JOIN odontologos o ON o.id_odontologo=co.id_odontologo
            LEFT JOIN especialidades es ON es.id_especialidad=c.id_especialidad
            WHERE c.id_clinica=? AND c.id_paciente=?
            ORDER BY co.fecha_consulta DESC,co.id_consulta DESC
            """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new ConsultaHistorialDetalle(
                    rs.getLong("id_consulta"), rs.getLong("id_cita"), rs.getString("fecha"),
                    rs.getString("odontologo"), rs.getString("especialidad"), rs.getString("motivo"),
                    rs.getString("diagnostico"), rs.getString("tratamiento"), rs.getString("observaciones"),
                    rs.getString("recomendaciones")
                ));
            }
        }
        return out;
    }

    private void ensureHistory(Connection cn, long patientId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("""
            INSERT INTO historias_clinicas (id_paciente,fecha_apertura,observaciones_generales,estado)
            SELECT ?,CURRENT_DATE,NULL,'ACTIVA'
            WHERE NOT EXISTS (SELECT 1 FROM historias_clinicas WHERE id_paciente=?)
            """)) {
            ps.setLong(1, patientId);
            ps.setLong(2, patientId);
            ps.executeUpdate();
        }
    }

    private long dentistIdForUser(Connection cn, long clinicId, long dentistUserId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT id_odontologo FROM odontologos WHERE id_clinica=? AND id_usuario=? LIMIT 1")) {
            ps.setLong(1, clinicId);
            ps.setLong(2, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No existe un perfil de odontólogo asociado a la cuenta conectada.", "45000");
                }
                return rs.getLong(1);
            }
        }
    }

    private long appointmentStatusId(Connection cn, String name) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT id_estado_cita FROM estados_cita WHERE nombre=? LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No existe el estado de cita requerido: " + name, "45000");
                }
                return rs.getLong(1);
            }
        }
    }

    private String observationsOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
