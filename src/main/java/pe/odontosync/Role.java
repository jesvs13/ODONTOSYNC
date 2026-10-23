package pe.odontosync;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.List;

/** Roles y permisos de navegación. Guava Multimap evita duplicar estructuras mutables. */
public enum Role {
    ADMINISTRADOR("Administrador"),
    RECEPCIONISTA("Recepción"),
    ODONTOLOGO("Odontólogo");

    private static final ImmutableListMultimap<Role, String> MODULES =
        ImmutableListMultimap.<Role, String>builder()
            .putAll(ADMINISTRADOR, "pacientes", "odontologos", "horarios", "agenda", "pagos", "inventario", "reportes", "usuarios", "clinica")
            .putAll(RECEPCIONISTA, "pacientes", "odontologos", "horarios", "agenda", "pagos")
            .putAll(ODONTOLOGO, "pacientes", "horarios", "agenda", "historias", "recetas", "placas")
            .build();

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public boolean allows(String module) {
        return module != null && MODULES.containsEntry(this, module);
    }

    public List<String> modules() {
        return ImmutableList.copyOf(MODULES.get(this));
    }

    public String label() { return label; }
    public String heading() { return "Panel " + label.toLowerCase(java.util.Locale.ROOT); }
    public String subtitle() {
        return switch (this) {
            case ADMINISTRADOR -> "Resumen por clínica y accesos de administración";
            case RECEPCIONISTA -> "Agenda diaria, pacientes y cobros";
            case ODONTOLOGO -> "Atenciones, historia clínica y recetas";
        };
    }
    public String welcome() {
        return switch (this) {
            case ADMINISTRADOR -> "La clínica, de un vistazo";
            case RECEPCIONISTA -> "Tu jornada comienza aquí";
            case ODONTOLOGO -> "Tus pacientes te esperan";
        };
    }
    public String detail() {
        return switch (this) {
            case ADMINISTRADOR -> "Consulta operaciones, cobros e indicadores.";
            case RECEPCIONISTA -> "Coordina citas, pacientes y pagos.";
            case ODONTOLOGO -> "Continúa con tus atenciones y registros clínicos.";
        };
    }
    public String code() {
        return switch (this) {
            case ADMINISTRADOR -> "CU01 · RF02";
            case RECEPCIONISTA -> "CU01 · CU02 · CU06";
            case ODONTOLOGO -> "CU01 · CU03 · CU04";
        };
    }

    public static Role fromDatabase(String value) {
        if (value == null) return null;
        try { return Role.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
