package pe.odontosync.model;

/** Fila de navegación de historias para el odontólogo. */
public record HistoriaPacienteResumen(
    long idPaciente,
    String paciente,
    String documento,
    String apertura,
    String estado,
    String ultimaConsulta
) { }
