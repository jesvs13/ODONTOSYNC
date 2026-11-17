package pe.odontosync.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Limita solicitudes repetidas de recuperacion sin revelar al usuario el bloqueo. */
public final class PasswordResetRequestGuard {
    private static final long COOLDOWN_NANOS = Duration.ofSeconds(60).toNanos();
    private final Cache<String, Long> lastRequest = CacheBuilder.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build();

    public synchronized boolean allow(String email, String remoteAddress) {
        String key = normalize(email) + "|" + normalize(remoteAddress);
        long now = System.nanoTime();
        Long previous = lastRequest.getIfPresent(key);
        if (previous != null && now - previous < COOLDOWN_NANOS) return false;
        lastRequest.put(key, now);
        return true;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
