package pe.odontosync.model;

/** Opción activa de especialidad para formularios de odontólogos. */
public record EspecialidadOpcion(
    long id,
    String nombre,
    String estado
) { }
