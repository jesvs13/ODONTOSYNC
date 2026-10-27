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
import pe.odontosync.dao.PagoCrudDAO;
import pe.odontosync.model.PagoPendiente;
import pe.odontosync.model.PagoResumen;

/** Reglas de negocio para consulta y registro de pagos. */
public final class PagoService {
    private static final Set<String> PAYMENT_METHODS = Set.of(
        "EFECTIVO", "YAPE", "PLIN", "TRANSFERENCIA", "TARJETA"
    );
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    private final PagoCrudDAO dao;

    public PagoService(PagoCrudDAO dao) {
        this.dao = dao;
    }

    public boolean canRegister(Role role) {
        return role == Role.ADMINISTRADOR || role == Role.RECEPCIONISTA;
    }

    public List<PagoResumen> history(User current) throws SQLException {
        requireAccess(current);
        return dao.findByClinic(current.clinicId());
    }

    public List<PagoPendiente> pending(User current) throws SQLException {
        requireAccess(current);
        return dao.findPendingByClinic(current.clinicId());
    }

    public long register(
        User current,
        long appointmentId,
        String amountRaw,
        String methodRaw,
        String observationsRaw
    ) throws SQLException {
        requireAccess(current);
        if (!canRegister(current.role())) {
            throw new IllegalArgumentException("Tu perfil no puede registrar pagos.");
        }
        if (appointmentId <= 0) {
            throw new IllegalArgumentException("Selecciona una cita atendida válida.");
        }

        BigDecimal amount = parseAmount(amountRaw);
        String method = normalizeMethod(methodRaw);
        String observations = StringUtils.trimToEmpty(observationsRaw);
        if (observations.length() > 1000) {
            throw new IllegalArgumentException("Las observaciones no pueden superar 1000 caracteres.");
        }

        try {
            return dao.create(
                current.clinicId(), appointmentId, current.id(), amount, method, observations
            );
        } catch (IllegalStateException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private void requireAccess(User current) {
        if (current == null || !current.role().allows("pagos")) {
            throw new IllegalArgumentException("No tienes permiso para acceder a Pagos.");
        }
    }

    private BigDecimal parseAmount(String raw) {
        String value = StringUtils.trimToEmpty(raw).replace(',', '.');
        if (value.isBlank()) {
            throw new IllegalArgumentException("Ingresa el monto del pago.");
        }
        try {
            BigDecimal amount = new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El monto debe ser mayor que cero.");
            }
            if (amount.compareTo(MAX_AMOUNT) > 0) {
                throw new IllegalArgumentException("El monto supera el máximo permitido.");
            }
            return amount;
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new IllegalArgumentException("Ingresa un monto válido con máximo dos decimales.", ex);
        }
    }

    private String normalizeMethod(String raw) {
        String method = StringUtils.trimToEmpty(raw).toUpperCase(Locale.ROOT);
        if (!PAYMENT_METHODS.contains(method)) {
            throw new IllegalArgumentException("Selecciona un método de pago válido.");
        }
        return method;
    }
}
