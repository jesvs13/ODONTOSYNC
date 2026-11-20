package pe.odontosync.service;

import java.security.SecureRandom;

public final class SecureLoginOtpGenerator implements LoginOtpGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generate() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
