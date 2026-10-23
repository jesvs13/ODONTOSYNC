package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.LoginAccount;
import pe.odontosync.model.UsuarioResumen;

/** Persistencia de usuarios: autenticación y consulta administrativa. */
public interface UsuarioDAO {
    List<LoginAccount> findLoginAccountsByEmail(String email) throws SQLException;
    void updateLastAccess(long userId) throws SQLException;
    List<UsuarioResumen> findByClinic(long clinicId) throws SQLException;
}
