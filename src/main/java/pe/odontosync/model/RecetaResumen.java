package pe.odontosync.model;

/** Resumen de una receta emitida. */
public record RecetaResumen(
    long idReceta,
    long idPaciente,
    String fecha,
    String paciente,
    String medicamentos,
    String indicaciones
) {
    /** Compatibilidad con usos previos que todavía no requieren idPaciente. */
    public RecetaResumen(
        long idReceta,
        String fecha,
        String paciente,
        String medicamentos,
        String indicaciones
    ) {
        this(idReceta, 0L, fecha, paciente, medicamentos, indicaciones);
    }

    /** Compatibilidad histórica con resúmenes sin identificadores. */
    public RecetaResumen(
        String fecha,
        String paciente,
        String medicamentos,
        String indicaciones
    ) {
        this(0L, 0L, fecha, paciente, medicamentos, indicaciones);
    }
}
