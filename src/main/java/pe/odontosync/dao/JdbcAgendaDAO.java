package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.CitaDetalle;
import pe.odontosync.model.CitaEstadoOpcion;
import pe.odontosync.model.CitaResumen;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoOpcion;
import pe.odontosync.model.PacienteOpcion;

/** Acceso JDBC de consulta y CRUD para la agenda de citas. */
public final class JdbcAgendaDAO implements AgendaCrudDAO {
    private static final String FIND_BY_CLINIC = """
        SELECT c.fecha, c.hora_inicio,
               CONCAT(p.nombres,' ',p.apellidos) AS paciente,
               CONCAT(o.nombres,' ',o.apellidos) AS odontologo,
               e.nombre AS estado,
               c.motivo
        FROM citas c
        JOIN pacientes p ON p.id_paciente = c.id_paciente
        JOIN odontologos o ON o.id_odontologo = c.id_odontologo
        JOIN estados_cita e ON e.id_estado_cita = c.id_estado_cita
        WHERE c.id_clinica = ?
          AND p.id_clinica = ?
          AND o.id_clinica = ?
        ORDER BY c.fecha DESC, c.hora_inicio
        LIMIT 50
        """;

    private static final String FIND_BY_DENTIST = """
        SELECT c.fecha, c.hora_inicio,
               CONCAT(p.nombres,' ',p.apellidos) AS paciente,
               CONCAT(o.nombres,' ',o.apellidos) AS odontologo,
               e.nombre AS estado,
               c.motivo
        FROM citas c
        JOIN pacientes p ON p.id_paciente = c.id_paciente
        JOIN estados_cita e ON e.id_estado_cita = c.id_estado_cita
        JOIN odontologos o ON o.id_odontologo = c.id_odontologo
        WHERE c.id_clinica = ?
          AND p.id_clinica = ?
          AND o.id_clinica = ?
          AND o.id_usuario = ?
        ORDER BY c.fecha DESC, c.hora_inicio
        LIMIT 50
        """;

    private static final String DETAIL_SELECT = """
        SELECT c.id_cita,c.id_paciente,
               CONCAT(p.nombres,' ',p.apellidos) paciente,
               CONCAT(p.tipo_documento,' ',p.numero_documento) documento,
               c.id_odontologo,CONCAT(o.nombres,' ',o.apellidos) odontologo,
               c.id_especialidad,COALESCE(es.nombre,'') especialidad,
               c.id_estado_cita,ec.nombre estado,
               c.fecha,c.hora_inicio,c.hora_fin,
               COALESCE(c.motivo,'') motivo,COALESCE(c.observaciones,'') observaciones
        FROM citas c
        JOIN pacientes p ON p.id_paciente=c.id_paciente AND p.id_clinica=c.id_clinica
        JOIN odontologos o ON o.id_odontologo=c.id_odontologo AND o.id_clinica=c.id_clinica
        JOIN estados_cita ec ON ec.id_estado_cita=c.id_estado_cita
        LEFT JOIN especialidades es ON es.id_especialidad=c.id_especialidad
        """;

