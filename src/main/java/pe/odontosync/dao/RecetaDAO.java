package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.RecetaResumen;

public interface RecetaDAO {
    List<RecetaResumen> findByDentist(long clinicId, long dentistUserId) throws SQLException;
}
