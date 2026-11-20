package pe.odontosync.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;
import pe.odontosync.User;
import pe.odontosync.model.LoginOtpChallenge;
import pe.odontosync.model.LoginOtpStatus;
import pe.odontosync.model.LoginOtpVerification;

public final class LoginOtpService {
    private static final Pattern CODE = Pattern.compile("^\\d{6}$");
    private static final Pattern EMAIL = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    );
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final Duration DEFAULT_RESEND_WAIT = Duration.ofSeconds(60);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final LoginOtpMailer mailer;
    private final LoginOtpGenerator generator;
    private final Clock clock;
    private final Duration ttl;
    private final Duration resendWait;
    private final int maxAttempts;

    public LoginOtpService(LoginOtpMailer mailer) {
        this(
            mailer,
            new SecureLoginOtpGenerator(),
            Clock.systemUTC(),
            DEFAULT_TTL,
            DEFAULT_RESEND_WAIT,
            DEFAULT_MAX_ATTEMPTS
        );
    }

    public LoginOtpService(
        LoginOtpMailer mailer,
        LoginOtpGenerator generator,
        Clock clock,
        Duration ttl,
        Duration resendWait,
        int maxAttempts
    ) {
        if (mailer == null || generator == null || clock == null
            || ttl == null || resendWait == null) {
            throw new IllegalArgumentException("La configuración OTP es obligatoria.");
        }
        if (ttl.isZero() || ttl.isNegative() || ttl.compareTo(Duration.ofMinutes(15)) > 0) {
            throw new IllegalArgumentException("La vigencia OTP no es válida.");
        }
        if (resendWait.isNegative() || resendWait.compareTo(ttl) >= 0) {
            throw new IllegalArgumentException("La espera de reenvío OTP no es válida.");
        }
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException("El máximo de intentos OTP no es válido.");
        }

        this.mailer = mailer;
        this.generator = generator;
        this.clock = clock;
        this.ttl = ttl;
        this.resendWait = resendWait;
        this.maxAttempts = maxAttempts;
    }

    public LoginOtpChallenge issue(User user, String rawEmail)
        throws LoginOtpDeliveryException {
        if (user == null) {
            throw new IllegalArgumentException("El usuario pendiente es obligatorio.");
        }

        String email = normalizeEmail(rawEmail);
        if (!validEmail(email)) {
            throw new IllegalArgumentException("El correo para verificación no es válido.");
        }

        String code = generator.generate();
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalStateException("El generador produjo un código OTP no válido.");
        }

        Instant now = clock.instant();
        String salt = newSalt();
        String hash = hash(salt, code);

        mailer.sendCode(user, email, code, ttl);

        return new LoginOtpChallenge(
            user,
            email,
            salt,
            hash,
            now,
            now.plus(ttl),
            now.plus(resendWait),
            0
        );
    }

    public LoginOtpVerification verify(LoginOtpChallenge challenge, String rawCode) {
        if (challenge == null) {
            throw new IllegalArgumentException("No existe una verificación pendiente.");
        }
        if (isExpired(challenge)) {
            return new LoginOtpVerification(LoginOtpStatus.EXPIRED, challenge);
        }
        if (isLocked(challenge)) {
            return new LoginOtpVerification(LoginOtpStatus.LOCKED, challenge);
        }

        String code = rawCode == null ? "" : rawCode.trim();
        boolean valid = CODE.matcher(code).matches()
            && MessageDigest.isEqual(
                challenge.codeHash().getBytes(StandardCharsets.US_ASCII),
                hash(challenge.salt(), code).getBytes(StandardCharsets.US_ASCII)
            );

        if (valid) {
            return new LoginOtpVerification(LoginOtpStatus.VERIFIED, challenge);
        }

        LoginOtpChallenge failed =
            challenge.withFailedAttempts(challenge.failedAttempts() + 1);
        LoginOtpStatus status = isLocked(failed)
            ? LoginOtpStatus.LOCKED
            : LoginOtpStatus.INVALID;
        return new LoginOtpVerification(status, failed);
    }

    public LoginOtpChallenge resend(LoginOtpChallenge challenge)
        throws LoginOtpDeliveryException {
        if (challenge == null) {
            throw new IllegalArgumentException("No existe una verificación pendiente.");
        }
        if (isLocked(challenge)) {
            throw new IllegalStateException("La verificación fue bloqueada por intentos fallidos.");
        }
        if (isExpired(challenge)) {
            throw new IllegalStateException("El código venció. Inicia sesión nuevamente.");
        }

        Instant now = clock.instant();
        if (now.isBefore(challenge.resendAvailableAt())) {
            long seconds = Math.max(
                1L,
                Duration.between(now, challenge.resendAvailableAt()).toSeconds()
            );
            throw new IllegalStateException(
                "Espera " + seconds + " segundos antes de reenviar el código."
            );
        }

        return issue(challenge.user(), challenge.email());
    }

    public boolean isExpired(LoginOtpChallenge challenge) {
        return challenge == null || !clock.instant().isBefore(challenge.expiresAt());
    }

    public boolean isLocked(LoginOtpChallenge challenge) {
        return challenge != null && challenge.failedAttempts() >= maxAttempts;
    }

    public int attemptsRemaining(LoginOtpChallenge challenge) {
        if (challenge == null) return 0;
        return Math.max(0, maxAttempts - challenge.failedAttempts());
    }

    private String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String salt, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = (salt + ":" + code).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible.", ex);
        }
    }

    private String normalizeEmail(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private boolean validEmail(String email) {
        return !email.isBlank()
            && email.length() <= 150
            && EMAIL.matcher(email).matches();
    }
}
