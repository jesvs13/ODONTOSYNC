package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Contrato de generacion de correos institucionales (Issue #33). */
class CorreoInstitucionalServiceTest {
    private static final String DOMAIN = "@odontosync.pe";

    @Test
    void generaInicialDelPrimerNombreYPrimerApellido() throws SQLException {
        var service = new CorreoInstitucionalService(email -> false);
        assertEquals("mmallcco" + DOMAIN,
            service.generate("Manuel Jesus", "Mallcco", "Cisneros"));
    }

    @Test
    void normalizaTildesEnieMayusculasYEspacios() throws SQLException {
        var service = new CorreoInstitucionalService(email -> false);
        assertEquals("mnunez" + DOMAIN,
            service.generate("  María José  ", "  NÚÑEZ  ", "Díaz"));
        assertEquals("jgarcia" + DOMAIN,
            service.generate("José", "García", "López"));
    }

    @Test
    void conservaLetrasDeApellidoCompuestoSinSeparadores() throws SQLException {
        var service = new CorreoInstitucionalService(email -> false);
        assertEquals("adelacruz" + DOMAIN,
            service.generate("Ana", "De la Cruz", "Pérez"));
        assertEquals("mgarcialopez" + DOMAIN,
            service.generate("María", "García-López", "Díaz"));
    }

    @Test
    void usaInicialSegundoApellidoSoloSiCorreoBaseOcupado() throws SQLException {
        Set<String> occupied = Set.of("mmallcco" + DOMAIN);
        var service = new CorreoInstitucionalService(occupied::contains);
        assertEquals("mmallccot" + DOMAIN,
            service.generate("Miguel", "Mallcco", "Torres"));
        assertEquals("mmallccor" + DOMAIN,
            service.generate("Mario", "Mallcco", "Ramos"));
    }

    @Test
    void agregaCorrelativoAlAlternativoOcupado() throws SQLException {
        Set<String> occupied = Set.of("mmallcco" + DOMAIN,
            "mmallccot" + DOMAIN, "mmallccot2" + DOMAIN);
        var service = new CorreoInstitucionalService(occupied::contains);
        assertEquals("mmallccot3" + DOMAIN,
            service.generate("Marcos", "Mallcco", "Torres"));
    }

    @Test
    void permiteSegundoApellidoOpcionalYUsaCorrelativoSiFalta() throws SQLException {
        Set<String> occupied = Set.of("alopez" + DOMAIN, "alopez2" + DOMAIN);
        var service = new CorreoInstitucionalService(occupied::contains);
        assertEquals("alopez3" + DOMAIN, service.generate("Ana", "López", null));
        assertEquals("alopez3" + DOMAIN, service.generate("Ana", "López", "  "));
        assertEquals("aperez" + DOMAIN, service.generate("Ana", "Pérez", null));
    }

    @Test
    void consultaDisponibilidadGlobalIncluyendoOtraClinica() throws SQLException {
        // El contrato de consulta recibe solo el correo, sin filtro por clinica.
        Set<String> clinic1 = Set.of("mmallcco" + DOMAIN);
        Set<String> clinic2 = Set.of("mmallccot" + DOMAIN);
        Set<String> global = new HashSet<>(clinic1);
        global.addAll(clinic2);
        var service = new CorreoInstitucionalService(global::contains);
        assertEquals("mmallccot2" + DOMAIN,
            service.generate("Miguel", "Mallcco", "Torres"));
    }

    @Test
    void rechazaNombresOApellidosSinLetras() {
        var service = new CorreoInstitucionalService(email -> {
            fail("No debe consultar disponibilidad con datos invalidos");
            return false;
        });
        assertThrows(IllegalArgumentException.class,
            () -> service.generate(null, "Mallcco", "Cisneros"));
        assertThrows(IllegalArgumentException.class,
            () -> service.generate("  ", "Mallcco", "Cisneros"));
        assertThrows(IllegalArgumentException.class,
            () -> service.generate("123", "Mallcco", "Cisneros"));
        assertThrows(IllegalArgumentException.class,
            () -> service.generate("Ana", null, "Cisneros"));
        assertThrows(IllegalArgumentException.class,
            () -> service.generate("Ana", "---", "Cisneros"));
    }

    @Test
    void limitaParteLocalA64CaracteresReservandoEspacioParaSufijos() throws SQLException {
        String surname = "a".repeat(80);
        String base = "m" + "a".repeat(63);
        String alternative = "m" + "a".repeat(62) + "t";
        Set<String> occupied = Set.of(base + DOMAIN, alternative + DOMAIN);
        var service = new CorreoInstitucionalService(occupied::contains);
        assertEquals("m" + "a".repeat(61) + "t2" + DOMAIN,
            service.generate("Manuel", surname, "Torres"));
    }

    @Test
    void noDaCorreoPorDisponibleSiFallaConsulta() {
        SQLException failure = new SQLException("Conexion no disponible");
        var service = new CorreoInstitucionalService(email -> { throw failure; });
        assertSame(failure, assertThrows(SQLException.class,
            () -> service.generate("Ana", "Lopez", "Diaz")));
    }
}
