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
    String clinicStatus,
    boolean mustChangePassword,
    long credentialVersion
) {
    public LoginAccount(long userId, long clinicId, String firstName, String lastName,
                        String passwordHash, String userStatus, String roleName,
                        String clinicName, String clinicStatus, boolean mustChangePassword) {
        this(userId, clinicId, firstName, lastName, passwordHash, userStatus, roleName,
            clinicName, clinicStatus, mustChangePassword, 0L);
    }
    public LoginAccount(long userId, long clinicId, String firstName, String lastName,
                        String passwordHash, String userStatus, String roleName,
                        String clinicName, String clinicStatus) {
        this(userId, clinicId, firstName, lastName, passwordHash, userStatus, roleName,
            clinicName, clinicStatus, false);
    }
}
