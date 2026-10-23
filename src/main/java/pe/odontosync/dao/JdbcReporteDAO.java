package pe.odontosync.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.ReporteEstadoResumen;

public final class JdbcReporteDAO implements ReporteDAO {
    private static final String SQL = """
        SELECT e.nombre AS estado, COUNT(*) AS citas
        FROM citas c
        JOIN estados_cita e ON e.id_estado_cita=c.id_estado_cita
        WHERE c.id_clinica=?
        GROUP BY e.nombre
        ORDER BY e.nombre
        LIMIT 50
        """;

    @Override public List<ReporteEstadoResumen> countAppointmentsByStatus(long clinicId) throws SQLException {
        List<ReporteEstadoResumen> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setQueryTimeout(8); ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(new ReporteEstadoResumen(rs.getString("estado"), rs.getString("citas")));
            }
        }
        return items;
    }
}
