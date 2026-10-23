package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.Map;

/** Persistencia de indicadores del panel. Las claves son códigos internos, no textos de UI. */
public interface DashboardDAO {
    Map<String, String> findAdministratorMetrics(long clinicId) throws SQLException;
    Map<String, String> findReceptionMetrics(long clinicId) throws SQLException;
    Map<String, String> findDentistMetrics(long clinicId, long dentistUserId) throws SQLException;
}
