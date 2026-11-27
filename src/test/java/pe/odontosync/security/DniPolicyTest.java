package pe.odontosync.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DniPolicyTest {
    @Test
    void aceptaDniDeOchoDigitosConCeroInicial() {
        assertEquals("01234567", DniPolicy.normalize("01234567"));
    }

    @Test
    void eliminaEspaciosExterioresSinModificarLosDigitos() {
        assertEquals("12345678", DniPolicy.normalize(" 12345678 "));
    }

    @Test
    void rechazaDniAusente() {
        assertThrows(IllegalArgumentException.class, () -> DniPolicy.normalize(null));
        assertThrows(IllegalArgumentException.class, () -> DniPolicy.normalize(""));
        assertThrows(IllegalArgumentException.class, () -> DniPolicy.normalize("   "));
    }

    @Test
    void rechazaLongitudIncorrecta() {
        assertThrows(IllegalArgumentException.class, () -> DniPolicy.normalize("1234567"));
        assertThrows(IllegalArgumentException.class, () -> DniPolicy.normalize("123456789"));
    }

    @Test
    void rechazaLetrasSeparadoresYDigitosNoAscii() {
        for (String invalid : new String[]{"1234567A", "1234 567", "1234-567", "１２３４５６７８"}) {
            assertThrows(IllegalArgumentException.class, () -> DniPolicy.normalize(invalid));
        }
    }
}
