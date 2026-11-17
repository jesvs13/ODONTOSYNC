package pe.odontosync.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import pe.odontosync.dao.PasswordResetDAO;
import pe.odontosync.model.PasswordResetResult;

/** Estado inicial intencional: el primer test debe quedar RED. */
public final class PasswordResetService {
    public static final String GENERIC_REQUEST_MESSAGE =
        "Si existe una cuenta asociada al correo indicado, recibiras instrucciones para restablecer tu contrasena.";

    public PasswordResetService(PasswordResetDAO dao, PasswordResetMailer mailer) { }

    public PasswordResetService(
        PasswordResetDAO dao,
        PasswordResetMailer mailer,
        PasswordResetTokenGenerator tokenGenerator,
        Clock clock,
        Duration ttl
    ) { }

    public void requestReset(String rawEmail, String publicBaseUrl)
        throws SQLException, PasswordResetDeliveryException {
        throw new UnsupportedOperationException("RED: solicitud aun no implementada");
    }

    public boolean isTokenUsable(String token) throws SQLException {
        throw new UnsupportedOperationException("RED: validacion aun no implementada");
    }

    public PasswordResetResult resetPassword(
        String token,
        char[] newPassword,
        char[] confirmation
    ) throws SQLException {
        throw new UnsupportedOperationException("RED: restablecimiento aun no implementado");
    }
}
