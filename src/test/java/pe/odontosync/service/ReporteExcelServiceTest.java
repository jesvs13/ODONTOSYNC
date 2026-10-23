package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.ReporteDAO;
import pe.odontosync.model.ReporteEstadoResumen;

class ReporteExcelServiceTest {
    @Test
    void generaXlsxConDatosDeLaClinica() throws Exception {
        ReporteDAO dao = clinicId -> List.of(
            new ReporteEstadoResumen("CONFIRMADA", "2"),
            new ReporteEstadoResumen("ATENDIDA", "5")
        );
        ReporteExcelService service = new ReporteExcelService(dao);
        User admin = new User(1, 7, "Admin", "Clínica Demo", Role.ADMINISTRADOR);

        byte[] bytes = service.exportAppointmentsByStatus(admin);
        assertTrue(bytes.length > 500);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("Citas por estado", workbook.getSheetAt(0).getSheetName());
            assertEquals("CONFIRMADA", workbook.getSheetAt(0).getRow(5).getCell(0).getStringCellValue());
            assertEquals(2d, workbook.getSheetAt(0).getRow(5).getCell(1).getNumericCellValue());
        }
    }
}
