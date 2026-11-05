package pe.odontosync.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {
    @Test
    void aceptaPasswordValida() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Clave123".toCharArray(), true));
    }

    @Test
    void rechazaPasswordObligatoriaVacia() {
        assertThrows(IllegalArgumentException.class,
            () -> PasswordPolicy.validate(new char[0], true));
    }

    @Test
    void permitePasswordOpcionalVacia() {
        assertDoesNotThrow(() -> PasswordPolicy.validate(new char[0], false));
    }

    @Test
    void rechazaPasswordCorta() {
        assertThrows(IllegalArgumentException.class,
            () -> PasswordPolicy.validate("Abc123".toCharArray(), true));
    }

    @Test
    void rechazaPasswordSinMayuscula() {
        assertThrows(IllegalArgumentException.class,
            () -> PasswordPolicy.validate("clave123".toCharArray(), true));
    }

    @Test
    void rechazaPasswordSinMinuscula() {
        assertThrows(IllegalArgumentException.class,
            () -> PasswordPolicy.validate("CLAVE123".toCharArray(), true));
    }

    @Test
    void rechazaPasswordSinNumero() {
        assertThrows(IllegalArgumentException.class,
            () -> PasswordPolicy.validate("ClaveSegura".toCharArray(), true));
    }
}
