package pe.odontosync.dao;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import pe.odontosync.config.DatabaseConnection;

/** Consultas agregadas exclusivamente para indicadores del panel. */
public final class JdbcDashboardDAO implements DashboardDAO {
    @Override public Map<String, String> findAdministratorMetrics(long clinicId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("activeUsers", scalar(cn, "SELECT COUNT(*) FROM usuarios WHERE id_clinica=? AND estado='ACTIVO'", clinicId));
            m.put("todayAppointments", scalar(cn, "SELECT COUNT(*) FROM citas WHERE id_clinica=? AND fecha=CURRENT_DATE", clinicId));
            m.put("todayPayments", scalar(cn, "SELECT COALESCE(SUM(p.monto),0) FROM pagos p JOIN citas c ON c.id_cita=p.id_cita WHERE c.id_clinica=? AND DATE(p.fecha_pago)=CURRENT_DATE", clinicId));
            m.put("lowStock", scalar(cn, "SELECT COUNT(*) FROM insumos WHERE id_clinica=? AND estado='ACTIVO' AND stock_actual<=stock_minimo", clinicId));
            return m;
        }
    }

    @Override public Map<String, String> findReceptionMetrics(long clinicId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("todayAppointments", scalar(cn, "SELECT COUNT(*) FROM citas WHERE id_clinica=? AND fecha=CURRENT_DATE", clinicId));
            m.put("pendingAppointments", scalar(cn, "SELECT COUNT(*) FROM citas c JOIN estados_cita e ON e.id_estado_cita=c.id_estado_cita WHERE c.id_clinica=? AND c.fecha=CURRENT_DATE AND e.nombre='PENDIENTE'", clinicId));
            m.put("attendedAppointments", scalar(cn, "SELECT COUNT(*) FROM citas c JOIN estados_cita e ON e.id_estado_cita=c.id_estado_cita WHERE c.id_clinica=? AND c.fecha=CURRENT_DATE AND e.nombre='ATENDIDA'", clinicId));
            m.put("todayPayments", scalar(cn, "SELECT COALESCE(SUM(p.monto),0) FROM pagos p JOIN citas c ON c.id_cita=p.id_cita WHERE c.id_clinica=? AND DATE(p.fecha_pago)=CURRENT_DATE", clinicId));
            return m;
        }
    }

    @Override public Map<String, String> findDentistMetrics(long clinicId, long dentistUserId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("todayAppointments", scalar(cn, "SELECT COUNT(*) FROM citas c JOIN odontologos o ON o.id_odontologo=c.id_odontologo WHERE c.id_clinica=? AND o.id_usuario=? AND c.fecha=CURRENT_DATE", clinicId, dentistUserId));
            m.put("waitingAppointments", scalar(cn, "SELECT COUNT(*) FROM citas c JOIN odontologos o ON o.id_odontologo=c.id_odontologo JOIN estados_cita e ON e.id_estado_cita=c.id_estado_cita WHERE c.id_clinica=? AND o.id_usuario=? AND c.fecha=CURRENT_DATE AND e.nombre IN ('PENDIENTE','CONFIRMADA')", clinicId, dentistUserId));
            m.put("attendedAppointments", scalar(cn, "SELECT COUNT(*) FROM citas c JOIN odontologos o ON o.id_odontologo=c.id_odontologo JOIN estados_cita e ON e.id_estado_cita=c.id_estado_cita WHERE c.id_clinica=? AND o.id_usuario=? AND c.fecha=CURRENT_DATE AND e.nombre='ATENDIDA'", clinicId, dentistUserId));
            m.put("todayPrescriptions", scalar(cn, "SELECT COUNT(*) FROM recetas r JOIN odontologos o ON o.id_odontologo=r.id_odontologo WHERE o.id_clinica=? AND o.id_usuario=? AND DATE(r.fecha_emision)=CURRENT_DATE", clinicId, dentistUserId));
            return m;
        }
    }

    private String scalar(Connection cn, String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(8);
            for (int i=0; i<values.length; i++) ps.setObject(i+1, values[i]);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : "0"; }
        }
    }
}
