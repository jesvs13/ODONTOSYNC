package pe.odontosync.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.InventarioCrudDAO;
import pe.odontosync.model.InventarioCategoria;
import pe.odontosync.model.InventarioInsumo;
import pe.odontosync.model.InventarioMovimiento;

/** Reglas de negocio del inventario operativo de una clínica. */
public final class InventarioService {
    private static final Set<String> STATUSES = Set.of("ACTIVO", "INACTIVO");
    private static final Set<String> MOVEMENT_TYPES = Set.of("ENTRADA", "SALIDA", "AJUSTE");
    private static final BigDecimal MAX_DECIMAL = new BigDecimal("99999999.99");

    private final InventarioCrudDAO dao;

    public InventarioService(InventarioCrudDAO dao) {
        this.dao = dao;
    }

    public boolean canManage(Role role) {
        return role == Role.ADMINISTRADOR;
    }

    public List<InventarioInsumo> list(User current) throws SQLException {
        requireAccess(current);
        return dao.findDetailedByClinic(current.clinicId());
    }

    public List<InventarioCategoria> categories(User current) throws SQLException {
        requireAccess(current);
        return dao.findActiveCategories();
    }

    public List<InventarioMovimiento> history(User current) throws SQLException {
        requireAccess(current);
        return dao.findMovementsByClinic(current.clinicId());
    }

    public InventarioInsumo detail(User current, long itemId) throws SQLException {
        requireAccess(current);
        if (itemId <= 0) {
            throw new IllegalArgumentException("Selecciona un insumo válido.");
        }
        return dao.findDetailedById(current.clinicId(), itemId)
            .orElseThrow(() -> new IllegalArgumentException(
                "El insumo no existe o no pertenece a tu clínica."
            ));
    }

    public long create(
        User current,
        long categoryId,
        String nameRaw,
        String descriptionRaw,
        String unitRaw,
        String minimumRaw,
        String priceRaw,
        String statusRaw
    ) throws SQLException {
        requireManage(current);
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Selecciona una categoría válida.");
        }

        String name = required(nameRaw, 3, 150, "El nombre del insumo");
        String description = optional(descriptionRaw, 255, "La descripción");
        String unit = required(unitRaw, 1, 30, "La unidad de medida");
        BigDecimal minimum = nonNegativeDecimal(minimumRaw, false, "El stock mínimo");
        BigDecimal price = nonNegativeDecimal(priceRaw, true, "El precio referencial");
        String status = normalizeStatus(statusRaw);

        if (dao.nameExists(current.clinicId(), name, null)) {
            throw new IllegalArgumentException("Ya existe un insumo con ese nombre en la clínica.");
        }

        try {
            return dao.create(
                current.clinicId(), categoryId, name, description, unit,
                minimum, price, status
            );
        } catch (IllegalStateException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    public void update(
        User current,
        long itemId,
        long categoryId,
        String nameRaw,
        String descriptionRaw,
        String unitRaw,
        String minimumRaw,
        String priceRaw,
        String statusRaw
    ) throws SQLException {
        requireManage(current);
        if (itemId <= 0) {
            throw new IllegalArgumentException("Selecciona un insumo válido.");
        }
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Selecciona una categoría válida.");
        }

        detail(current, itemId);
        String name = required(nameRaw, 3, 150, "El nombre del insumo");
        String description = optional(descriptionRaw, 255, "La descripción");
        String unit = required(unitRaw, 1, 30, "La unidad de medida");
        BigDecimal minimum = nonNegativeDecimal(minimumRaw, false, "El stock mínimo");
        BigDecimal price = nonNegativeDecimal(priceRaw, true, "El precio referencial");
        String status = normalizeStatus(statusRaw);

        if (dao.nameExists(current.clinicId(), name, itemId)) {
            throw new IllegalArgumentException("Ya existe un insumo con ese nombre en la clínica.");
        }

        try {
            dao.update(
                current.clinicId(), itemId, categoryId, name, description,
                unit, minimum, price, status
            );
        } catch (IllegalStateException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    public void changeStatus(User current, long itemId, String statusRaw) throws SQLException {
        requireManage(current);
        if (itemId <= 0) {
            throw new IllegalArgumentException("Selecciona un insumo válido.");
        }
        detail(current, itemId);
        dao.updateStatus(current.clinicId(), itemId, normalizeStatus(statusRaw));
    }

    public long registerMovement(
        User current,
        long itemId,
        String typeRaw,
        String quantityRaw,
        String reasonRaw
    ) throws SQLException {
        requireManage(current);
        if (itemId <= 0) {
            throw new IllegalArgumentException("Selecciona un insumo válido.");
        }

        String type = StringUtils.trimToEmpty(typeRaw).toUpperCase(Locale.ROOT);
        if (!MOVEMENT_TYPES.contains(type)) {
            throw new IllegalArgumentException("Selecciona un tipo de movimiento válido.");
        }

        BigDecimal quantity = positiveDecimal(quantityRaw, "La cantidad");
        String reason = required(reasonRaw, 3, 255, "El motivo");

        try {
            return dao.createMovement(
                current.clinicId(), current.id(), itemId, type, quantity, reason
            );
        } catch (IllegalStateException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private void requireAccess(User current) {
        if (current == null || !current.role().allows("inventario")) {
            throw new IllegalArgumentException("No tienes permiso para acceder a Inventario.");
        }
    }

    private void requireManage(User current) {
        requireAccess(current);
        if (!canManage(current.role())) {
            throw new IllegalArgumentException("Tu perfil no puede gestionar Inventario.");
        }
    }

    private String normalizeStatus(String raw) {
        String status = StringUtils.trimToEmpty(raw).toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException("Selecciona un estado válido.");
        }
        return status;
    }

    private String required(String raw, int min, int max, String label) {
        String value = StringUtils.trimToEmpty(raw);
        if (value.length() < min) {
            throw new IllegalArgumentException(label + " debe contener al menos " + min + " caracteres.");
        }
        if (value.length() > max) {
            throw new IllegalArgumentException(label + " no puede superar " + max + " caracteres.");
        }
        return value;
    }

    private String optional(String raw, int max, String label) {
        String value = StringUtils.trimToEmpty(raw);
        if (value.length() > max) {
            throw new IllegalArgumentException(label + " no puede superar " + max + " caracteres.");
        }
        return value;
    }

    private BigDecimal nonNegativeDecimal(String raw, boolean optional, String label) {
        String value = StringUtils.trimToEmpty(raw).replace(',', '.');
        if (value.isBlank() && optional) return null;
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " es obligatorio.");
        }
        try {
            BigDecimal amount = new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.compareTo(BigDecimal.ZERO) < 0 || amount.compareTo(MAX_DECIMAL) > 0) {
                throw new IllegalArgumentException(label + " debe estar entre 0 y 99999999.99.");
            }
            return amount;
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new IllegalArgumentException(label + " debe ser un número válido con máximo dos decimales.", ex);
        }
    }

    private BigDecimal positiveDecimal(String raw, String label) {
        BigDecimal amount = nonNegativeDecimal(raw, false, label);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(label + " debe ser mayor que cero.");
        }
        return amount;
    }
}
