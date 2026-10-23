package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.HistoriaResumen;

public interface HistoriaDAO {
    List<HistoriaResumen> findAssignedToDentist(long clinicId, long dentistUserId) throws SQLException;
}
