package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.PlacaResumen;

public interface PlacaDAO {
    List<PlacaResumen> findAssignedToDentist(long clinicId, long dentistUserId) throws SQLException;
}
