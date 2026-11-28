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

    /**
     * Compatibilidad con el flujo anterior: autentica y registra el acceso.
     * El login web con 2FA usa verifyFirstFactor + completeLogin.
     */
    public User authenticate(String email, char[] password) throws SQLException {
        User user = verifyFirstFactor(email, password);
        if (user != null) completeLogin(user);
        return user;
    }

    /** Valida correo/contraseña sin considerar la sesión todavía autenticada. */
    public User verifyFirstFactor(String email, char[] password) throws SQLException {
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
                    role, account.mustChangePassword()
                );
            }
        }
        return match;
    }

    /** Registra el acceso únicamente después de completar el segundo factor. */
    public void completeLogin(User user) throws SQLException {
        if (user == null || user.id() <= 0) {
            throw new IllegalArgumentException("El usuario autenticado es obligatorio.");
        }
        usuarioDAO.updateLastAccess(user.id());
    }
}
