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

/** Reglas de negocio de la recuperacion de contrasena sin sesion. */
public final class PasswordResetService {
    public static final String GENERIC_REQUEST_MESSAGE =
        "Si existe una cuenta asociada al correo indicado, recibiras instrucciones para restablecer tu contrasena.";

    private static final Pattern EMAIL = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final PasswordResetDAO dao;
    private final PasswordResetMailer mailer;
    private final PasswordResetTokenGenerator tokenGenerator;
    private final Clock clock;
    private final Duration ttl;

    public PasswordResetService(PasswordResetDAO dao, PasswordResetMailer mailer) {
        this(
            dao,
            mailer,
            new SecurePasswordResetTokenGenerator(),
            Clock.systemUTC(),
            DEFAULT_TTL
        );
    }

    public PasswordResetService(
        PasswordResetDAO dao,
        PasswordResetMailer mailer,
        PasswordResetTokenGenerator tokenGenerator,
        Clock clock,
        Duration ttl
    ) {
        if (dao == null || mailer == null || tokenGenerator == null || clock == null || ttl == null) {
            throw new IllegalArgumentException("La configuracion de recuperacion es obligatoria.");
        }
        if (ttl.isZero() || ttl.isNegative() || ttl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("La expiracion del token no es valida.");
        }
        this.dao = dao;
        this.mailer = mailer;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
        this.ttl = ttl;
    }

    /**
     * Solicita recuperacion. El llamador siempre debe mostrar el mismo mensaje,
     * exista o no la cuenta, para evitar enumeracion de usuarios.
     */
    public void requestReset(String rawEmail, String publicBaseUrl)
        throws SQLException, PasswordResetDeliveryException {
        String email = normalizeEmail(rawEmail);
        if (!validEmail(email)) {
            performDummyHash();
            return;
        }

        List<PasswordResetAccount> accounts = dao.findActiveAccountsByEmail(email);
        if (accounts.isEmpty()) {
            performDummyHash();
            return;
        }

        String baseUrl = normalizePublicBaseUrl(publicBaseUrl);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);

        for (PasswordResetAccount account : accounts) {
            dao.invalidateUnusedTokens(account.userId(), now);

            String token = tokenGenerator.generate();
            if (!validTokenFormat(token)) {
                throw new IllegalStateException("El generador produjo un token no valido.");
            }

            dao.saveToken(
                account.userId(),
                account.clinicId(),
                tokenHash(token),
                expiresAt
            );

            String resetLink = baseUrl + "/restablecer-password?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
            mailer.sendResetLink(account, resetLink);
        }
    }

    public boolean isTokenUsable(String token) throws SQLException {
        if (!validTokenFormat(token)) return false;
        return dao.isTokenUsable(tokenHash(token), clock.instant());
    }

    public PasswordResetResult resetPassword(
        String token,
        char[] newPassword,
        char[] confirmation
    ) throws SQLException {
        if (!validTokenFormat(token)) {
            throw invalidLink();
        }

        PasswordPolicy.validate(newPassword, true);
        if (confirmation == null || !Arrays.equals(newPassword, confirmation)) {
            throw new IllegalArgumentException("La confirmacion no coincide con la nueva contrasena.");
        }

        String newHash = Passwords.hash(newPassword);
        Optional<PasswordResetResult> result = dao.consumeTokenAndUpdatePassword(
            tokenHash(token),
            newHash,
            clock.instant()
        );
        return result.orElseThrow(PasswordResetService::invalidLink);
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

    private void performDummyHash() {
        tokenHash(tokenGenerator.generate());
    }

    private static String normalizeEmail(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean validEmail(String value) {
        return !value.isBlank() && value.length() <= 150 && EMAIL.matcher(value).matches();
    }

    private static boolean validTokenFormat(String token) {
        return token != null && TOKEN.matcher(token).matches();
    }

    private static String normalizePublicBaseUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Configura ODONTOSYNC_PUBLIC_URL.");
        }
        String value = raw.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (!(value.startsWith("https://") || value.startsWith("http://localhost")
            || value.startsWith("http://127.0.0.1"))) {
            throw new IllegalStateException("ODONTOSYNC_PUBLIC_URL debe usar HTTPS fuera de localhost.");
        }
        return value;
    }

    private static IllegalArgumentException invalidLink() {
        return new IllegalArgumentException(
            "El enlace de recuperacion no es valido, ya fue utilizado o ha expirado."
        );
    }
}
