package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import pe.odontosync.Role;
import pe.odontosync.model.UsuarioDetalle;

/** Operaciones CRUD administrativas sobre cuentas de usuario. */
public interface UsuarioCrudDAO {
    List<UsuarioDetalle> findDetailedByClinic(long clinicId) throws SQLException;
    Optional<UsuarioDetalle> findDetailedById(long clinicId, long userId) throws SQLException;
    boolean emailExists(long clinicId, String email, Long excludeUserId) throws SQLException;
    long countActiveAdministrators(long clinicId) throws SQLException;

    long create(
        long clinicId, Role role, String nombres, String apellidos, String correo,
        String telefono, String passwordHash, String estado, String numeroColegiatura
    ) throws SQLException;

    void update(
        long clinicId, long userId, Role role, String nombres, String apellidos, String correo,
        String telefono, String passwordHashOrNull, String estado, String numeroColegiatura
    ) throws SQLException;

    /** Elimina la cuenta si no tiene referencias que deban conservarse. */
    boolean delete(long clinicId, long userId) throws SQLException;
}
