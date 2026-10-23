package pe.odontosync.model;

import pe.odontosync.Role;

/** Datos administrativos de una cuenta; nunca expone el hash de la contraseña. */
public record UsuarioDetalle(
    long id,
    String nombres,
    String apellidos,
    String correo,
    String telefono,
    Role role,
    String estado,
    String numeroColegiatura
) { }
