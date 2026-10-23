package pe.odontosync.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pe.odontosync.model.CitaDetalle;
import pe.odontosync.model.CitaEstadoOpcion;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoOpcion;
import pe.odontosync.model.PacienteOpcion;

/** Persistencia completa para Agenda/Citas. */
public interface AgendaCrudDAO extends AgendaDAO {
    List<CitaDetalle> searchDetailed(
        long clinicId, Long dentistUserId, LocalDate from, LocalDate to, String status, String query
    ) throws SQLException;

    Optional<CitaDetalle> findDetailedById(long clinicId, long appointmentId) throws SQLException;
    List<PacienteOpcion> findActivePatients(long clinicId) throws SQLException;
    List<OdontologoOpcion> findActiveDentists(long clinicId) throws SQLException;
    List<EspecialidadOpcion> findActiveSpecialties() throws SQLException;
    Map<Long, List<Long>> findActiveDentistSpecialtyIds(long clinicId) throws SQLException;
    List<CitaEstadoOpcion> findStates() throws SQLException;

    boolean patientIsActive(long clinicId, long patientId) throws SQLException;
    boolean dentistIsActive(long clinicId, long dentistId) throws SQLException;
    boolean dentistBelongsToUser(long clinicId, long dentistId, long dentistUserId) throws SQLException;
    boolean specialtyAssignedToDentist(long dentistId, long specialtyId) throws SQLException;
    Optional<Long> findCoveringSchedule(
        long clinicId, long dentistId, int dayOfWeek, LocalTime start, LocalTime end
    ) throws SQLException;
    boolean isBlocked(long clinicId, long dentistId, LocalDate date, LocalTime start, LocalTime end)
        throws SQLException;
    boolean dentistOverlaps(
        long clinicId, long dentistId, LocalDate date, LocalTime start, LocalTime end, Long excludeAppointmentId
    ) throws SQLException;
    boolean patientOverlaps(
        long clinicId, long patientId, LocalDate date, LocalTime start, LocalTime end, Long excludeAppointmentId
    ) throws SQLException;

    long create(
        long clinicId, long patientId, long dentistId, Long specialtyId, Long scheduleId,
        long stateId, LocalDate date, LocalTime start, LocalTime end,
        String reason, String notes, long createdBy
    ) throws SQLException;

    void update(
        long clinicId, long appointmentId, long patientId, long dentistId, Long specialtyId, Long scheduleId,
        LocalDate date, LocalTime start, LocalTime end, String reason, String notes
    ) throws SQLException;

    void updateStatus(long clinicId, long appointmentId, long stateId) throws SQLException;
}
