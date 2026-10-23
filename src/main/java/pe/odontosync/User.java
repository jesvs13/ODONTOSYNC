package pe.odontosync;

public record User(long id, long clinicId, String name, String clinicName, Role role)
    implements java.io.Serializable { }
