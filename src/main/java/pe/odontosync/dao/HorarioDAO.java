package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.HorarioResumen;

/** Contrato de persistencia para consultas de horarios. */
public interface HorarioDAO {
    List<HorarioResumen> findByClinic(long clinicId) throws SQLException;
    List<HorarioResumen> findByDentist(long clinicId, long dentistUserId) throws SQLException;
}
