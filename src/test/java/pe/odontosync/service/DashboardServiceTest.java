package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.DashboardDAO;

class DashboardServiceTest {
    @Test void administradorRecibeSusCuatroIndicadores() throws Exception {
        FakeDashboardDAO dao = new FakeDashboardDAO(); DashboardService service = new DashboardService(dao);
        var cards = service.load(new User(1, 7, "Admin", "Clínica", Role.ADMINISTRADOR));
        assertEquals(4, cards.size()); assertEquals("Usuarios activos", cards.get(0).label());
        assertEquals("S/ 250.00", cards.get(2).value()); assertEquals(7, dao.clinic);
    }

    @Test void odontologoConsultaIndicadoresConSuUsuario() throws Exception {
        FakeDashboardDAO dao = new FakeDashboardDAO(); DashboardService service = new DashboardService(dao);
        var cards = service.load(new User(44, 3, "Carlos", "Clínica", Role.ODONTOLOGO));
        assertEquals("Mis citas", cards.get(0).label()); assertEquals(44, dao.user); assertEquals(3, dao.clinic);
    }

    private static final class FakeDashboardDAO implements DashboardDAO {
        long clinic,user;
        public Map<String,String> findAdministratorMetrics(long c){clinic=c;return Map.of("activeUsers","3","todayAppointments","4","todayPayments","250.00","lowStock","1");}
        public Map<String,String> findReceptionMetrics(long c){clinic=c;return Map.of("todayAppointments","4","pendingAppointments","1","attendedAppointments","2","todayPayments","125.00");}
        public Map<String,String> findDentistMetrics(long c,long u){clinic=c;user=u;return Map.of("todayAppointments","2","waitingAppointments","1","attendedAppointments","1","todayPrescriptions","1");}
    }
}
