package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import pe.odontosync.model.ClinicaResumen;
import pe.odontosync.model.RecetaResumen;

class RecetaPdfServiceTest {

    @Test
    void generaPdfValidoConIdentidadDeLaClinica() {
        RecetaResumen receta = receta();
        ClinicaResumen clinica = clinica();

        RecetaPdfService service = new RecetaPdfService();

        byte[] pdf = service.generate(receta, clinica);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100, "El PDF no debe estar vacío");

        String firma = new String(pdf, 0, 5, StandardCharsets.ISO_8859_1);
        assertEquals("%PDF-", firma, "Debe generarse un archivo PDF real");

        String contenido = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(
            contenido.contains("Clinica Odontologica Sonrisa Salud"),
            "El PDF debe identificar a la clínica emisora"
        );
        assertTrue(
            contenido.contains("20601234567"),
            "El PDF debe incluir el RUC de la clínica cuando está registrado"
        );
        assertFalse(
            contenido.contains("ODONTOSYNC - RECETA MEDICA"),
            "OdontoSync no debe presentarse como nombre de la clínica"
        );
    }

    @Test
    void rechazaRecetaNula() {
        RecetaPdfService service = new RecetaPdfService();

        assertThrows(
            IllegalArgumentException.class,
            () -> service.generate(null, clinica())
        );
    }

    @Test
    void rechazaClinicaNula() {
        RecetaPdfService service = new RecetaPdfService();

        assertThrows(
            IllegalArgumentException.class,
            () -> service.generate(receta(), null)
        );
    }

    private static RecetaResumen receta() {
        return new RecetaResumen(
            2L,
            31L,
            "2026-11-20 00:48",
            "Ana Garcia Silva",
            "DICLOFENACO 20G X 7 DIAS CADA 8HRS",
            "COMER ANTES DE CADA TOMA"
        );
    }

    private static ClinicaResumen clinica() {
        return new ClinicaResumen(
            "Clinica Odontologica Sonrisa Salud",
            "20601234567",
            "Jr. Principal 245, Bagua",
            "999111222",
            "ACTIVA"
        );
    }
}
