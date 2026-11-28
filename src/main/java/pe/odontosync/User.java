package pe.odontosync;

public record User(long id, long clinicId, String name, String clinicName, Role role, boolean mustChangePassword, long credentialVersion)
    implements java.io.Serializable {
    public User(long id, long clinicId, String name, String clinicName, Role role, boolean mustChangePassword) {
        this(id, clinicId, name, clinicName, role, mustChangePassword, 0L);
    }
    public User(long id, long clinicId, String name, String clinicName, Role role) {
        this(id, clinicId, name, clinicName, role, false);
    }
}
