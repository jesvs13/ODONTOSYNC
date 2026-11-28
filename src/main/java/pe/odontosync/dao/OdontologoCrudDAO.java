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
    /** Activas visibles en la clínica y, al editar, inactivas ya asignadas al profesional de esa clínica. */
    List<EspecialidadOpcion> findAvailableSpecialties(long clinicId, Long dentistId) throws SQLException;
    boolean emailExists(long clinicId, String email, Long excludeUserId) throws SQLException;
    boolean emailExistsGlobally(String email) throws SQLException;
    boolean licenseExists(long clinicId, String license, Long excludeDentistId) throws SQLException;

    long create(
        long clinicId, String nombres, String apellidos, String correo, String telefono,
        String passwordHash, String colegiatura, LocalDate fechaIngreso, String estado,
        List<Long> especialidadIds, String dni
    ) throws SQLException;

    void update(
        long clinicId, long dentistId, String nombres, String apellidos, String correo,
        String telefono, String passwordHashOrNull, String colegiatura,
        LocalDate fechaIngreso, String estado, List<Long> especialidadIds
    ) throws SQLException;

    void updateStatus(long clinicId, long dentistId, String estado) throws SQLException;
}
