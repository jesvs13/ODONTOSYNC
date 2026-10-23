package pe.odontosync.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoDetalle;

/** CRUD de perfiles profesionales y su cuenta de acceso vinculada. */
public interface OdontologoCrudDAO {
    List<OdontologoDetalle> searchByClinic(long clinicId, String query) throws SQLException;
    Optional<OdontologoDetalle> findDetailedById(long clinicId, long dentistId) throws SQLException;
    List<EspecialidadOpcion> findActiveSpecialties() throws SQLException;
    boolean emailExists(long clinicId, String email, Long excludeUserId) throws SQLException;
    boolean licenseExists(long clinicId, String license, Long excludeDentistId) throws SQLException;

    long create(
        long clinicId, String nombres, String apellidos, String correo, String telefono,
        String passwordHash, String colegiatura, LocalDate fechaIngreso, String estado,
        List<Long> especialidadIds
    ) throws SQLException;

    void update(
        long clinicId, long dentistId, String nombres, String apellidos, String correo,
        String telefono, String passwordHashOrNull, String colegiatura,
        LocalDate fechaIngreso, String estado, List<Long> especialidadIds
    ) throws SQLException;

    void updateStatus(long clinicId, long dentistId, String estado) throws SQLException;
}
