package pe.odontosync.service;

import java.sql.SQLException;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.UsuarioDAO;
import pe.odontosync.model.LoginAccount;
import pe.odontosync.security.Passwords;

/** Lógica de negocio de autenticación, separada del Servlet y del SQL. */
public final class AuthService {
    private final UsuarioDAO usuarioDAO;

    public AuthService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public User authenticate(String email, char[] password) throws SQLException {
        User match = null;
        for (LoginAccount account : usuarioDAO.findLoginAccountsByEmail(email)) {
            Role role = Role.fromDatabase(account.roleName());
            boolean valid = "ACTIVO".equals(account.userStatus())
                && "ACTIVA".equals(account.clinicStatus())
                && role != null
                && Passwords.matches(password, account.passwordHash());

            if (valid) {
                // Si un mismo correo/clave coincide en dos clínicas, el login es ambiguo y se rechaza.
                if (match != null) return null;
                match = new User(
                    account.userId(),
                    account.clinicId(),
                    account.firstName() + " " + account.lastName(),
                    account.clinicName(),
                    role
                );
            }
        }

        if (match != null) usuarioDAO.updateLastAccess(match.id());
        return match;
    }
}
