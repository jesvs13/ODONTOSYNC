package pe.odontosync.dao;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.PagoPendiente;

/** Operaciones de escritura del módulo Pagos, separadas del DAO de consulta existente. */
public interface PagoCrudDAO extends PagoDAO {
    List<PagoPendiente> findPendingByClinic(long clinicId) throws SQLException;

    long create(
        long clinicId,
        long appointmentId,
        long userId,
        BigDecimal amount,
        String paymentMethod,
        String observations
    ) throws SQLException;
}
