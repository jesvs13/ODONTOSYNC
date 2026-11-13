package pe.odontosync.service;

import java.sql.SQLException;
import pe.odontosync.User;
import pe.odontosync.dao.PasswordChangeDAO;

/** Esqueleto TDD: en RED la lógica todavía no está implementada. */
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
        // RED: implementación intencionalmente incompleta.
    }
}
