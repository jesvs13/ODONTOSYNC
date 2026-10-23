package pe.odontosync.model;

public record PacientePagoDetalle(
    String fecha,
    String monto,
    String metodo,
    String cita
) { }
