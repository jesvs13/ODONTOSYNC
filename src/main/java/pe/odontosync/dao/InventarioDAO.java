package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.InventarioResumen;

public interface InventarioDAO {
    List<InventarioResumen> findByClinic(long clinicId) throws SQLException;
}
