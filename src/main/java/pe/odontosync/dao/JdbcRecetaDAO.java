package pe.odontosync.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.RecetaResumen;

public final class JdbcRecetaDAO implements RecetaDAO {
    private static final String SQL = """
        SELECT r.fecha_emision AS fecha, CONCAT(p.nombres,' ',p.apellidos) AS paciente,
               r.medicamentos, r.indicaciones
        FROM recetas r
        JOIN pacientes p ON p.id_paciente=r.id_paciente
        JOIN odontologos o ON o.id_odontologo=r.id_odontologo
        WHERE p.id_clinica=? AND o.id_clinica=? AND o.id_usuario=?
        ORDER BY r.fecha_emision DESC LIMIT 50
        """;

    @Override public List<RecetaResumen> findByDentist(long clinicId, long dentistUserId) throws SQLException {
        List<RecetaResumen> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId); ps.setLong(2, clinicId); ps.setLong(3, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(new RecetaResumen(rs.getString("fecha"), rs.getString("paciente"), rs.getString("medicamentos"), rs.getString("indicaciones")));
            }
        }
        return items;
    }
}
