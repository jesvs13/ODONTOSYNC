package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.CitaResumen;

/** Contrato de persistencia para consultas de agenda/citas. */
public interface AgendaDAO {
    List<CitaResumen> findByClinic(long clinicId) throws SQLException;
    List<CitaResumen> findByDentist(long clinicId, long dentistUserId) throws SQLException;
}
