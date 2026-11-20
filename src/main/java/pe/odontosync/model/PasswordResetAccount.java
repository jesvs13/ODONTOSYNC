package pe.odontosync.model;

/** Cuenta activa destinataria de una solicitud de recuperacion. */
public record PasswordResetAccount(
    long userId,
    long clinicId,
    String email,
    String firstName,
    String clinicName
) { }
