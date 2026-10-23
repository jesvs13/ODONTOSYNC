package pe.odontosync.security;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.Locale;

/**
 * Limita intentos repetidos de autenticación utilizando la caché concurrente de Guava.
 * No guarda contraseñas; la clave combina IP y correo normalizado.
 */
public final class LoginAttemptGuard {
    private static final int MAX_FAILURES = 5;
    private static final Duration BLOCK_TIME = Duration.ofMinutes(5);

    private final Cache<String, AttemptState> attempts = CacheBuilder.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(Duration.ofMinutes(30))
        .build();

    public String key(String email, String remoteAddress) {
        String safeIp = Strings.nullToEmpty(remoteAddress).trim();
        String safeEmail = Strings.nullToEmpty(email).trim().toLowerCase(Locale.ROOT);
        Preconditions.checkArgument(safeIp.length() <= 120, "IP no válida");
        Preconditions.checkArgument(safeEmail.length() <= 150, "Correo no válido");
        return safeIp + "|" + safeEmail;
    }

    public boolean isAllowed(String key) {
        AttemptState state = attempts.getIfPresent(key);
        return state == null || System.nanoTime() >= state.blockedUntilNanos();
    }

    public void recordFailure(String key) {
        attempts.asMap().compute(key, (ignored, current) -> {
            int failures = current == null ? 1 : current.failures() + 1;
            long blockedUntil = current == null ? 0L : current.blockedUntilNanos();
            if (failures >= MAX_FAILURES) {
                failures = 0;
                blockedUntil = System.nanoTime() + BLOCK_TIME.toNanos();
            }
            return new AttemptState(failures, blockedUntil);
        });
    }

    public void recordSuccess(String key) {
        attempts.invalidate(key);
    }

    private record AttemptState(int failures, long blockedUntilNanos) { }
}
