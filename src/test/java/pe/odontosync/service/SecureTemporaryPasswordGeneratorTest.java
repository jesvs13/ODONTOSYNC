package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import pe.odontosync.security.PasswordPolicy;

class SecureTemporaryPasswordGeneratorTest {
    private final SecureTemporaryPasswordGenerator generator = new SecureTemporaryPasswordGenerator();

    @Test
    void generaDoceCaracteresImprimiblesSinEspacios() {
        char[] password = generator.generate();
        try {
            assertEquals(12, password.length);
            for (char c : password) {
                assertTrue(c >= 33 && c <= 126);
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @Test
    void cadaPasswordCumpleLaPoliticaExistente() {
        for (int i = 0; i < 20; i++) {
            char[] password = generator.generate();
            try {
                assertDoesNotThrow(() -> PasswordPolicy.validate(password, true));
            } finally {
                Arrays.fill(password, '\0');
            }
        }
    }

    @Test
    void dosGeneracionesNoReutilizanLaMismaPassword() {
        char[] first = generator.generate();
        char[] second = generator.generate();
        try {
            assertFalse(Arrays.equals(first, second));
        } finally {
            Arrays.fill(first, '\0');
            Arrays.fill(second, '\0');
        }
    }
}
