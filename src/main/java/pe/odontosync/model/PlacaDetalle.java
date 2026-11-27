package pe.odontosync.model;

import java.time.LocalDate;

/** Metadatos de una radiografía; la clave del archivo es interna y privada. */
public record PlacaDetalle(
    long idPlaca,
    long idPaciente,
    long idUsuario,
    String tipoRadiografia,
    LocalDate fechaRadiografia,
    String regionDental,
    String observaciones,
    String nombreArchivo,
    String claveArchivo,
    String tipoMime,
    long tamanoBytes,
    String estado
) { }
