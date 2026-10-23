package pe.odontosync.model;

/** Odontólogo disponible para selectores de agenda/horarios. */
public record OdontologoOpcion(
    long idOdontologo,
    String nombre,
    String estado
) { }
