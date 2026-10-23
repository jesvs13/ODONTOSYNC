package pe.odontosync.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.InventarioResumen;

public final class JdbcInventarioDAO implements InventarioDAO {
    private static final String SQL = """
        SELECT i.nombre AS insumo, cat.nombre AS categoria, i.stock_actual AS stock,
               i.stock_minimo AS minimo,
               CASE WHEN i.stock_actual<=i.stock_minimo THEN 'Stock bajo' ELSE 'Normal' END AS alerta
        FROM insumos i
        JOIN categorias_insumos cat ON cat.id_categoria=i.id_categoria
        WHERE i.id_clinica=?
        ORDER BY i.nombre LIMIT 50
        """;

    @Override public List<InventarioResumen> findByClinic(long clinicId) throws SQLException {
        List<InventarioResumen> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setQueryTimeout(8); ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(new InventarioResumen(rs.getString("insumo"), rs.getString("categoria"), rs.getString("stock"), rs.getString("minimo"), rs.getString("alerta")));
            }
        }
        return items;
    }
}
