package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.HorarioDetalle;
import pe.odontosync.model.HorarioResumen;
import pe.odontosync.model.OdontologoOpcion;

/** Acceso JDBC de consulta y CRUD de horarios semanales. */
public final class JdbcHorarioDAO implements HorarioDAO, HorarioCrudDAO {
    private static final String FIND_BY_CLINIC = """
        SELECT CONCAT(o.nombres,' ',o.apellidos) AS odontologo,
               h.dia_semana, h.hora_inicio, h.hora_fin, h.estado
        FROM horarios h
        JOIN odontologos o ON o.id_odontologo = h.id_odontologo
        WHERE o.id_clinica = ?
        ORDER BY o.apellidos, h.dia_semana, h.hora_inicio
        LIMIT 50
        """;

    private static final String FIND_BY_DENTIST = """
        SELECT CONCAT(o.nombres,' ',o.apellidos) AS odontologo,
               h.dia_semana, h.hora_inicio, h.hora_fin, h.estado
        FROM horarios h
        JOIN odontologos o ON o.id_odontologo = h.id_odontologo
        WHERE o.id_clinica = ?
          AND o.id_usuario = ?
        ORDER BY h.dia_semana, h.hora_inicio
        LIMIT 50
        """;

    @Override
    public List<HorarioResumen> findByClinic(long clinicId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_CLINIC)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) { return readSummary(rs); }
        }
    }

    @Override
    public List<HorarioResumen> findByDentist(long clinicId, long dentistUserId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_DENTIST)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setLong(2, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) { return readSummary(rs); }
        }
    }

    @Override
    public List<HorarioDetalle> findDetailedByClinic(long clinicId) throws SQLException {
        String sql = """
            SELECT h.id_horario,h.id_odontologo,CONCAT(o.nombres,' ',o.apellidos) odontologo,
                   h.dia_semana,h.hora_inicio,h.hora_fin,h.estado
            FROM horarios h
            JOIN odontologos o ON o.id_odontologo=h.id_odontologo
            WHERE o.id_clinica=?
            ORDER BY o.apellidos,o.nombres,h.dia_semana,h.hora_inicio
            """;
        return readDetailedList(sql, clinicId, null);
    }

    @Override
    public List<HorarioDetalle> findDetailedByDentistUser(long clinicId, long userId) throws SQLException {
        String sql = """
            SELECT h.id_horario,h.id_odontologo,CONCAT(o.nombres,' ',o.apellidos) odontologo,
                   h.dia_semana,h.hora_inicio,h.hora_fin,h.estado
            FROM horarios h
            JOIN odontologos o ON o.id_odontologo=h.id_odontologo
            WHERE o.id_clinica=? AND o.id_usuario=?
            ORDER BY h.dia_semana,h.hora_inicio
            """;
        return readDetailedList(sql, clinicId, userId);
    }

    @Override
    public Optional<HorarioDetalle> findDetailedById(long clinicId, long scheduleId) throws SQLException {
        String sql = """
            SELECT h.id_horario,h.id_odontologo,CONCAT(o.nombres,' ',o.apellidos) odontologo,
                   h.dia_semana,h.hora_inicio,h.hora_fin,h.estado
            FROM horarios h
            JOIN odontologos o ON o.id_odontologo=h.id_odontologo
            WHERE o.id_clinica=? AND h.id_horario=?
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(readDetailed(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<OdontologoOpcion> findDentistsByClinic(long clinicId) throws SQLException {
        String sql = """
            SELECT id_odontologo,CONCAT(nombres,' ',apellidos) nombre,estado
            FROM odontologos
            WHERE id_clinica=?
            ORDER BY CASE estado WHEN 'ACTIVO' THEN 0 ELSE 1 END, apellidos,nombres
            """;
        List<OdontologoOpcion> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
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
    public boolean dentistBelongsToClinic(long clinicId, long dentistId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "SELECT 1 FROM odontologos WHERE id_clinica=? AND id_odontologo=? LIMIT 1")) {
            ps.setLong(1, clinicId);
            ps.setLong(2, dentistId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public boolean dentistIsActive(long clinicId, long dentistId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "SELECT 1 FROM odontologos WHERE id_clinica=? AND id_odontologo=? AND estado='ACTIVO' LIMIT 1")) {
            ps.setLong(1, clinicId);
            ps.setLong(2, dentistId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public boolean overlaps(
        long clinicId, long dentistId, int day, LocalTime start, LocalTime end, Long excludeScheduleId
    ) throws SQLException {
        String sql = """
            SELECT 1
            FROM horarios h
            JOIN odontologos o ON o.id_odontologo=h.id_odontologo
            WHERE o.id_clinica=? AND h.id_odontologo=? AND h.dia_semana=?
              AND h.estado='ACTIVO'
              AND h.hora_inicio < ? AND h.hora_fin > ?
            """ + (excludeScheduleId == null ? "" : " AND h.id_horario<>?") + " LIMIT 1";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, clinicId);
            ps.setLong(i++, dentistId);
            ps.setInt(i++, day);
            ps.setTime(i++, Time.valueOf(end));
            ps.setTime(i++, Time.valueOf(start));
            if (excludeScheduleId != null) ps.setLong(i, excludeScheduleId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public long create(long clinicId, long dentistId, int day, LocalTime start, LocalTime end, String estado)
        throws SQLException {
        String sql = """
            INSERT INTO horarios (id_odontologo,dia_semana,hora_inicio,hora_fin,estado)
            SELECT id_odontologo,?,?,?,?
            FROM odontologos WHERE id_clinica=? AND id_odontologo=?
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, day);
            ps.setTime(2, Time.valueOf(start));
            ps.setTime(3, Time.valueOf(end));
            ps.setString(4, estado);
            ps.setLong(5, clinicId);
            ps.setLong(6, dentistId);
            if (ps.executeUpdate() != 1) throw new SQLException("No se encontró el odontólogo para registrar el horario.");
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id del horario.");
                return keys.getLong(1);
            }
        }
    }

    @Override
    public void update(
        long clinicId, long scheduleId, long dentistId, int day, LocalTime start, LocalTime end, String estado
    ) throws SQLException {
        String sql = """
            UPDATE horarios h
            JOIN odontologos current_o ON current_o.id_odontologo=h.id_odontologo
            JOIN odontologos selected_o ON selected_o.id_odontologo=? AND selected_o.id_clinica=?
            SET h.id_odontologo=?,h.dia_semana=?,h.hora_inicio=?,h.hora_fin=?,h.estado=?
            WHERE h.id_horario=? AND current_o.id_clinica=?
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, dentistId);
            ps.setLong(2, clinicId);
            ps.setLong(3, dentistId);
            ps.setInt(4, day);
            ps.setTime(5, Time.valueOf(start));
            ps.setTime(6, Time.valueOf(end));
            ps.setString(7, estado);
            ps.setLong(8, scheduleId);
            ps.setLong(9, clinicId);
            if (ps.executeUpdate() != 1) throw new SQLException("No se encontró el horario a actualizar.");
        }
    }

    @Override
    public void updateStatus(long clinicId, long scheduleId, String estado) throws SQLException {
        String sql = """
            UPDATE horarios h
            JOIN odontologos o ON o.id_odontologo=h.id_odontologo
            SET h.estado=?
            WHERE o.id_clinica=? AND h.id_horario=?
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setLong(2, clinicId);
            ps.setLong(3, scheduleId);
            if (ps.executeUpdate() != 1) throw new SQLException("No se encontró el horario.");
        }
    }

    private List<HorarioResumen> readSummary(ResultSet rs) throws SQLException {
        List<HorarioResumen> schedules = new ArrayList<>();
        while (rs.next()) {
            schedules.add(new HorarioResumen(
                rs.getString("odontologo"),
                dayName(rs.getInt("dia_semana")),
                rs.getTime("hora_inicio").toLocalTime().toString(),
                rs.getTime("hora_fin").toLocalTime().toString(),
                rs.getString("estado")
            ));
        }
        return schedules;
    }

    private List<HorarioDetalle> readDetailedList(String sql, long clinicId, Long userId) throws SQLException {
        List<HorarioDetalle> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            if (userId != null) ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(readDetailed(rs));
            }
        }
        return result;
    }

    private HorarioDetalle readDetailed(ResultSet rs) throws SQLException {
        return new HorarioDetalle(
            rs.getLong("id_horario"), rs.getLong("id_odontologo"), rs.getString("odontologo"),
            rs.getInt("dia_semana"), rs.getTime("hora_inicio").toLocalTime(),
            rs.getTime("hora_fin").toLocalTime(), rs.getString("estado")
        );
    }

    private String dayName(int day) {
        return switch (day) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sábado";
            case 7 -> "Domingo";
            default -> "Día " + day;
        };
    }
}
