package pe.odontosync.model;

/** Identidad minima devuelta al consumir correctamente un token. */
public record PasswordResetResult(long userId, long clinicId) { }
