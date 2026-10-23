package pe.odontosync.model;

public record AlergiaClinicaDetalle(
    long idAlergia,
    String alergeno,
    String reaccion,
    String observaciones,
    String fechaRegistro
) { }
