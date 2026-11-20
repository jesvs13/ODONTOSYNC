package pe.odontosync.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.model.ClinicaResumen;
import pe.odontosync.model.RecetaResumen;

/**
 * Genera el documento PDF de una receta usando la identidad de la clínica emisora.
 * No requiere dependencias externas.
 */
public final class RecetaPdfService {

    public byte[] generate(RecetaResumen receta, ClinicaResumen clinica) {
        if (receta == null) {
            throw new IllegalArgumentException("La receta es obligatoria.");
        }
        if (clinica == null) {
            throw new IllegalArgumentException("La clínica es obligatoria.");
        }

        return buildPdf(buildContent(receta, clinica));
    }

    private String buildContent(RecetaResumen receta, ClinicaResumen clinica) {
        StringBuilder out = new StringBuilder();

        // Encabezado institucional.
        out.append("0.97 0.99 1 rg\n");
        out.append("40 742 515 70 re f\n");

        out.append("0.05 0.25 0.36 rg\n");
        text(out, "F2", 18, 55, 785, safe(clinica.nombre()));
        text(out, "F1", 9, 55, 767, clinicLine(clinica));

        out.append("0.08 0.43 0.55 rg\n");
        text(out, "F2", 15, 405, 785, "RECETA MÉDICA");

        out.append("0.70 0.82 0.87 RG\n");
        out.append("1 w\n");
        out.append("40 733 m 555 733 l S\n");

        // Identificación del documento.
        bodyColor(out);
        text(out, "F2", 10, 55, 708, "Receta N.° " + receta.idReceta());
        text(out, "F1", 10, 370, 708, "Fecha de emisión: " + safe(receta.fecha()));

        // Paciente.
        sectionTitle(out, 55, 672, "PACIENTE");
        infoBox(out, 55, 620, 500, 38);

        bodyColor(out);
        text(out, "F1", 8, 68, 644, "Nombre");
        text(out, "F2", 11, 68, 627, safe(receta.paciente()));

        // Medicamentos.
        sectionTitle(out, 55, 585, "MEDICAMENTOS");

        List<String> medicationLines = wrapLines(safe(receta.medicamentos()), 76);
        if (medicationLines.isEmpty()) {
            medicationLines = List.of("-");
        }

        int medicationHeight = Math.max(62, 28 + medicationLines.size() * 17);
        int medicationBottom = 568 - medicationHeight;
        infoBox(out, 55, medicationBottom, 500, medicationHeight);

        int y = 548;
        int item = 1;

        bodyColor(out);
        for (String line : medicationLines) {
            text(out, "F2", 10, 70, y, item + ".");
            text(out, "F1", 10, 92, y, line);
            item++;
            y -= 17;
        }

        // Indicaciones.
        int indicationsTitleY = medicationBottom - 30;
        sectionTitle(out, 55, indicationsTitleY, "INDICACIONES");

        List<String> indicationLines = wrapLines(safe(receta.indicaciones()), 76);
        if (indicationLines.isEmpty()) {
            indicationLines = List.of("-");
        }

        int indicationHeight = Math.max(62, 28 + indicationLines.size() * 17);
        int indicationBottom = indicationsTitleY - 18 - indicationHeight;
        infoBox(out, 55, indicationBottom, 500, indicationHeight);

        y = indicationsTitleY - 42;

        bodyColor(out);
        for (String line : indicationLines) {
            text(out, "F1", 10, 70, y, line);
            y -= 17;
        }

        // Pie institucional.
        int footerY = Math.max(62, indicationBottom - 55);

        out.append("0.82 0.88 0.91 RG\n");
        out.append("40 ").append(footerY + 18).append(" m 555 ")
            .append(footerY + 18).append(" l S\n");

        out.append("0.35 0.42 0.46 rg\n");
        text(out, "F1", 8, 55, footerY,
            "Documento emitido por " + safe(clinica.nombre()) + ".");
        text(out, "F1", 8, 55, footerY - 13,
            "Receta médica generada electrónicamente.");

        return out.toString();
    }

    private void bodyColor(StringBuilder out) {
        out.append("0.12 0.18 0.23 rg\n");
    }

