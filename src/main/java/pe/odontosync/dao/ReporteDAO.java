package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.ReporteEstadoResumen;

public interface ReporteDAO {
    List<ReporteEstadoResumen> countAppointmentsByStatus(long clinicId) throws SQLException;
}
