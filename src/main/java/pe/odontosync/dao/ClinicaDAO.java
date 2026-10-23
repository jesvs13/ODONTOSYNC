package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.Optional;
import pe.odontosync.model.ClinicaResumen;

public interface ClinicaDAO {
    Optional<ClinicaResumen> findById(long clinicId) throws SQLException;
}