    @Override
    public List<CitaResumen> findByClinic(long clinicId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_CLINIC)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setLong(2, clinicId);
            ps.setLong(3, clinicId);
            try (ResultSet rs = ps.executeQuery()) { return readSummary(rs); }
        }
    }

    @Override
    public List<CitaResumen> findByDentist(long clinicId, long dentistUserId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_DENTIST)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setLong(2, clinicId);
            ps.setLong(3, clinicId);
            ps.setLong(4, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) { return readSummary(rs); }
        }
    }

    @Override
    public List<CitaDetalle> searchDetailed(
        long clinicId, Long dentistUserId, LocalDate from, LocalDate to, String status, String query
    ) throws SQLException {
        StringBuilder sql = new StringBuilder(DETAIL_SELECT).append(" WHERE c.id_clinica=? ");
        List<Object> params = new ArrayList<>();
        params.add(clinicId);
        if (dentistUserId != null) {
            sql.append(" AND o.id_usuario=? ");
            params.add(dentistUserId);
        }
        if (from != null) {
            sql.append(" AND c.fecha>=? ");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND c.fecha<=? ");
            params.add(to);
        }
        String st = status == null ? "" : status.trim();
        if (!st.isEmpty()) {
            sql.append(" AND ec.nombre=? ");
            params.add(st);
        }
        String q = query == null ? "" : query.trim();
        if (!q.isEmpty()) {
            sql.append(" AND (p.numero_documento LIKE ? OR p.nombres LIKE ? OR p.apellidos LIKE ? "
                + "OR o.nombres LIKE ? OR o.apellidos LIKE ? OR COALESCE(c.motivo,'') LIKE ?) ");
            String like = "%" + q + "%";
            for (int i = 0; i < 6; i++) params.add(like);
        }
        sql.append(" ORDER BY (c.fecha>=CURRENT_DATE) DESC, c.fecha ASC, c.hora_inicio ASC LIMIT 200");

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            ps.setQueryTimeout(8);
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<CitaDetalle> result = new ArrayList<>();
                while (rs.next()) result.add(readDetailed(rs));
                return result;
            }
        }
    }

    @Override
    public Optional<CitaDetalle> findDetailedById(long clinicId, long appointmentId) throws SQLException {
        String sql = DETAIL_SELECT + " WHERE c.id_clinica=? AND c.id_cita=?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(readDetailed(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<PacienteOpcion> findActivePatients(long clinicId) throws SQLException {
        String sql = """
            SELECT id_paciente,CONCAT(nombres,' ',apellidos) nombre,
                   CONCAT(tipo_documento,' ',numero_documento) documento,estado
            FROM pacientes
            WHERE id_clinica=? AND estado='ACTIVO'
            ORDER BY apellidos,nombres
            LIMIT 300
            """;
        List<PacienteOpcion> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new PacienteOpcion(
                    rs.getLong("id_paciente"), rs.getString("nombre"), rs.getString("documento"), rs.getString("estado")
                ));
            }
        }
        return result;
    }

    @Override
    public List<OdontologoOpcion> findActiveDentists(long clinicId) throws SQLException {
        String sql = """
            SELECT id_odontologo,CONCAT(nombres,' ',apellidos) nombre,estado
            FROM odontologos
            WHERE id_clinica=? AND estado='ACTIVO'
            ORDER BY apellidos,nombres
            """;
        List<OdontologoOpcion> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new OdontologoOpcion(
                    rs.getLong("id_odontologo"), rs.getString("nombre"), rs.getString("estado")
                ));
            }
        }
        return result;
    }

    @Override
    public List<EspecialidadOpcion> findActiveSpecialties() throws SQLException {
        String sql = "SELECT id_especialidad,nombre,estado FROM especialidades WHERE estado='ACTIVA' ORDER BY nombre";
        List<EspecialidadOpcion> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(new EspecialidadOpcion(
                rs.getLong("id_especialidad"), rs.getString("nombre"), rs.getString("estado")
            ));
        }
        return result;
    }

    @Override
    public Map<Long, List<Long>> findActiveDentistSpecialtyIds(long clinicId) throws SQLException {
        String sql = """
            SELECT o.id_odontologo, oe.id_especialidad
            FROM odontologos o
            JOIN odontologo_especialidad oe ON oe.id_odontologo=o.id_odontologo
            JOIN especialidades e ON e.id_especialidad=oe.id_especialidad
            WHERE o.id_clinica=? AND o.estado='ACTIVO' AND e.estado='ACTIVA'
            ORDER BY o.id_odontologo, e.nombre
            """;
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.computeIfAbsent(rs.getLong("id_odontologo"), k -> new ArrayList<>())
                        .add(rs.getLong("id_especialidad"));
                }
            }
        }
        return result;
    }

    @Override
    public List<CitaEstadoOpcion> findStates() throws SQLException {
        String sql = "SELECT id_estado_cita,nombre FROM estados_cita ORDER BY id_estado_cita";
        List<CitaEstadoOpcion> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(new CitaEstadoOpcion(rs.getLong(1), rs.getString(2)));
        }
        return result;
    }

    @Override
    public boolean patientIsActive(long clinicId, long patientId) throws SQLException {
        return exists("SELECT 1 FROM pacientes WHERE id_clinica=? AND id_paciente=? AND estado='ACTIVO' LIMIT 1",
            clinicId, patientId);
    }

    @Override
    public boolean dentistIsActive(long clinicId, long dentistId) throws SQLException {
        return exists("SELECT 1 FROM odontologos WHERE id_clinica=? AND id_odontologo=? AND estado='ACTIVO' LIMIT 1",
            clinicId, dentistId);
    }

    @Override
    public boolean dentistBelongsToUser(long clinicId, long dentistId, long dentistUserId) throws SQLException {
        String sql = "SELECT 1 FROM odontologos WHERE id_clinica=? AND id_odontologo=? AND id_usuario=? LIMIT 1";
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId); ps.setLong(2, dentistId); ps.setLong(3, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public boolean specialtyAssignedToDentist(long dentistId, long specialtyId) throws SQLException {
        String sql = """
            SELECT 1
            FROM odontologo_especialidad oe
            JOIN especialidades e ON e.id_especialidad=oe.id_especialidad
            WHERE oe.id_odontologo=? AND oe.id_especialidad=? AND e.estado='ACTIVA'
            LIMIT 1
            """;
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, dentistId); ps.setLong(2, specialtyId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public Optional<Long> findCoveringSchedule(
        long clinicId, long dentistId, int dayOfWeek, LocalTime start, LocalTime end
    ) throws SQLException {
        String sql = """
            SELECT h.id_horario
            FROM horarios h
            JOIN odontologos o ON o.id_odontologo=h.id_odontologo
            WHERE o.id_clinica=? AND o.estado='ACTIVO'
              AND h.id_odontologo=? AND h.dia_semana=? AND h.estado='ACTIVO'
              AND h.hora_inicio<=? AND h.hora_fin>=?
            ORDER BY h.hora_inicio DESC
            LIMIT 1
            """;
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, dentistId);
            ps.setInt(3, dayOfWeek);
            ps.setTime(4, Time.valueOf(start));
            ps.setTime(5, Time.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty(); }
        }
    }

    @Override
    public boolean isBlocked(long clinicId, long dentistId, LocalDate date, LocalTime start, LocalTime end)
        throws SQLException {
        String sql = """
            SELECT 1
            FROM bloqueos_agenda b
            JOIN odontologos o ON o.id_odontologo=b.id_odontologo
            WHERE o.id_clinica=? AND b.id_odontologo=? AND b.fecha=? AND b.estado='ACTIVO'
              AND (b.hora_inicio IS NULL OR (b.hora_inicio < ? AND b.hora_fin > ?))
            LIMIT 1
            """;
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId); ps.setLong(2, dentistId); ps.setDate(3, Date.valueOf(date));
            ps.setTime(4, Time.valueOf(end)); ps.setTime(5, Time.valueOf(start));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public boolean dentistOverlaps(
        long clinicId, long dentistId, LocalDate date, LocalTime start, LocalTime end, Long excludeAppointmentId
    ) throws SQLException {
        return appointmentOverlaps("c.id_odontologo=?", clinicId, dentistId, date, start, end, excludeAppointmentId);
    }

    @Override
    public boolean patientOverlaps(
        long clinicId, long patientId, LocalDate date, LocalTime start, LocalTime end, Long excludeAppointmentId
    ) throws SQLException {
        return appointmentOverlaps("c.id_paciente=?", clinicId, patientId, date, start, end, excludeAppointmentId);
    }

    @Override
    public long create(
        long clinicId, long patientId, long dentistId, Long specialtyId, Long scheduleId,
        long stateId, LocalDate date, LocalTime start, LocalTime end,
        String reason, String notes, long createdBy
    ) throws SQLException {
        String sql = """
            INSERT INTO citas
              (id_clinica,id_paciente,id_odontologo,id_especialidad,id_horario,id_estado_cita,
               fecha,hora_inicio,hora_fin,motivo,observaciones,creado_por)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i=1;
            ps.setLong(i++, clinicId); ps.setLong(i++, patientId); ps.setLong(i++, dentistId);
            setNullableLong(ps, i++, specialtyId); setNullableLong(ps, i++, scheduleId);
            ps.setLong(i++, stateId); ps.setDate(i++, Date.valueOf(date));
            ps.setTime(i++, Time.valueOf(start)); ps.setTime(i++, Time.valueOf(end));
            ps.setString(i++, blankToNull(reason)); ps.setString(i++, blankToNull(notes)); ps.setLong(i, createdBy);
            if (ps.executeUpdate()!=1) throw new SQLException("No se pudo registrar la cita.");
            try (ResultSet keys=ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id de la cita.");
                return keys.getLong(1);
            }
        }
    }

    @Override
    public void update(
        long clinicId, long appointmentId, long patientId, long dentistId, Long specialtyId, Long scheduleId,
        LocalDate date, LocalTime start, LocalTime end, String reason, String notes
    ) throws SQLException {
        String sql = """
            UPDATE citas
            SET id_paciente=?,id_odontologo=?,id_especialidad=?,id_horario=?,fecha=?,
                hora_inicio=?,hora_fin=?,motivo=?,observaciones=?
            WHERE id_clinica=? AND id_cita=?
            """;
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            int i=1;
            ps.setLong(i++, patientId); ps.setLong(i++, dentistId);
            setNullableLong(ps, i++, specialtyId); setNullableLong(ps, i++, scheduleId);
            ps.setDate(i++, Date.valueOf(date)); ps.setTime(i++, Time.valueOf(start)); ps.setTime(i++, Time.valueOf(end));
            ps.setString(i++, blankToNull(reason)); ps.setString(i++, blankToNull(notes));
            ps.setLong(i++, clinicId); ps.setLong(i, appointmentId);
            if (ps.executeUpdate()!=1) throw new SQLException("No se encontró la cita a actualizar.");
        }
    }

    @Override
    public void updateStatus(long clinicId, long appointmentId, long stateId) throws SQLException {
        String sql = "UPDATE citas SET id_estado_cita=? WHERE id_clinica=? AND id_cita=?";
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, stateId); ps.setLong(2, clinicId); ps.setLong(3, appointmentId);
            if (ps.executeUpdate()!=1) throw new SQLException("No se encontró la cita para actualizar su estado.");
        }
    }

    private boolean appointmentOverlaps(
        String ownerClause, long clinicId, long ownerId, LocalDate date,
        LocalTime start, LocalTime end, Long excludeAppointmentId
    ) throws SQLException {
        String excludeClause = excludeAppointmentId == null ? "" : "AND c.id_cita<>?";
        String sql = """
            SELECT 1
            FROM citas c
            JOIN estados_cita ec ON ec.id_estado_cita=c.id_estado_cita
            WHERE c.id_clinica=?
              AND %s
              AND c.fecha=?
              AND ec.nombre NOT IN ('CANCELADA','NO_ASISTIO')
              AND c.hora_inicio < ?
              AND c.hora_fin > ?
              %s
            LIMIT 1
            """.formatted(ownerClause, excludeClause);
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            int i=1;
            ps.setLong(i++, clinicId); ps.setLong(i++, ownerId); ps.setDate(i++, Date.valueOf(date));
            ps.setTime(i++, Time.valueOf(end)); ps.setTime(i++, Time.valueOf(start));
            if (excludeAppointmentId != null) ps.setLong(i, excludeAppointmentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private boolean exists(String sql, long a, long b) throws SQLException {
        try (Connection cn=DatabaseConnection.getConnection(); PreparedStatement ps=cn.prepareStatement(sql)) {
            ps.setLong(1,a); ps.setLong(2,b);
            try (ResultSet rs=ps.executeQuery()) { return rs.next(); }
        }
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i=0;i<params.size();i++) {
            Object value=params.get(i);
            int p=i+1;
            if (value instanceof Long x) ps.setLong(p,x);
            else if (value instanceof LocalDate x) ps.setDate(p,Date.valueOf(x));
            else ps.setString(p,String.valueOf(value));
        }
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.BIGINT); else ps.setLong(index,value);
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

    private CitaDetalle readDetailed(ResultSet rs) throws SQLException {
        long specialty = rs.getLong("id_especialidad");
        Long specialtyId = rs.wasNull() ? null : specialty;
        return new CitaDetalle(
            rs.getLong("id_cita"), rs.getLong("id_paciente"), rs.getString("paciente"), rs.getString("documento"),
            rs.getLong("id_odontologo"), rs.getString("odontologo"), specialtyId, rs.getString("especialidad"),
            rs.getLong("id_estado_cita"), rs.getString("estado"), rs.getDate("fecha").toLocalDate(),
            rs.getTime("hora_inicio").toLocalTime(), rs.getTime("hora_fin").toLocalTime(),
            rs.getString("motivo"), rs.getString("observaciones")
        );
    }

    private List<CitaResumen> readSummary(ResultSet rs) throws SQLException {
        List<CitaResumen> appointments = new ArrayList<>();
        while (rs.next()) {
            appointments.add(new CitaResumen(
                rs.getString("fecha"), rs.getString("hora_inicio"), rs.getString("paciente"),
                rs.getString("odontologo"), rs.getString("estado"), rs.getString("motivo")
            ));
        }
        return appointments;
    }
}
