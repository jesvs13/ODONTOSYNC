package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.OdontologoResumen;

/** Contrato de persistencia para consultas de odontólogos. */
public interface OdontologoDAO {
    List<OdontologoResumen> findByClinic(long clinicId) throws SQLException;
}
