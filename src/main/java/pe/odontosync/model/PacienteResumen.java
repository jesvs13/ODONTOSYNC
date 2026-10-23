package pe.odontosync.model;

/** Datos mínimos que necesita la vista de pacientes. */
public record PacienteResumen(
    String nombre,
    String documento,
    String telefono,
    String estado
) { }
