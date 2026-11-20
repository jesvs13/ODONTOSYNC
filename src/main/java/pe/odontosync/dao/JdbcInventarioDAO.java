package pe.odontosync.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.InventarioCategoria;
import pe.odontosync.model.InventarioInsumo;
import pe.odontosync.model.InventarioMovimiento;
import pe.odontosync.model.InventarioResumen;

public final class JdbcInventarioDAO implements InventarioCrudDAO {
    private static final int QUERY_TIMEOUT_SECONDS = 8;

    private static final String SUMMARY_SQL = """
        SELECT i.nombre AS insumo, cat.nombre AS categoria, i.stock_actual AS stock,
               i.stock_minimo AS minimo,
               CASE WHEN i.stock_actual<=i.stock_minimo THEN 'Stock bajo' ELSE 'Normal' END AS alerta
        FROM insumos i
        JOIN categorias_insumos cat ON cat.id_categoria=i.id_categoria
        WHERE i.id_clinica=?
        ORDER BY i.nombre LIMIT 50
        """;

    private static final String DETAIL_SELECT = """
        SELECT i.id_insumo, i.id_categoria, cat.nombre AS categoria, i.nombre,
               i.descripcion, i.unidad_medida, i.stock_actual, i.stock_minimo,
               i.precio_referencial, i.estado
        FROM insumos i
        JOIN categorias_insumos cat ON cat.id_categoria=i.id_categoria
        """;

