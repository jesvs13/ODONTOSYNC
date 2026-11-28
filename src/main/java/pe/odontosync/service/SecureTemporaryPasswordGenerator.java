package pe.odontosync.service;

import java.security.SecureRandom;
import java.util.Arrays;
import pe.odontosync.security.PasswordPolicy;

/** Genera credenciales temporales independientes de los datos personales. */
public final class SecureTemporaryPasswordGenerator {
    private static final int LENGTH = 12;
    private static final String ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%+-_";
    private final SecureRandom random = new SecureRandom();

    /** El llamador debe limpiar el arreglo cuando deje de necesitarlo. */
    public char[] generate() {
        while (true) {
            char[] password = new char[LENGTH];
            for (int i = 0; i < password.length; i++) {
                password[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
            }
            try {
                PasswordPolicy.validate(password, true);
                return password;
            } catch (IllegalArgumentException invalid) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