    private void sectionTitle(StringBuilder out, int x, int y, String title) {
        out.append("0.08 0.43 0.55 rg\n");
        text(out, "F2", 9, x, y, title);
    }

    private void infoBox(
        StringBuilder out,
        int x,
        int y,
        int width,
        int height
    ) {
        out.append("0.985 0.992 0.995 rg\n");
        out.append(x).append(' ').append(y).append(' ')
            .append(width).append(' ').append(height).append(" re f\n");

        out.append("0.84 0.89 0.92 RG\n");
        out.append("0.8 w\n");
        out.append(x).append(' ').append(y).append(' ')
            .append(width).append(' ').append(height).append(" re S\n");
    }

    private String clinicLine(ClinicaResumen clinica) {
        List<String> parts = new ArrayList<>();

        if (!blank(clinica.ruc())) {
            parts.add("RUC " + clinica.ruc().trim());
        }
        if (!blank(clinica.direccion())) {
            parts.add(clinica.direccion().trim());
        }
        if (!blank(clinica.telefono())) {
            parts.add("Tel. " + clinica.telefono().trim());
        }

        return parts.isEmpty()
            ? "Información institucional"
            : String.join("  ·  ", parts);
    }

    private void text(
        StringBuilder out,
        String font,
        int size,
        int x,
        int y,
        String value
    ) {
        out.append("BT\n");
        out.append('/').append(font).append(' ').append(size).append(" Tf\n");
        out.append(x).append(' ').append(y).append(" Td\n");
        out.append(pdfText(value)).append(" Tj\n");
        out.append("ET\n");
    }

    private List<String> wrapLines(String value, int maxLength) {
        List<String> result = new ArrayList<>();

        for (String rawLine : safe(value).replace("\r", "").split("\n", -1)) {
            String line = rawLine.trim();

            if (line.isEmpty()) {
                continue;
            }

            while (line.length() > maxLength) {
                int cut = line.lastIndexOf(' ', maxLength);
                if (cut <= 0) {
                    cut = maxLength;
                }

                result.add(line.substring(0, cut).trim());
                line = line.substring(cut).trim();
            }

            if (!line.isEmpty()) {
                result.add(line);
            }
        }

        return result;
    }

    private byte[] buildPdf(String content) {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        write(pdf, "%PDF-1.4\n");
        write(pdf, "%\u00E2\u00E3\u00CF\u00D3\n");

        offsets.add(pdf.size());
        write(pdf, """
            1 0 obj
            << /Type /Catalog /Pages 2 0 R >>
            endobj
            """);

        offsets.add(pdf.size());
        write(pdf, """
            2 0 obj
            << /Type /Pages /Kids [3 0 R] /Count 1 >>
            endobj
            """);

        offsets.add(pdf.size());
        write(pdf, """
            3 0 obj
            << /Type /Page
               /Parent 2 0 R
               /MediaBox [0 0 595 842]
               /Resources <<
                 /Font <<
                   /F1 4 0 R
                   /F2 5 0 R
                 >>
               >>
               /Contents 6 0 R
            >>
            endobj
            """);

        offsets.add(pdf.size());
        write(pdf, """
            4 0 obj
            << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>
            endobj
            """);

        offsets.add(pdf.size());
        write(pdf, """
            5 0 obj
            << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>
            endobj
            """);

        byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);

        offsets.add(pdf.size());
        write(pdf, "6 0 obj\n");
        write(pdf, "<< /Length " + contentBytes.length + " >>\n");
        write(pdf, "stream\n");
        pdf.writeBytes(contentBytes);
        write(pdf, "\nendstream\nendobj\n");

        int xrefOffset = pdf.size();

        write(pdf, "xref\n0 7\n");
        write(pdf, "0000000000 65535 f \n");

        for (int offset : offsets) {
            write(pdf, String.format("%010d 00000 n \n", offset));
        }

        write(pdf, """
            trailer
            << /Size 7 /Root 1 0 R >>
            startxref
            """);

        write(pdf, Integer.toString(xrefOffset));
        write(pdf, "\n%%EOF\n");

        return pdf.toByteArray();
    }

    private void write(ByteArrayOutputStream out, String value) {
        out.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private String pdfText(String value) {
        String escaped = safe(value)
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)");

        return "(" + escaped + ")";
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
