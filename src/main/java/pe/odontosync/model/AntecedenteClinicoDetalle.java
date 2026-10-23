package pe.odontosync.model;

public record AntecedenteClinicoDetalle(
    long idAntecedente,
    String tipo,
    String descripcion,
    String observaciones,
    String fechaRegistro
) { }
