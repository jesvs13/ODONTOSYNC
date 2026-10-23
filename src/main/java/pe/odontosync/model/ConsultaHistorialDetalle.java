package pe.odontosync.model;

/** Consulta visible dentro de la historia longitudinal de un paciente. */
public record ConsultaHistorialDetalle(
    long idConsulta,
    long idCita,
    String fechaConsulta,
    String odontologo,
    String especialidad,
    String motivo,
    String diagnostico,
    String tratamiento,
    String observaciones,
    String recomendaciones
) { }