    @Override
    public List<InventarioResumen> findByClinic(long clinicId) throws SQLException {
        List<InventarioResumen> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SUMMARY_SQL)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new InventarioResumen(
                        rs.getString("insumo"), rs.getString("categoria"),
                        rs.getString("stock"), rs.getString("minimo"), rs.getString("alerta")
                    ));
                }
            }
        }
        return items;
    }

    @Override
    public List<InventarioInsumo> findDetailedByClinic(long clinicId) throws SQLException {
        String sql = DETAIL_SELECT + " WHERE i.id_clinica=? ORDER BY i.estado='ACTIVO' DESC, i.nombre LIMIT 200";
        List<InventarioInsumo> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapItem(rs));
            }
        }
        return items;
    }

    @Override
    public Optional<InventarioInsumo> findDetailedById(long clinicId, long itemId) throws SQLException {
        String sql = DETAIL_SELECT + " WHERE i.id_clinica=? AND i.id_insumo=? LIMIT 1";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, clinicId);
            ps.setLong(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapItem(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<InventarioCategoria> findActiveCategories() throws SQLException {
        String sql = "SELECT id_categoria, nombre FROM categorias_insumos WHERE estado='ACTIVA' ORDER BY nombre";
        List<InventarioCategoria> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new InventarioCategoria(rs.getLong("id_categoria"), rs.getString("nombre")));
                }
            }
        }
        return items;
    }

    @Override
    public boolean nameExists(long clinicId, String name, Long excludeId) throws SQLException {
        String sql = """
            SELECT 1 FROM insumos
            WHERE id_clinica=? AND LOWER(TRIM(nombre))=LOWER(TRIM(?))
              AND (? IS NULL OR id_insumo<>?)
            LIMIT 1
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, clinicId);
            ps.setString(2, name);
            if (excludeId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
                ps.setNull(4, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, excludeId);
                ps.setLong(4, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public long create(
        long clinicId, long categoryId, String name, String description, String unit,
        BigDecimal minimumStock, BigDecimal referencePrice, String status
    ) throws SQLException {
        String sql = """
            INSERT INTO insumos
            (id_clinica,id_categoria,nombre,descripcion,unidad_medida,stock_minimo,precio_referencial,estado)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, clinicId);
            ps.setLong(2, categoryId);
            ps.setString(3, name);
            ps.setString(4, emptyToNull(description));
            ps.setString(5, unit);
            ps.setBigDecimal(6, minimumStock);
            setNullableDecimal(ps, 7, referencePrice);
            ps.setString(8, status);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new SQLException("MySQL no devolvió el identificador del insumo.");
        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
            throw new IllegalStateException("Ya existe un insumo con ese nombre o la categoría no es válida.", ex);
        }
    }

    @Override
    public void update(
        long clinicId, long itemId, long categoryId, String name, String description,
        String unit, BigDecimal minimumStock, BigDecimal referencePrice, String status
    ) throws SQLException {
        String sql = """
            UPDATE insumos
            SET id_categoria=?, nombre=?, descripcion=?, unidad_medida=?, stock_minimo=?,
                precio_referencial=?, estado=?
            WHERE id_insumo=? AND id_clinica=?
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, categoryId);
            ps.setString(2, name);
            ps.setString(3, emptyToNull(description));
            ps.setString(4, unit);
            ps.setBigDecimal(5, minimumStock);
            setNullableDecimal(ps, 6, referencePrice);
            ps.setString(7, status);
            ps.setLong(8, itemId);
            ps.setLong(9, clinicId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("El insumo no existe o no pertenece a tu clínica.");
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
            throw new IllegalStateException("Ya existe un insumo con ese nombre o la categoría no es válida.", ex);
        }
    }

    @Override
    public void updateStatus(long clinicId, long itemId, String status) throws SQLException {
        String sql = "UPDATE insumos SET estado=? WHERE id_insumo=? AND id_clinica=?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setString(1, status);
            ps.setLong(2, itemId);
            ps.setLong(3, clinicId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("El insumo no existe o no pertenece a tu clínica.");
            }
        }
    }

    @Override
    public List<InventarioMovimiento> findMovementsByClinic(long clinicId) throws SQLException {
        String sql = """
            SELECT m.id_movimiento, m.id_insumo, i.nombre AS insumo, m.tipo, m.cantidad,
                   m.motivo, DATE_FORMAT(m.fecha_movimiento,'%Y-%m-%d %H:%i') AS fecha,
                   CONCAT(u.nombres,' ',u.apellidos) AS usuario
            FROM movimientos_inventario m
            JOIN insumos i ON i.id_insumo=m.id_insumo
            JOIN usuarios u ON u.id_usuario=m.id_usuario
            WHERE i.id_clinica=?
            ORDER BY m.fecha_movimiento DESC, m.id_movimiento DESC
            LIMIT 100
            """;
        List<InventarioMovimiento> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new InventarioMovimiento(
                        rs.getLong("id_movimiento"), rs.getLong("id_insumo"),
                        rs.getString("insumo"), rs.getString("tipo"), rs.getBigDecimal("cantidad"),
                        rs.getString("motivo"), rs.getString("fecha"), rs.getString("usuario")
                    ));
                }
            }
        }
        return items;
    }

    @Override
    public long createMovement(
        long clinicId, long userId, long itemId, String type,
        BigDecimal quantity, String reason
    ) throws SQLException {
        String itemSql = """
            SELECT stock_actual, estado FROM insumos
            WHERE id_insumo=? AND id_clinica=?
            FOR UPDATE
            """;
        String userSql = """
            SELECT 1 FROM usuarios
            WHERE id_usuario=? AND id_clinica=? AND estado='ACTIVO'
            LIMIT 1
            """;
        String insertSql = """
            INSERT INTO movimientos_inventario(id_insumo,id_usuario,tipo,cantidad,motivo)
            VALUES (?,?,?,?,?)
            """;

        try (Connection cn = DatabaseConnection.getConnection()) {
            boolean previousAutoCommit = cn.getAutoCommit();
            cn.setAutoCommit(false);
            try {
                BigDecimal stock;
                String status;
                try (PreparedStatement ps = cn.prepareStatement(itemSql)) {
                    ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    ps.setLong(1, itemId);
                    ps.setLong(2, clinicId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("El insumo no existe o no pertenece a tu clínica.");
                        }
                        stock = rs.getBigDecimal("stock_actual");
                        status = rs.getString("estado");
                    }
                }

                if (!"ACTIVO".equals(status)) {
                    throw new IllegalStateException("No se pueden registrar movimientos en un insumo inactivo.");
                }
                if ("SALIDA".equals(type) && stock.compareTo(quantity) < 0) {
                    throw new IllegalStateException("Stock insuficiente para realizar la salida.");
                }

                try (PreparedStatement ps = cn.prepareStatement(userSql)) {
                    ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    ps.setLong(1, userId);
                    ps.setLong(2, clinicId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("El usuario no pertenece a la clínica activa.");
                        }
                    }
                }

                long movementId;
                try (PreparedStatement ps = cn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    ps.setLong(1, itemId);
                    ps.setLong(2, userId);
                    ps.setString(3, type);
                    ps.setBigDecimal(4, quantity);
                    ps.setString(5, reason);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("MySQL no devolvió el identificador del movimiento.");
                        }
                        movementId = keys.getLong(1);
                    }
                }

                cn.commit();
                cn.setAutoCommit(previousAutoCommit);
                return movementId;
            } catch (SQLException | RuntimeException ex) {
                try { cn.rollback(); } catch (SQLException rollbackEx) { ex.addSuppressed(rollbackEx); }
                try { cn.setAutoCommit(previousAutoCommit); } catch (SQLException restoreEx) { ex.addSuppressed(restoreEx); }
                throw ex;
            }
        }
    }

    private InventarioInsumo mapItem(ResultSet rs) throws SQLException {
        return new InventarioInsumo(
            rs.getLong("id_insumo"), rs.getLong("id_categoria"), rs.getString("categoria"),
            rs.getString("nombre"), rs.getString("descripcion"), rs.getString("unidad_medida"),
            rs.getBigDecimal("stock_actual"), rs.getBigDecimal("stock_minimo"),
            rs.getBigDecimal("precio_referencial"), rs.getString("estado")
        );
    }

    private void setNullableDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.DECIMAL);
        else ps.setBigDecimal(index, value);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
