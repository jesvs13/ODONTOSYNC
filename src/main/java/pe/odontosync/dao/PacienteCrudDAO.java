package pe.odontosync.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pe.odontosync.model.PacienteDetalle;
import pe.odontosync.model.PacienteFicha;

/** Persistencia del CRUD y ficha integral de pacientes. */
public interface PacienteCrudDAO {
    List<PacienteDetalle> searchByClinic(long clinicId, String query) throws SQLException;
    List<PacienteDetalle> searchAssignedToDentist(long clinicId, long dentistUserId, String query) throws SQLException;
    Optional<PacienteDetalle> findDetailedById(long clinicId, long patientId) throws SQLException;
    boolean isAssignedToDentist(long clinicId, long dentistUserId, long patientId) throws SQLException;
    boolean documentExists(long clinicId, String documentType, String documentNumber, Long excludePatientId) throws SQLException;

    long create(
        long clinicId, String documentType, String documentNumber, String nombres, String apellidos,
        LocalDate birthDate, String sexo, String direccion, String telefono, String correo,
        String ocupacion, String emergencyContact, String emergencyPhone, String estado
    ) throws SQLException;

    void update(
        long clinicId, long patientId, String documentType, String documentNumber,
        String nombres, String apellidos, LocalDate birthDate, String sexo, String direccion,
        String telefono, String correo, String ocupacion, String emergencyContact,
        String emergencyPhone, String estado
    ) throws SQLException;

    void updateStatus(long clinicId, long patientId, String estado) throws SQLException;
    PacienteFicha loadFicha(long clinicId, long patientId, boolean includeClinical, boolean includePayments) throws SQLException;
}
