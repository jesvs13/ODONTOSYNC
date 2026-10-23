package pe.odontosync.model;

/** Datos mínimos que necesita la vista de horarios. */
public record HorarioResumen(
    String odontologo,
    String dia,
    String horaInicio,
    String horaFin,
    String estado
) { }
