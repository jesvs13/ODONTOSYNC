package pe.odontosync.dao;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import pe.odontosync.model.HorarioDetalle;
import pe.odontosync.model.OdontologoOpcion;

/** CRUD de disponibilidad semanal de odontólogos. */
public interface HorarioCrudDAO {
    List<HorarioDetalle> findDetailedByClinic(long clinicId) throws SQLException;
    List<HorarioDetalle> findDetailedByDentistUser(long clinicId, long userId) throws SQLException;
    Optional<HorarioDetalle> findDetailedById(long clinicId, long scheduleId) throws SQLException;
    List<OdontologoOpcion> findDentistsByClinic(long clinicId) throws SQLException;
    boolean dentistBelongsToClinic(long clinicId, long dentistId) throws SQLException;
    boolean dentistIsActive(long clinicId, long dentistId) throws SQLException;
    boolean overlaps(
        long clinicId, long dentistId, int day, LocalTime start, LocalTime end,
        Long excludeScheduleId
    ) throws SQLException;

    long create(long clinicId, long dentistId, int day, LocalTime start, LocalTime end, String estado)
        throws SQLException;
    void update(long clinicId, long scheduleId, long dentistId, int day, LocalTime start, LocalTime end, String estado)
        throws SQLException;
    void updateStatus(long clinicId, long scheduleId, String estado) throws SQLException;
}
