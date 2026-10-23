package pe.odontosync.model;

public record PacienteRecetaDetalle(
    String fecha,
    String odontologo,
    String medicamentos,
    String indicaciones
) { }
