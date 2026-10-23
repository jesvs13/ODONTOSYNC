package pe.odontosync.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.PagoResumen;

public final class JdbcPagoDAO implements PagoDAO {
    private static final String SQL = """
        SELECT p.fecha_pago AS fecha, CONCAT(pa.nombres,' ',pa.apellidos) AS paciente,
               p.monto, p.metodo_pago AS metodo
        FROM pagos p
        JOIN citas c ON c.id_cita=p.id_cita
        JOIN pacientes pa ON pa.id_paciente=c.id_paciente
        WHERE c.id_clinica=? AND pa.id_clinica=?
        ORDER BY p.fecha_pago DESC LIMIT 50
        """;

    @Override public List<PagoResumen> findByClinic(long clinicId) throws SQLException {
        List<PagoResumen> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setQueryTimeout(8); ps.setLong(1, clinicId); ps.setLong(2, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(new PagoResumen(rs.getString("fecha"), rs.getString("paciente"), rs.getString("monto"), rs.getString("metodo")));
            }
        }
        return items;
    }
}
