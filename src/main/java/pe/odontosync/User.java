package pe.odontosync;

public record User(long id, long clinicId, String name, String clinicName, Role role, boolean mustChangePassword)
    implements java.io.Serializable {
    public User(long id, long clinicId, String name, String clinicName, Role role) {
        this(id, clinicId, name, clinicName, role, false);
    }
}
