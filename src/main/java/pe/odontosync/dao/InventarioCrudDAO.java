package pe.odontosync.dao;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import pe.odontosync.model.InventarioCategoria;
import pe.odontosync.model.InventarioInsumo;
import pe.odontosync.model.InventarioMovimiento;

public interface InventarioCrudDAO extends InventarioDAO {
    List<InventarioInsumo> findDetailedByClinic(long clinicId) throws SQLException;
    Optional<InventarioInsumo> findDetailedById(long clinicId, long itemId) throws SQLException;
    List<InventarioCategoria> findActiveCategories() throws SQLException;
    boolean nameExists(long clinicId, String name, Long excludeId) throws SQLException;

    long create(
        long clinicId,
        long categoryId,
        String name,
        String description,
        String unit,
        BigDecimal minimumStock,
        BigDecimal referencePrice,
        String status
    ) throws SQLException;

    void update(
        long clinicId,
        long itemId,
        long categoryId,
        String name,
        String description,
        String unit,
        BigDecimal minimumStock,
        BigDecimal referencePrice,
        String status
    ) throws SQLException;

    void updateStatus(long clinicId, long itemId, String status) throws SQLException;
    List<InventarioMovimiento> findMovementsByClinic(long clinicId) throws SQLException;

    long createMovement(
        long clinicId,
        long userId,
        long itemId,
        String type,
        BigDecimal quantity,
        String reason
    ) throws SQLException;
}
