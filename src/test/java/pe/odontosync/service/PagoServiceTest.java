package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.PagoCrudDAO;
import pe.odontosync.model.PagoPendiente;
import pe.odontosync.model.PagoResumen;

class PagoServiceTest {
    @Test
    void recepcionRegistraPagoValido() throws Exception {
        FakePagoDAO dao = new FakePagoDAO();
        PagoService service = new PagoService(dao);

        long id = service.register(reception(), 21, "150.50", "yape", "Pago completo");

        assertEquals(900, id);
        assertEquals(5, dao.clinicId);
        assertEquals(21, dao.appointmentId);
        assertEquals(2, dao.userId);
        assertEquals(new BigDecimal("150.50"), dao.amount);
        assertEquals("YAPE", dao.method);
    }

    @Test
    void rechazaMontoCero() {
        PagoService service = new PagoService(new FakePagoDAO());
        assertThrows(IllegalArgumentException.class,
            () -> service.register(reception(), 21, "0", "EFECTIVO", ""));
    }

    @Test
    void rechazaMetodoDesconocido() {
        PagoService service = new PagoService(new FakePagoDAO());
        assertThrows(IllegalArgumentException.class,
            () -> service.register(reception(), 21, "20.00", "CRIPTO", ""));
    }

    @Test
    void odontologoNoPuedeRegistrarPago() {
        PagoService service = new PagoService(new FakePagoDAO());
        assertThrows(IllegalArgumentException.class,
            () -> service.register(dentist(), 21, "20.00", "EFECTIVO", ""));
    }

    private static User reception() {
        return new User(2, 5, "Recepción", "Clínica", Role.RECEPCIONISTA);
    }

    private static User dentist() {
        return new User(44, 5, "Carlos", "Clínica", Role.ODONTOLOGO);
    }

    private static final class FakePagoDAO implements PagoCrudDAO {
        long clinicId;
        long appointmentId;
        long userId;
        BigDecimal amount;
        String method;

        @Override
        public List<PagoResumen> findByClinic(long clinicId) {
            return List.of();
        }

        @Override
        public List<PagoPendiente> findPendingByClinic(long clinicId) {
            return List.of();
        }

        @Override
        public long create(
            long clinicId,
            long appointmentId,
            long userId,
            BigDecimal amount,
            String paymentMethod,
            String observations
        ) {
            this.clinicId = clinicId;
            this.appointmentId = appointmentId;
            this.userId = userId;
            this.amount = amount;
            this.method = paymentMethod;
            return 900;
        }
    }
}
