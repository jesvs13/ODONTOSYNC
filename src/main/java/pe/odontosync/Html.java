package pe.odontosync;

public final class Html {
    private Html() { }

    public static String escape(String raw) {
        if (raw == null) return "";
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String moduleLabel(String key) {
        return switch (key) {
            case "pacientes" -> "Pacientes";
            case "odontologos" -> "Odontólogos";
            case "horarios" -> "Horarios";
            case "agenda" -> "Agenda";
            case "historias" -> "Historia clínica";
            case "recetas" -> "Recetas";
            case "placas" -> "Placas radiográficas";
            case "pagos" -> "Pagos";
            case "inventario" -> "Inventario";
            case "reportes" -> "Reportes";
            case "usuarios" -> "Usuarios";
            case "clinica" -> "Datos clínica";
            default -> "Módulo";
        };
    }
}
