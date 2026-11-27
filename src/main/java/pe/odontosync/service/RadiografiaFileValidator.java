package pe.odontosync.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Validación del nombre, extensión, MIME y contenido; sin acceso a disco ni a MySQL. */
final class RadiografiaFileValidator {
    private static final long MAX_PIXELS = 40_000_000L;
    private static final Pattern PDF_END = Pattern.compile(
        "startxref\\s+([0-9]{1,10})\\s+%%EOF[\\x00\\t\\n\\f\\r ]*\\z");
    private static final byte[] PNG_END = {0,0,0,0,73,69,78,68,(byte)174,66,96,(byte)130};

    private RadiografiaFileValidator() { }

    static String validateName(String raw) {
        String name = clean(raw);
        if (name.isEmpty() || name.length() > 255 || name.startsWith(".")
            || name.contains("..") || name.matches("(?s).*[\\\\/:*?\"<>|\\p{Cntrl}].*")) {
            throw new IllegalArgumentException("Nombre de archivo no válido.");
        }
        return name;
    }

    static String validateContent(String name, String mime, byte[] bytes) {
        String lower = name.toLowerCase(Locale.ROOT);
        if ("image/png".equals(mime) && lower.endsWith(".png")) {
            if (!endsWith(bytes, PNG_END)) throw invalidFile();
            validateImage(bytes, "png");
            return ".png";
        }
        if ("image/jpeg".equals(mime) && (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
            if (!endsWith(bytes, new byte[]{(byte)255, (byte)217})) throw invalidFile();
            validateImage(bytes, "jpeg");
            return ".jpg";
        }
        if ("application/pdf".equals(mime) && lower.endsWith(".pdf")) {
            validatePdf(bytes);
            return ".pdf";
        }
        throw invalidFile();
    }

    private static boolean endsWith(byte[] bytes, byte[] tail) {
        if (bytes.length < tail.length) return false;
        for (int i = 0; i < tail.length; i++) {
            if (bytes[bytes.length - tail.length + i] != tail[i]) return false;
        }
        return true;
    }

    private static void validateImage(byte[] bytes, String format) {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidFile();
            ImageReader reader = readers.next();
            try {
                if (!format.equalsIgnoreCase(reader.getFormatName())) throw invalidFile();
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long)width * height > MAX_PIXELS) throw invalidFile();
                if (reader.read(0) == null) throw invalidFile();
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw invalidFile();
        }
    }

    // Comprobación estructural inicial, no sustituye un parser ni un análisis de seguridad PDF.
    private static void validatePdf(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.ISO_8859_1);
        if (!text.matches("(?s)%PDF-(1\\.[0-7]|2\\.0)[\\r\\n].*")) throw invalidFile();
        Matcher end = PDF_END.matcher(text.substring(Math.max(0, text.length() - 4096)));
        if (!end.find()) throw invalidFile();
        long offset = Long.parseLong(end.group(1));
        if (offset < 9 || offset >= text.length()) throw invalidFile();
        int start = (int)offset;
        String section = text.substring(start, Math.min(start + 4096, text.length()));
        boolean classic = section.startsWith("xref") && text.indexOf("trailer", start) >= 0;
        boolean stream = section.matches("(?s)^[0-9]+\\s+[0-9]+\\s+obj\\b.*?/Type\\s*/XRef\\b.*");
        if (!classic && !stream) throw invalidFile();
    }

    private static IllegalArgumentException invalidFile() {
        return new IllegalArgumentException("El contenido del archivo no corresponde a un JPG, PNG o PDF válido.");
    }

    private static String clean(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
