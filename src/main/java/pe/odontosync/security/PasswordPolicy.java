package pe.odontosync.security;

/**
 * Política única de contraseñas de OdontoSync.
 * Centraliza una regla que antes estaba duplicada en varios Services.
 */
public final class PasswordPolicy {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 72;

    private PasswordPolicy() { }

    public static void validate(char[] password, boolean required) {
        if (password == null || password.length == 0) {
            if (required) {
                throw new IllegalArgumentException("La contraseña es obligatoria.");
            }
            return;
        }

        if (password.length < MIN_LENGTH || password.length > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "La contraseña debe tener entre 8 y 72 caracteres."
            );
        }

        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        for (char c : password) {
            upper |= Character.isUpperCase(c);
            lower |= Character.isLowerCase(c);
            digit |= Character.isDigit(c);
        }

        if (!upper || !lower || !digit) {
            throw new IllegalArgumentException(
                "La contraseña debe incluir mayúscula, minúscula y número."
            );
        }
    }
}
