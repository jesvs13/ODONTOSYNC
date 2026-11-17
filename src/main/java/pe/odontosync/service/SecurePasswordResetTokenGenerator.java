package pe.odontosync.service;

import java.security.SecureRandom;
import java.util.Base64;

/** Genera 256 bits aleatorios y los codifica en Base64 URL-safe sin padding. */
public final class SecurePasswordResetTokenGenerator implements PasswordResetTokenGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generate() {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
