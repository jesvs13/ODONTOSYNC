package pe.odontosync.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import pe.odontosync.Card;
import pe.odontosync.User;
import pe.odontosync.dao.DashboardDAO;

/** Lógica de presentación del panel; el SQL permanece en DashboardDAO. */
public final class DashboardService {
    private final DashboardDAO dashboardDAO;

    public DashboardService(DashboardDAO dashboardDAO) { this.dashboardDAO = dashboardDAO; }

    public List<Card> load(User user) throws SQLException {
        return switch (user.role()) {
            case ADMINISTRADOR -> administrator(dashboardDAO.findAdministratorMetrics(user.clinicId()));
            case RECEPCIONISTA -> reception(dashboardDAO.findReceptionMetrics(user.clinicId()));
            case ODONTOLOGO -> dentist(dashboardDAO.findDentistMetrics(user.clinicId(), user.id()));
        };
    }

    private List<Card> administrator(Map<String,String> m) {
        return List.of(
            new Card("Usuarios activos", value(m,"activeUsers"), "usuarios"),
            new Card("Citas hoy", value(m,"todayAppointments"), "agenda"),
            new Card("Cobros registrados", money(m,"todayPayments"), "pagos"),
            new Card("Insumos bajos", value(m,"lowStock"), "inventario")
        );
    }

    private List<Card> reception(Map<String,String> m) {
        return List.of(
            new Card("Citas de hoy", value(m,"todayAppointments"), "agenda"),
            new Card("Por confirmar", value(m,"pendingAppointments"), "agenda"),
            new Card("Atendidas", value(m,"attendedAppointments"), "agenda"),
            new Card("Cobros del día", money(m,"todayPayments"), "pagos")
        );
    }

    private List<Card> dentist(Map<String,String> m) {
        return List.of(
            new Card("Mis citas", value(m,"todayAppointments"), "agenda"),
            new Card("En espera", value(m,"waitingAppointments"), "agenda"),
            new Card("Atendidas", value(m,"attendedAppointments"), "historias"),
            new Card("Recetas emitidas", value(m,"todayPrescriptions"), "recetas")
        );
    }

    private String value(Map<String,String> m, String key) { return m.getOrDefault(key, "0"); }
    private String money(Map<String,String> m, String key) { return "S/ " + value(m,key); }
}
