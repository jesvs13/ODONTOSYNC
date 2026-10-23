package pe.odontosync.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.PlacaResumen;

public final class JdbcPlacaDAO implements PlacaDAO {
    private static final String SQL = """
        SELECT DISTINCT pr.fecha_subida AS fecha, CONCAT(p.nombres,' ',p.apellidos) AS paciente,
               pr.nombre_archivo AS archivo
        FROM placas_radiograficas pr
        JOIN pacientes p ON p.id_paciente=pr.id_paciente
        JOIN citas c ON c.id_paciente=p.id_paciente
        JOIN odontologos o ON o.id_odontologo=c.id_odontologo
        WHERE p.id_clinica=? AND c.id_clinica=? AND o.id_clinica=? AND o.id_usuario=?
        ORDER BY fecha DESC LIMIT 50
        """;

    @Override public List<PlacaResumen> findAssignedToDentist(long clinicId, long dentistUserId) throws SQLException {
        List<PlacaResumen> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId); ps.setLong(2, clinicId); ps.setLong(3, clinicId); ps.setLong(4, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(new PlacaResumen(rs.getString("fecha"), rs.getString("paciente"), rs.getString("archivo")));
            }
        }
        return items;
    }
}
