package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import pe.odontosync.model.RecetaResumen;

class RecetaPdfServiceTest {

    @Test
    void generaPdfValidoParaRecetaIndividual() {
        RecetaResumen receta = new RecetaResumen(
            801L,
            "2026-11-19 20:00",
            "Ana García Silva",
            "ibuprofeno 500 cada 8hrs\nparacetamol 500 cada 8hrs",
            "después de un alimento"
        );

        RecetaPdfService service = new RecetaPdfService();

        byte[] pdf = service.generate(receta);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100, "El PDF no debe estar vacío");

        String firma = new String(pdf, 0, 5, StandardCharsets.ISO_8859_1);
        assertEquals("%PDF-", firma, "Debe generarse un archivo PDF real");
    }

    @Test
    void rechazaRecetaNula() {
        RecetaPdfService service = new RecetaPdfService();

        assertThrows(IllegalArgumentException.class, () -> service.generate(null));
    }
}
