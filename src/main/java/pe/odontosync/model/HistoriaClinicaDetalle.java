package pe.odontosync.model;

/** Cabecera de la historia clínica longitudinal del paciente. */
public record HistoriaClinicaDetalle(
    long idHistoria,
    String fechaApertura,
    String observacionesGenerales,
    String estado
) { }
