package pe.odontosync.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SecurityHeadersTest {
    @Test
    void contieneCabecerasDeEndurecimientoEsperadas() {
        Map<String, String> headers = SecurityHeaders.values();

        assertEquals("nosniff", headers.get("X-Content-Type-Options"));
        assertEquals("DENY", headers.get("X-Frame-Options"));
        assertEquals("no-referrer", headers.get("Referrer-Policy"));
        assertEquals("camera=(), microphone=(), geolocation=()",
            headers.get("Permissions-Policy"));
        assertEquals(5, headers.size());
    }

    @Test
    void mapaDeCabecerasEsInmutable() {
        Map<String, String> headers = SecurityHeaders.values();
        assertThrows(UnsupportedOperationException.class,
            () -> headers.put("X-Test", "no-debe-permitirse"));
    }
}
