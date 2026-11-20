package pe.odontosync.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.DashboardCategory;
import pe.odontosync.model.DashboardPoint;
import pe.odontosync.model.DashboardSummary;

/**
 * Consultas agregadas del dashboard analítico.
 *
 * Todas las consultas parten del id de clínica recibido desde la sesión
 * autenticada por DashboardAnalyticsService.
 */
public final class JdbcDashboardAnalyticsDAO implements DashboardAnalyticsDAO {
    private static final int QUERY_TIMEOUT_SECONDS = 8;

    @Override
    public DashboardSummary summary(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            int patients = scalarInt(
                cn,
                """
                SELECT COUNT(*)
                FROM pacientes
                WHERE id_clinica=? AND estado='ACTIVO'
                """,
                clinicId
            );

            int appointments = scalarInt(
                cn,
                appointmentCountSql(dentistUserId, null),
                appointmentParams(clinicId, from, to, dentistUserId)
            );

            BigDecimal income = scalarDecimal(
                cn,
                incomeSql(dentistUserId, false),
                incomeParams(clinicId, from, to, dentistUserId)
            );

            int attended = scalarInt(
                cn,
                appointmentCountSql(dentistUserId, "ATENDIDA"),
                appointmentParams(clinicId, from, to, dentistUserId)
            );

            int completed = scalarInt(
                cn,
                completedCountSql(dentistUserId),
                appointmentParams(clinicId, from, to, dentistUserId)
            );

            double attendanceRate = completed == 0
                ? 0.0
                : (attended * 100.0) / completed;

            return new DashboardSummary(
                patients,
                appointments,
                income,
                attendanceRate
            );
        }
    }

    @Override
    public List<DashboardPoint> appointmentsTrend(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) throws SQLException {
        String sql = """
            SELECT DATE_FORMAT(citas_dia.fecha_dia, '%d/%m') AS etiqueta,
                   COUNT(*) AS valor
            FROM (
                SELECT c.fecha AS fecha_dia
                FROM citas c
                JOIN odontologos o ON o.id_odontologo=c.id_odontologo
                WHERE c.id_clinica=? AND c.fecha BETWEEN ? AND ?
            """ + dentistClause(dentistUserId) + """
            ) citas_dia
            GROUP BY citas_dia.fecha_dia
            ORDER BY citas_dia.fecha_dia
            """;

        return points(
            sql,
            appointmentParams(clinicId, from, to, dentistUserId)
        );
    }

    @Override
    public List<DashboardCategory> appointmentStatus(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) throws SQLException {
        String sql = """
            SELECT e.nombre AS etiqueta, COUNT(*) AS valor
            FROM citas c
            JOIN odontologos o ON o.id_odontologo=c.id_odontologo
            JOIN estados_cita e ON e.id_estado_cita=c.id_estado_cita
            WHERE c.id_clinica=? AND c.fecha BETWEEN ? AND ?
            """ + dentistClause(dentistUserId) + """
            GROUP BY e.id_estado_cita, e.nombre
            ORDER BY valor DESC, e.nombre
            """;

        return categories(
            sql,
            appointmentParams(clinicId, from, to, dentistUserId),
            true
        );
    }

    @Override
    public List<DashboardPoint> incomeTrend(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) throws SQLException {
        return points(
            incomeSql(dentistUserId, true),
            incomeParams(clinicId, from, to, dentistUserId)
        );
    }

    @Override
    public List<DashboardPoint> newPatientsTrend(
        long clinicId,
        LocalDate from,
        LocalDate to
    ) throws SQLException {
        String sql = """
            SELECT DATE_FORMAT(pacientes_dia.fecha_dia, '%d/%m') AS etiqueta,
                   COUNT(*) AS valor
            FROM (
                SELECT DATE(fecha_registro) AS fecha_dia
                FROM pacientes
                WHERE id_clinica=?
                  AND DATE(fecha_registro) BETWEEN ? AND ?
            ) pacientes_dia
            GROUP BY pacientes_dia.fecha_dia
            ORDER BY pacientes_dia.fecha_dia
            """;

        return points(sql, clinicId, Date.valueOf(from), Date.valueOf(to));
    }

    @Override
    public List<DashboardCategory> attentionsByDentist(
        long clinicId,
        LocalDate from,
        LocalDate to
    ) throws SQLException {
        String sql = """
            SELECT CONCAT(o.nombres, ' ', o.apellidos) AS etiqueta,
                   COUNT(*) AS valor
            FROM citas c
            JOIN odontologos o ON o.id_odontologo=c.id_odontologo
            JOIN estados_cita e ON e.id_estado_cita=c.id_estado_cita
            WHERE c.id_clinica=?
              AND c.fecha BETWEEN ? AND ?
              AND e.nombre='ATENDIDA'
            GROUP BY o.id_odontologo, o.nombres, o.apellidos
            ORDER BY valor DESC, o.apellidos, o.nombres
            """;

        return categories(
            sql,
            new Object[] { clinicId, Date.valueOf(from), Date.valueOf(to) },
            false
        );
    }

    @Override
    public List<DashboardCategory> inventoryStatus(long clinicId)
        throws SQLException {
        String sql = """
            SELECT estado_stock AS etiqueta, COUNT(*) AS valor
            FROM (
                SELECT CASE
                    WHEN stock_actual=0 THEN 'SIN_STOCK'
                    WHEN stock_actual<=stock_minimo THEN 'STOCK_BAJO'
                    ELSE 'NORMAL'
                END AS estado_stock
                FROM insumos
                WHERE id_clinica=? AND estado='ACTIVO'
            ) inventario_estado
            GROUP BY estado_stock
            ORDER BY FIELD(estado_stock,'SIN_STOCK','STOCK_BAJO','NORMAL')
            """;

        return categories(sql, new Object[] { clinicId }, true);
    }

    @Override
    public List<DashboardCategory> paymentMethods(
        long clinicId,
        LocalDate from,
        LocalDate to
    ) throws SQLException {
        String sql = """
            SELECT p.metodo_pago AS etiqueta, COALESCE(SUM(p.monto),0) AS valor
            FROM pagos p
            JOIN citas c ON c.id_cita=p.id_cita
            WHERE c.id_clinica=?
              AND DATE(p.fecha_pago) BETWEEN ? AND ?
            GROUP BY p.metodo_pago
            ORDER BY valor DESC, p.metodo_pago
            """;

        return categories(
            sql,
            new Object[] { clinicId, Date.valueOf(from), Date.valueOf(to) },
            true
        );
    }

    private String appointmentCountSql(
        Long dentistUserId,
        String status
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM citas c
            JOIN odontologos o ON o.id_odontologo=c.id_odontologo
            """);

        if (status != null) {
            sql.append("""
                JOIN estados_cita e ON e.id_estado_cita=c.id_estado_cita
                """);
        }

        sql.append("""
            WHERE c.id_clinica=? AND c.fecha BETWEEN ? AND ?
            """);

        sql.append(dentistClause(dentistUserId));

        if (status != null) {
            sql.append(" AND e.nombre='").append(status).append("'");
        }

        return sql.toString();
    }

    private String completedCountSql(Long dentistUserId) {
        return """
            SELECT COUNT(*)
            FROM citas c
            JOIN odontologos o ON o.id_odontologo=c.id_odontologo
            JOIN estados_cita e ON e.id_estado_cita=c.id_estado_cita
            WHERE c.id_clinica=? AND c.fecha BETWEEN ? AND ?
            """ + dentistClause(dentistUserId) + """
             AND e.nombre IN ('ATENDIDA','NO_ASISTIO')
            """;
    }

    private String incomeSql(Long dentistUserId, boolean grouped) {
        if (!grouped) {
            return """
                SELECT COALESCE(SUM(p.monto),0)
                FROM pagos p
                JOIN citas c ON c.id_cita=p.id_cita
                JOIN odontologos o ON o.id_odontologo=c.id_odontologo
                WHERE c.id_clinica=?
                  AND DATE(p.fecha_pago) BETWEEN ? AND ?
                """ + dentistClause(dentistUserId);
        }

        return """
            SELECT DATE_FORMAT(ingresos.fecha_dia, '%d/%m') AS etiqueta,
                   COALESCE(SUM(ingresos.monto),0) AS valor
            FROM (
                SELECT DATE(p.fecha_pago) AS fecha_dia, p.monto
                FROM pagos p
                JOIN citas c ON c.id_cita=p.id_cita
                JOIN odontologos o ON o.id_odontologo=c.id_odontologo
                WHERE c.id_clinica=?
                  AND DATE(p.fecha_pago) BETWEEN ? AND ?
            """ + dentistClause(dentistUserId) + """
            ) ingresos
            GROUP BY ingresos.fecha_dia
            ORDER BY ingresos.fecha_dia
            """;
    }

    private String dentistClause(Long dentistUserId) {
        return dentistUserId == null ? "" : " AND o.id_usuario=?\n";
    }

    private Object[] appointmentParams(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) {
        if (dentistUserId == null) {
            return new Object[] {
                clinicId,
                Date.valueOf(from),
                Date.valueOf(to)
            };
        }
        return new Object[] {
            clinicId,
            Date.valueOf(from),
            Date.valueOf(to),
            dentistUserId
        };
    }

    private Object[] incomeParams(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) {
        return appointmentParams(clinicId, from, to, dentistUserId);
    }

    private int scalarInt(Connection cn, String sql, Object... values)
        throws SQLException {
        try (PreparedStatement ps = prepare(cn, sql, values);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private BigDecimal scalarDecimal(
        Connection cn,
        String sql,
        Object... values
    ) throws SQLException {
        try (PreparedStatement ps = prepare(cn, sql, values);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return BigDecimal.ZERO;
            }
            BigDecimal value = rs.getBigDecimal(1);
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    private List<DashboardPoint> points(String sql, Object... values)
        throws SQLException {
        List<DashboardPoint> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepare(cn, sql, values);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new DashboardPoint(
                    rs.getString("etiqueta"),
                    decimal(rs, "valor")
                ));
            }
        }
        return result;
    }

    private List<DashboardCategory> categories(
        String sql,
        Object[] values,
        boolean humanize
    ) throws SQLException {
        List<DashboardCategory> result = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = prepare(cn, sql, values);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String label = rs.getString("etiqueta");
                result.add(new DashboardCategory(
                    humanize ? humanize(label) : label,
                    decimal(rs, "valor")
                ));
            }
        }
        return result;
    }

    private PreparedStatement prepare(
        Connection cn,
        String sql,
        Object... values
    ) throws SQLException {
        PreparedStatement ps = cn.prepareStatement(sql);
        ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
        for (int i = 0; i < values.length; i++) {
            ps.setObject(i + 1, values[i]);
        }
        return ps;
    }

    private BigDecimal decimal(ResultSet rs, String column)
        throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }

    private String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "Sin dato";
        }
        String normalized = value.trim().toLowerCase()
            .replace('_', ' ');
        StringBuilder out = new StringBuilder(normalized.length());
        boolean upper = true;
        for (char ch : normalized.toCharArray()) {
            if (upper && Character.isLetter(ch)) {
                out.append(Character.toUpperCase(ch));
                upper = false;
            } else {
                out.append(ch);
            }
            if (ch == ' ') {
                upper = true;
            }
        }
        return out.toString();
    }
}
