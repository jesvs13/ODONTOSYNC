package pe.odontosync.dao;

import java.sql.*;
import java.util.Optional;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.ClinicaResumen;

public final class JdbcClinicaDAO implements ClinicaDAO {
    private static final String SQL = """
        SELECT nombre, ruc, direccion, telefono, estado
        FROM clinicas WHERE id_clinica=?
        """;

    @Override public Optional<ClinicaResumen> findById(long clinicId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setQueryTimeout(8); ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new ClinicaResumen(rs.getString("nombre"), rs.getString("ruc"), rs.getString("direccion"), rs.getString("telefono"), rs.getString("estado")));
            }
        }
    }
}
