package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.PlacaDetalle;

/** Toda operación debe comprobar clínica y vinculación con el odontólogo en la BD. */
public interface PlacaCrudDAO {
    boolean patientVisible(long clinicId, long dentistUserId, long patientId) throws SQLException;

    /** Inserta solo si el paciente es visible; si falla, no debe dejar un registro confirmado. */
    long create(long clinicId, long dentistUserId, PlacaDetalle data) throws SQLException;

    List<PlacaDetalle> findByPatient(long clinicId, long dentistUserId, long patientId) throws SQLException;

    PlacaDetalle findById(long clinicId, long dentistUserId, long plateId) throws SQLException;

    /** Actualización atómica, solo registro propio activo y paciente visible. */
    boolean annul(long clinicId, long dentistUserId, long plateId, String reason) throws SQLException;
}
