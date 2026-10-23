package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.PagoResumen;

public interface PagoDAO {
    List<PagoResumen> findByClinic(long clinicId) throws SQLException;
}
