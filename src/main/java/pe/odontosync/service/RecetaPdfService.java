package pe.odontosync.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.model.RecetaResumen;

/**
 * Genera un PDF simple y autónomo para una receta individual.
 * No requiere dependencias externas.
 */
public final class RecetaPdfService {

    public byte[] generate(RecetaResumen receta) {
        if (receta == null) {
            throw new IllegalArgumentException("La receta es obligatoria.");
        }

        String content = buildContent(receta);
        return buildPdf(content);
    }

    private String buildContent(RecetaResumen receta) {
        StringBuilder out = new StringBuilder();

        out.append("BT\n");
        out.append("/F1 18 Tf\n");
        out.append("50 790 Td\n");
        out.append(pdfText("ODONTOSYNC - RECETA MEDICA")).append(" Tj\n");

        out.append("0 -30 Td\n");
        out.append("/F1 11 Tf\n");
        out.append(pdfText("Receta #" + receta.idReceta())).append(" Tj\n");

        out.append("0 -20 Td\n");
        out.append(pdfText("Paciente: " + safe(receta.paciente()))).append(" Tj\n");

        out.append("0 -18 Td\n");
        out.append(pdfText("Fecha de emision: " + safe(receta.fecha()))).append(" Tj\n");

        out.append("0 -28 Td\n");
        out.append("/F1 13 Tf\n");
        out.append(pdfText("Medicamentos")).append(" Tj\n");

        out.append("/F1 11 Tf\n");
        appendMultiline(out, receta.medicamentos(), 18);

        out.append("0 -12 Td\n");
        out.append("/F1 13 Tf\n");
        out.append(pdfText("Indicaciones")).append(" Tj\n");

        out.append("/F1 11 Tf\n");
        appendMultiline(out, receta.indicaciones(), 18);

        out.append("ET\n");
        return out.toString();
    }

    private void appendMultiline(StringBuilder out, String value, int leading) {
        List<String> lines = wrap(safe(value), 78);
        if (lines.isEmpty()) {
            lines = List.of("-");
        }

        for (String line : lines) {
            out.append("0 -").append(leading).append(" Td\n");
            out.append(pdfText(line)).append(" Tj\n");
        }
    }

    private List<String> wrap(String value, int maxLength) {
        List<String> result = new ArrayList<>();

        for (String rawLine : value.replace("\r", "").split("\n", -1)) {
            String line = rawLine.trim();

            if (line.isEmpty()) {
                result.add("");
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

            result.add(line);
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
               /Resources << /Font << /F1 4 0 R >> >>
               /Contents 5 0 R
            >>
            endobj
            """);

        offsets.add(pdf.size());
        write(pdf, """
            4 0 obj
            << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>
            endobj
            """);

        byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);
        offsets.add(pdf.size());
        write(pdf, "5 0 obj\n");
        write(pdf, "<< /Length " + contentBytes.length + " >>\n");
        write(pdf, "stream\n");
        pdf.writeBytes(contentBytes);
        write(pdf, "\nendstream\nendobj\n");

        int xrefOffset = pdf.size();
        write(pdf, "xref\n0 6\n");
        write(pdf, "0000000000 65535 f \n");

        for (int offset : offsets) {
            write(pdf, String.format("%010d 00000 n \n", offset));
        }

        write(pdf, """
            trailer
            << /Size 6 /Root 1 0 R >>
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
