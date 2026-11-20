package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.PacienteOpcion;
import pe.odontosync.model.RecetaResumen;

/** Operaciones funcionales del módulo Recetas. */
public interface RecetaCrudDAO extends RecetaDAO {
    List<RecetaResumen> findByPatient(long clinicId, long dentistUserId, long patientId) throws SQLException;

    List<PacienteOpcion> findPatientsByDentist(long clinicId, long dentistUserId) throws SQLException;

    long create(
        long clinicId,
        long dentistUserId,
        long patientId,
        String medications,
        String indications
    ) throws SQLException;
}
