package pe.odontosync.model;

/** Resumen de una receta emitida. */
public record RecetaResumen(
    long idReceta,
    String fecha,
    String paciente,
    String medicamentos,
    String indicaciones
) { }
