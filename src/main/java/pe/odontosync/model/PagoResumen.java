package pe.odontosync.model;

/** Resumen de un pago asociado a una cita. */
public record PagoResumen(String fecha, String paciente, String monto, String metodo) { }
