
package pe.odontosync.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pe.odontosync.Card;
import pe.odontosync.User;
import pe.odontosync.dao.DashboardDAO;

/**
 * Prepara los indicadores del Dashboard según el rol del usuario.
 * La obtención de datos y las consultas SQL permanecen en DashboardDAO.
 */
public final class DashboardService {

    private final DashboardDAO dashboardDAO;

    /**
     * Crea el servicio con el DAO encargado de consultar los indicadores.
     *
     * @param dashboardDAO acceso a los datos del Dashboard
     * @throws NullPointerException si el DAO es null
     */
    public DashboardService(DashboardDAO dashboardDAO) {
        this.dashboardDAO = Objects.requireNonNull(
            dashboardDAO, "dashboardDAO no puede ser null"
        );
    }

    /**
     * Carga las tarjetas correspondientes al rol del usuario.
     *
     * @param user usuario autenticado
     * @return lista de indicadores del rol correspondiente
     * @throws NullPointerException si el usuario es null
     * @throws SQLException si ocurre un error al consultar los datos
     */
    public List<Card> load(User user) throws SQLException {
        Objects.requireNonNull(user, "user no puede ser null");

        return switch (user.role()) {
            case ADMINISTRADOR ->
                administrator(dashboardDAO.findAdministratorMetrics(user.clinicId()));
            case RECEPCIONISTA ->
                reception(dashboardDAO.findReceptionMetrics(user.clinicId()));
            case ODONTOLOGO ->
                dentist(dashboardDAO.findDentistMetrics(user.clinicId(), user.id()));
        };
    }

    private List<Card> administrator(Map<String, String> metrics) {
        return List.of(
            new Card("Usuarios activos", value(metrics, "activeUsers"), "usuarios"),
            new Card("Citas hoy", value(metrics, "todayAppointments"), "agenda"),
            new Card("Cobros registrados", money(metrics, "todayPayments"), "pagos"),
            new Card("Insumos bajos", value(metrics, "lowStock"), "inventario")
        );
    }

    private List<Card> reception(Map<String, String> metrics) {
        return List.of(
            new Card("Citas de hoy", value(metrics, "todayAppointments"), "agenda"),
            new Card("Por confirmar", value(metrics, "pendingAppointments"), "agenda"),
            new Card("Atendidas", value(metrics, "attendedAppointments"), "agenda"),
            new Card("Cobros del día", money(metrics, "todayPayments"), "pagos")
        );
    }

    private List<Card> dentist(Map<String, String> metrics) {
        return List.of(
            new Card("Mis citas", value(metrics, "todayAppointments"), "agenda"),
            new Card("En espera", value(metrics, "waitingAppointments"), "agenda"),
            new Card("Atendidas", value(metrics, "attendedAppointments"), "historias"),
            new Card("Recetas emitidas", value(metrics, "todayPrescriptions"), "recetas")
        );
    }

    /**
     * Obtiene un indicador y utiliza cero si el mapa, la clave o su valor
     * no contienen información.
     *
     * @param metrics indicadores devueltos por el DAO
     * @param key clave del indicador
     * @return valor del indicador o "0" si no está disponible
     */
    private String value(Map<String, String> metrics, String key) {
        if (metrics == null) {
            return "0";
        }

        String result = metrics.get(key);
        return result == null ? "0" : result;
    }

    /**
     * Formatea un indicador monetario en soles peruanos.
     *
     * @param metrics indicadores devueltos por el DAO
     * @param key clave del indicador monetario
     * @return importe con el prefijo S/
     */
    private String money(Map<String, String> metrics, String key) {
        return "S/ " + value(metrics, key);
    }
}
