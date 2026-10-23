package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import pe.odontosync.model.AtencionCitaDetalle;
import pe.odontosync.model.HistoriaPacienteResumen;
import pe.odontosync.model.HistoriaPacienteVista;

/** Persistencia de atención clínica e historia longitudinal. */
public interface ClinicaAtencionDAO {
    Optional<AtencionCitaDetalle> findAppointmentForDentist(long clinicId, long dentistUserId, long appointmentId)
        throws SQLException;

    long saveAttention(
        long clinicId, long dentistUserId, long appointmentId,
        String motivoConsulta, String diagnostico, String tratamiento,
        String observaciones, String recomendaciones
    ) throws SQLException;

    List<HistoriaPacienteResumen> searchHistories(long clinicId, long dentistUserId, String query)
        throws SQLException;

    Optional<HistoriaPacienteVista> loadHistory(long clinicId, long dentistUserId, long patientId)
        throws SQLException;

    void updateGeneralNotes(long clinicId, long dentistUserId, long patientId, String notes)
        throws SQLException;

    long addMedicalHistory(
        long clinicId, long dentistUserId, long patientId,
        String type, String description, String observations
    ) throws SQLException;

    long addAllergy(
        long clinicId, long dentistUserId, long patientId,
        String allergen, String reaction, String observations
    ) throws SQLException;
}
