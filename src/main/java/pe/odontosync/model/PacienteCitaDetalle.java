package pe.odontosync.model;

public record PacienteCitaDetalle(
    String fecha,
    String hora,
    String odontologo,
    String estado,
    String motivo
) { }
