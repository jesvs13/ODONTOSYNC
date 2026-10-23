package pe.odontosync.service;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.ReporteDAO;
import pe.odontosync.model.ReporteEstadoResumen;

/**
 * Genera reportes XLSX con Apache POI.
 * La lista de columnas es inmutable con Guava y la normalización de texto usa Apache Commons Lang.
 */
public final class ReporteExcelService {
    private static final List<String> HEADERS = ImmutableList.of("Estado de cita", "Cantidad");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReporteDAO reporteDAO;

    public ReporteExcelService(ReporteDAO reporteDAO) {
        this.reporteDAO = reporteDAO;
    }

    /**
     * Exporta el resumen de citas por estado para la clínica del administrador.
     *
     * @param current usuario autenticado
     * @return contenido binario de un archivo .xlsx
     * @throws SQLException si falla la consulta de MySQL
     * @throws IOException si no se puede construir el libro Excel
     */
    public byte[] exportAppointmentsByStatus(User current) throws SQLException, IOException {
        if (current == null || current.role() != Role.ADMINISTRADOR) {
            throw new IllegalArgumentException("Solo Administración puede exportar reportes.");
        }

        List<ReporteEstadoResumen> rows = reporteDAO.countAppointmentsByStatus(current.clinicId());
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Citas por estado");
            sheet.setColumnWidth(0, 28 * 256);
            sheet.setColumnWidth(1, 16 * 256);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row title = sheet.createRow(0);
            Cell titleCell = title.createCell(0);
            titleCell.setCellValue("OdontoSync · Resumen de citas por estado");
            titleCell.setCellStyle(titleStyle);

            Row clinic = sheet.createRow(1);
            clinic.createCell(0).setCellValue("Clínica");
            clinic.createCell(1).setCellValue(clean(current.clinicName()));

            Row generated = sheet.createRow(2);
            generated.createCell(0).setCellValue("Generado");
            generated.createCell(1).setCellValue(DATE_TIME.format(LocalDateTime.now()));

            Row header = sheet.createRow(4);
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 5;
            for (ReporteEstadoResumen item : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(clean(item.estado()));
                row.createCell(1).setCellValue(parseCount(item.citas()));
            }

            if (rows.isEmpty()) {
                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue("Sin registros");
                row.createCell(1).setCellValue(0);
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String clean(String value) {
        return StringUtils.defaultIfBlank(StringUtils.trim(value), "—");
    }

    private int parseCount(String value) {
        try {
            return Integer.parseInt(StringUtils.trimToEmpty(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
