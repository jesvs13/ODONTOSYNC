package pe.odontosync.model;

/** Registro clínico de una atención odontológica vinculada a una cita. */
public record ConsultaClinicaDetalle(
    long idConsulta,
    String fechaConsulta,
    String motivoConsulta,
    String diagnostico,
    String tratamiento,
    String observaciones,
    String recomendaciones
) { }
