package pe.odontosync.model;

/** Datos mínimos que el servicio de autenticación necesita desde persistencia. */
public record LoginAccount(
    long userId,
    long clinicId,
    String firstName,
    String lastName,
    String passwordHash,
    String userStatus,
    String roleName,
    String clinicName,
    String clinicStatus
) { }
