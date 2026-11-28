package pe.odontosync.security;

/** Validación de formato del DNI peruano sin perder ceros iniciales. */
public final class DniPolicy {
    private DniPolicy() { }

    public static String normalize(String dni) {
        String normalized = dni == null ? "" : dni.strip();
        if (!normalized.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente ocho dígitos.");
        }
        return normalized;
    }
}
