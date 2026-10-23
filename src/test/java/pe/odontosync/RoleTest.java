package pe.odontosync;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RoleTest {
    @Test
    void permisosPrincipalesPorRol() {
        assertTrue(Role.ADMINISTRADOR.allows("usuarios"));
        assertFalse(Role.ODONTOLOGO.allows("usuarios"));
        assertTrue(Role.RECEPCIONISTA.allows("pagos"));
        assertFalse(Role.ODONTOLOGO.allows("pagos"));
        assertTrue(Role.ODONTOLOGO.allows("historias"));
        assertFalse(Role.RECEPCIONISTA.allows("historias"));
    }
}
