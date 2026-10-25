package pe.odontosync.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.PagoPendiente;
import pe.odontosync.model.PagoResumen;

/** Acceso JDBC a pagos. Conserva la consulta existente y añade registro controlado. */
public final class JdbcPagoDAO implements PagoCrudDAO {
    private static final String FIND_BY_CLINIC = """
        SELECT p.fecha_pago AS fecha, CONCAT(pa.nombres,' ',pa.apellidos) AS paciente,
               p.monto, p.metodo_pago AS metodo
        FROM pagos p
        JOIN citas c ON c.id_cita=p.id_cita
        JOIN pacientes pa ON pa.id_paciente=c.id_paciente
        WHERE c.id_clinica=? AND pa.id_clinica=?
        ORDER BY p.fecha_pago DESC LIMIT 50
        """;

    private static final String FIND_PENDING = """
        SELECT c.id_cita,
               DATE_FORMAT(c.fecha,'%Y-%m-%d') AS fecha,
               TIME_FORMAT(c.hora_inicio,'%H:%i') AS hora,
               CONCAT(pa.nombres,' ',pa.apellidos) AS paciente,
               pa.numero_documento AS documento,
               CONCAT(o.nombres,' ',o.apellidos) AS odontologo
        FROM citas c
        JOIN estados_cita ec ON ec.id_estado_cita=c.id_estado_cita
        JOIN pacientes pa ON pa.id_paciente=c.id_paciente
        JOIN odontologos o ON o.id_odontologo=c.id_odontologo
        WHERE c.id_clinica=?
          AND pa.id_clinica=?
          AND o.id_clinica=?
          AND ec.nombre='ATENDIDA'
          AND NOT EXISTS (SELECT 1 FROM pagos p WHERE p.id_cita=c.id_cita)
        ORDER BY c.fecha DESC, c.hora_inicio DESC
        LIMIT 100
        """;

    private static final String LOCK_APPOINTMENT = """
        SELECT id_estado_cita
        FROM citas
        WHERE id_cita=? AND id_clinica=?
        FOR UPDATE
        """;

    private static final String FIND_STATUS = """
        SELECT nombre FROM estados_cita WHERE id_estado_cita=?
        """;

    private static final String PAYMENT_EXISTS = """
        SELECT 1 FROM pagos WHERE id_cita=? LIMIT 1
        """;

    private static final String INSERT_PAYMENT = """
        INSERT INTO pagos (id_cita,id_usuario,monto,metodo_pago,url_comprobante,observaciones)
        VALUES (?,?,?,?,NULL,?)
        """;

    @Override
    public List<PagoResumen> findByClinic(long clinicId) throws SQLException {
        List<PagoResumen> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_CLINIC)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setLong(2, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new PagoResumen(
                        rs.getString("fecha"),
                        rs.getString("paciente"),
                        rs.getString("monto"),
                        rs.getString("metodo")
                    ));
                }
            }
        }
        return items;
    }

    @Override
    public List<PagoPendiente> findPendingByClinic(long clinicId) throws SQLException {
        List<PagoPendiente> items = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_PENDING)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setLong(2, clinicId);
            ps.setLong(3, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new PagoPendiente(
                        rs.getLong("id_cita"),
                        rs.getString("fecha"),
                        rs.getString("hora"),
                        rs.getString("paciente"),
                        rs.getString("documento"),
                        rs.getString("odontologo")
                    ));
                }
            }
        }
        return items;
    }

    @Override
    public long create(
        long clinicId,
        long appointmentId,
        long userId,
        BigDecimal amount,
        String paymentMethod,
        String observations
    ) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                long statusId = lockAppointmentAndGetStatus(cn, clinicId, appointmentId);
                String status = findStatus(cn, statusId);
                if (!"ATENDIDA".equals(status)) {
                    throw new IllegalStateException("Solo se puede registrar el pago de una cita ATENDIDA.");
                }
                if (paymentExists(cn, appointmentId)) {
                    throw new IllegalStateException("La cita seleccionada ya tiene un pago registrado.");
                }

                long paymentId = insertPayment(
                    cn, appointmentId, userId, amount, paymentMethod, observations
                );
                cn.commit();
                return paymentId;
            } catch (SQLException | RuntimeException ex) {
                try {
                    cn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                throw ex;
            }
        }
    }

    private long lockAppointmentAndGetStatus(Connection cn, long clinicId, long appointmentId)
        throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(LOCK_APPOINTMENT)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, appointmentId);
            ps.setLong(2, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("La cita seleccionada no existe en tu clínica.");
                }
                return rs.getLong("id_estado_cita");
            }
        }
    }

    private String findStatus(Connection cn, long statusId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(FIND_STATUS)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, statusId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("No se pudo determinar el estado de la cita.");
                }
                return rs.getString("nombre");
            }
        }
    }

    private boolean paymentExists(Connection cn, long appointmentId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(PAYMENT_EXISTS)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private long insertPayment(
        Connection cn,
        long appointmentId,
        long userId,
        BigDecimal amount,
        String paymentMethod,
        String observations
    ) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(INSERT_PAYMENT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, appointmentId);
            ps.setLong(2, userId);
            ps.setBigDecimal(3, amount);
            ps.setString(4, paymentMethod);
            ps.setString(5, observations.isBlank() ? null : observations);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL no devolvió el identificador del pago registrado.");
                }
                return keys.getLong(1);
            }
        }
    }
}
