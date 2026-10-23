package pe.odontosync.model;

/** Paciente disponible para el selector de agenda. */
public record PacienteOpcion(
    long idPaciente,
    String nombre,
    String documento,
    String estado
) { }
