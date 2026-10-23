package pe.odontosync.model;

/** Datos mínimos que necesita la vista de odontólogos. */
public record OdontologoResumen(
    String nombre,
    String colegiatura,
    String correo,
    String estado
) { }
