package pe.odontosync.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.HistoriaResumen;

public final class JdbcHistoriaDAO implements HistoriaDAO {
    private static final String SQL = """
        SELECT DISTINCT CONCAT(p.nombres,' ',p.apellidos) AS paciente,
               p.numero_documento AS documento, h.fecha_apertura AS apertura, h.estado
        FROM historias_clinicas h
        JOIN pacientes p ON p.id_paciente = h.id_paciente
        JOIN citas c ON c.id_paciente = p.id_paciente
        JOIN odontologos o ON o.id_odontologo = c.id_odontologo
        WHERE p.id_clinica=? AND c.id_clinica=? AND o.id_clinica=? AND o.id_usuario=?
        ORDER BY paciente LIMIT 50
        """;

    @Override public List<HistoriaResumen> findAssignedToDentist(long clinicId, long dentistUserId) throws SQLException {
        List<HistoriaResumen> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId); ps.setLong(2, clinicId); ps.setLong(3, clinicId); ps.setLong(4, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(new HistoriaResumen(rs.getString("paciente"), rs.getString("documento"), rs.getString("apertura"), rs.getString("estado")));
            }
        }
        return items;
    }
}
