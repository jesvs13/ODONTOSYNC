package pe.odontosync.model;

/** Cita atendida que todavía no cuenta con un pago registrado. */
public record PagoPendiente(
    long idCita,
    String fecha,
    String hora,
    String paciente,
    String documento,
    String odontologo
) { }
