package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.PacienteResumen;

/** Contrato de persistencia para consultas de pacientes. */
public interface PacienteDAO {
    List<PacienteResumen> findByClinic(long clinicId) throws SQLException;
    List<PacienteResumen> findAssignedToDentist(long clinicId, long dentistUserId) throws SQLException;
}
