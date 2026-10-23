package pe.odontosync.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash y verificación de contraseñas con PBKDF2-HMAC-SHA256 y salt aleatorio. */
public final class Passwords {
    private static final int ITERATIONS = 310_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() { }

    public static String hash(char[] password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        return "pbkdf2_sha256$" + ITERATIONS + "$"
            + Base64.getEncoder().encodeToString(salt) + "$"
            + Base64.getEncoder().encodeToString(derived);
    }

    public static boolean matches(char[] password, String stored) {
        if (stored == null || !stored.startsWith("pbkdf2_sha256$")) return false;
        try {
            String[] parts = stored.split("\\$", -1);
            if (parts.length != 4) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length < 16 || expected.length != 32) return false;
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("PBKDF2 no está disponible", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
