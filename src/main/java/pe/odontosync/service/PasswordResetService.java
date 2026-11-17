package pe.odontosync.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import pe.odontosync.dao.PasswordResetDAO;
import pe.odontosync.model.PasswordResetAccount;
import pe.odontosync.model.PasswordResetResult;
import pe.odontosync.security.PasswordPolicy;
import pe.odontosync.security.Passwords;

public final class PasswordResetService {
    public static final String GENERIC_REQUEST_MESSAGE =
        "Si existe una cuenta asociada al correo indicado, recibiras instrucciones para restablecer tu contrasena.";

    private static final Pattern EMAIL = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final PasswordResetDAO dao;
    private final PasswordResetMailer mailer;
    private final PasswordResetTokenGenerator tokenGenerator;
    private final Clock clock;
    private final Duration ttl;

    public PasswordResetService(PasswordResetDAO dao, PasswordResetMailer mailer) {
        this(dao, mailer, new SecurePasswordResetTokenGenerator(), Clock.systemUTC(), Duration.ofMinutes(15));
    }

    public PasswordResetService(
        PasswordResetDAO dao,
        PasswordResetMailer mailer,
        PasswordResetTokenGenerator tokenGenerator,
        Clock clock,
        Duration ttl
    ) {
        this.dao = dao;
        this.mailer = mailer;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
        this.ttl = ttl;
    }

    public void requestReset(String rawEmail, String publicBaseUrl)
        throws SQLException, PasswordResetDeliveryException {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        PasswordResetAccount account = dao.findActiveAccountsByEmail(email).get(0);
        String token = tokenGenerator.generate();
        Instant expiresAt = clock.instant().plus(ttl);

        dao.invalidateUnusedTokens(account.userId(), clock.instant());
        dao.saveToken(account.userId(), account.clinicId(), tokenHash(token), expiresAt);
        String baseUrl = stripTrailingSlash(publicBaseUrl);
        String resetLink = baseUrl + "/restablecer-password?token="
            + URLEncoder.encode(token, StandardCharsets.UTF_8);
        mailer.sendResetLink(account, resetLink);
    }

    public boolean isTokenUsable(String token) throws SQLException {
        throw new UnsupportedOperationException("Aun no implementado");
    }

    public PasswordResetResult resetPassword(String token, char[] newPassword, char[] confirmation)
        throws SQLException {
        throw new UnsupportedOperationException("Aun no implementado");
    }

    static String tokenHash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible", ex);
        }
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
