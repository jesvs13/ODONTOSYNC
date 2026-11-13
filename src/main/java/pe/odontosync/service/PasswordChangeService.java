package pe.odontosync.service;

import java.sql.SQLException;
import java.util.Arrays;
import pe.odontosync.User;
import pe.odontosync.dao.PasswordChangeDAO;
import pe.odontosync.security.PasswordPolicy;
import pe.odontosync.security.Passwords;

/** Reglas de negocio del cambio de contraseña de la cuenta autenticada. */
public final class PasswordChangeService {
    private final PasswordChangeDAO dao;

    public PasswordChangeService(PasswordChangeDAO dao) {
        this.dao = dao;
    }

    public void change(
        User current,
        char[] currentPassword,
        char[] newPassword,
        char[] confirmation
    ) throws SQLException {
        if (current == null) {
            throw new IllegalArgumentException("La sesión ya no es válida.");
        }
        if (currentPassword == null || currentPassword.length == 0 || currentPassword.length > 1000) {
            throw new IllegalArgumentException("Ingresa tu contraseña actual.");
        }

        PasswordPolicy.validate(newPassword, true);
        if (confirmation == null || !Arrays.equals(newPassword, confirmation)) {
            throw new IllegalArgumentException("La confirmación no coincide con la nueva contraseña.");
        }

        String storedHash = dao.findPasswordHash(current.clinicId(), current.id())
            .orElseThrow(() -> new IllegalArgumentException("La cuenta activa ya no está disponible."));

        if (!Passwords.matches(currentPassword, storedHash)) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }
        if (Passwords.matches(newPassword, storedHash)) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual.");
        }

        dao.updatePasswordHash(current.clinicId(), current.id(), Passwords.hash(newPassword));
    }
}
