package pe.odontosync.service;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Genera candidatos de correo institucional; no realiza escrituras ni reserva cuentas. */
public final class CorreoInstitucionalService {
    private static final String DOMAIN = "@odontosync.pe";
    private static final int MAX_LOCAL_LENGTH = 64;
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_LETTERS = Pattern.compile("[^a-z]");

    /** Consulta global: true si el correo ya existe, sin filtrar por clínica. */
    @FunctionalInterface
    public interface EmailAvailability {
        boolean exists(String email) throws SQLException;
    }

    private final EmailAvailability availability;

    public CorreoInstitucionalService(EmailAvailability availability) {
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    public String generate(String nombres, String primerApellido, String segundoApellido)
        throws SQLException {
        String names = normalize(nombres);
        String surname = normalize(primerApellido);
        if (names.isEmpty() || surname.isEmpty()) {
            throw new IllegalArgumentException("Ingresa nombres y primer apellido válidos.");
        }

        String base = names.substring(0, 1) + surname;
        String candidate = candidate(base, "");
        if (!availability.exists(candidate)) {
            return candidate;
        }

        String second = normalize(segundoApellido);
        String initial = second.isEmpty() ? "" : second.substring(0, 1);
        if (!initial.isEmpty()) {
            candidate = candidate(base, initial);
            if (!availability.exists(candidate)) {
                return candidate;
            }
        }

        for (long number = 2; number < Long.MAX_VALUE; number++) {
            candidate = candidate(base, initial + number);
            if (!availability.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No se pudo generar un correo institucional disponible.");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String unaccented = MARKS.matcher(decomposed).replaceAll("");
        return NON_LETTERS.matcher(unaccented.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static String candidate(String base, String suffix) {
        int limit = MAX_LOCAL_LENGTH - suffix.length();
        return base.substring(0, Math.min(base.length(), limit)) + suffix + DOMAIN;
    }
}
