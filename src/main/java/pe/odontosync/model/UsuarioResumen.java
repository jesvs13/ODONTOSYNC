package pe.odontosync.model;

/** Datos mínimos que necesita la vista de usuarios. */
public record UsuarioResumen(String nombre, String correo, String rol, String estado) { }
