package pe.odontosync.model;

/** Datos mínimos que necesita la vista de agenda/citas. */
public record CitaResumen(
    String fecha,
    String horaInicio,
    String paciente,
    String odontologo,
    String estado,
    String motivo
) { }
