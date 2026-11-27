package pe.odontosync.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import pe.odontosync.Role;
import pe.odontosync.dao.UsuarioCrudDAO;
import pe.odontosync.model.UsuarioDetalle;
import pe.odontosync.security.PasswordPolicy;
import pe.odontosync.security.Passwords;

/** Reglas de negocio del CRUD de usuarios. No contiene SQL ni código HTTP. */
public final class UsuarioCrudService {
    private final UsuarioCrudDAO dao;
    private final CorreoInstitucionalService emails;
    private static final int MAX_CREATE_ATTEMPTS = 5;

    public record CreatedUser(long id, String correo) { }

    public UsuarioCrudService(UsuarioCrudDAO dao) {
        this.dao = dao;
        this.emails = new CorreoInstitucionalService(dao::emailExistsGlobally);
    }

    public List<UsuarioDetalle> list(long clinicId) throws SQLException {
        return dao.findDetailedByClinic(clinicId);
    }

    public Optional<UsuarioDetalle> find(long clinicId, long userId) throws SQLException {
        return dao.findDetailedById(clinicId, userId);
    }

    public String suggestEmail(String nombres, String primerApellido, String segundoApellido)
        throws SQLException {
        String apellidos = joinSurnames(primerApellido, segundoApellido);
        validateNames(nombres, apellidos);
        return emails.generate(nombres, primerApellido, segundoApellido);
    }

    public CreatedUser create(
        long clinicId, String nombres, String primerApellido, String segundoApellido, String telefono,
        String roleValue, String estado, char[] password, String numeroColegiatura
    ) throws SQLException {
        Role role = parseRole(roleValue);
        if (role == Role.ODONTOLOGO) {
            throw new IllegalArgumentException("Registra al profesional desde el módulo Odontólogos.");
        }
        String apellidos = joinSurnames(primerApellido, segundoApellido);
        validateCommon(nombres, apellidos, telefono, estado);
        PasswordPolicy.validate(password, true);
        String colegiatura = null;
        String hash = Passwords.hash(password);

        for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
            String email = emails.generate(nombres, primerApellido, segundoApellido);
            try {
                long id = dao.create(clinicId, role, clean(nombres), apellidos, email,
                    nullable(telefono), hash, estado, colegiatura);
                return new CreatedUser(id, email);
            } catch (SQLException ex) {
                // El índice global decide en caso de altas simultáneas.
                // No reintentar errores de integridad que no correspondan a un correo ocupado.
                if (ex.getErrorCode() != 1062 || !dao.emailExistsGlobally(email)) throw ex;
            }
        }
        throw new IllegalArgumentException("El correo cambió durante el registro. Intenta nuevamente.");
    }

    private String joinSurnames(String first, String second) {
        if (clean(first).isEmpty()) throw new IllegalArgumentException("Ingresa el primer apellido.");
        return (clean(first) + " " + clean(second)).trim();
    }

    public void update(
        long clinicId, long currentUserId, long userId,
        String nombres, String apellidos, String telefono,
        String roleValue, String estado, char[] newPassword, String numeroColegiatura
    ) throws SQLException {
        UsuarioDetalle existing = dao.findDetailedById(clinicId, userId)
            .orElseThrow(() -> new IllegalArgumentException("El usuario ya no existe."));

        Role role = parseRole(roleValue);
        validateCommon(nombres, apellidos, telefono, estado);
        boolean professional = existing.role() == Role.ODONTOLOGO || existing.numeroColegiatura() != null;
        if (professional && role != existing.role() || !professional && role == Role.ODONTOLOGO) {
            throw new IllegalArgumentException("El rol de odontólogo se gestiona desde Odontólogos.");
        }
        if (professional && (!Objects.equals(clean(nombres), clean(existing.nombres()))
            || !Objects.equals(clean(apellidos), clean(existing.apellidos()))
            || !Objects.equals(clean(telefono), clean(existing.telefono()))
            || (!clean(numeroColegiatura).isEmpty()
                && !Objects.equals(clean(numeroColegiatura), clean(existing.numeroColegiatura()))))) {
            throw new IllegalArgumentException("Edita los datos del profesional desde Odontólogos.");
        }
        String colegiatura = professional ? existing.numeroColegiatura() : null;

        if (currentUserId == userId && (role != Role.ADMINISTRADOR || !"ACTIVO".equals(estado))) {
            throw new IllegalArgumentException("No puedes quitarte el rol de administrador ni desactivar tu propia cuenta.");
        }

        if (existing.role() == Role.ADMINISTRADOR && "ACTIVO".equals(existing.estado())
            && (role != Role.ADMINISTRADOR || !"ACTIVO".equals(estado))
            && dao.countActiveAdministrators(clinicId) <= 1) {
            throw new IllegalArgumentException("Debe quedar al menos un administrador activo en la clínica.");
        }

        String hash = null;
        if (newPassword != null && newPassword.length > 0) {
            PasswordPolicy.validate(newPassword, false);
            hash = Passwords.hash(newPassword);
        }

        dao.update(
            clinicId, userId, role, clean(nombres), clean(apellidos), existing.correo(),
            nullable(telefono), hash, estado, colegiatura
        );
    }

    public void delete(long clinicId, long currentUserId, long userId) throws SQLException {
        if (currentUserId == userId) {
            throw new IllegalArgumentException("No puedes eliminar la cuenta con la que estás conectado.");
        }
        UsuarioDetalle existing = dao.findDetailedById(clinicId, userId)
            .orElseThrow(() -> new IllegalArgumentException("El usuario ya no existe."));
        if (existing.role() == Role.ODONTOLOGO || existing.numeroColegiatura() != null) {
            throw new IllegalArgumentException("Inactiva al profesional desde Odontólogos; su cuenta no se elimina aquí.");
        }
        if (existing.role() == Role.ADMINISTRADOR && "ACTIVO".equals(existing.estado())
            && dao.countActiveAdministrators(clinicId) <= 1) {
            throw new IllegalArgumentException("Debe quedar al menos un administrador activo en la clínica.");
        }
        if (!dao.delete(clinicId, userId)) {
            throw new IllegalArgumentException(
                "La cuenta tiene movimientos o pagos asociados. Cámbiala a INACTIVO en lugar de eliminarla."
            );
        }
    }

    private void validateNames(String nombres, String apellidos) {
        if (clean(nombres).length() < 2 || clean(nombres).length() > 100) {
            throw new IllegalArgumentException("Los nombres deben tener entre 2 y 100 caracteres.");
        }
        if (clean(apellidos).length() < 2 || clean(apellidos).length() > 100) {
            throw new IllegalArgumentException("Los apellidos deben tener entre 2 y 100 caracteres.");
        }
    }

    private void validateCommon(String nombres, String apellidos, String telefono, String estado) {
        validateNames(nombres, apellidos);
        String phone = clean(telefono);
        if (phone.length() > 20 || (!phone.isEmpty() && !phone.matches("[0-9+() -]{6,20}"))) {
            throw new IllegalArgumentException("El teléfono contiene caracteres no válidos.");
        }
        if (!List.of("ACTIVO", "INACTIVO", "BLOQUEADO").contains(estado)) {
            throw new IllegalArgumentException("Estado de usuario no válido.");
        }
    }

    private Role parseRole(String value) {
        Role role = Role.fromDatabase(value);
        if (role == null) throw new IllegalArgumentException("Rol no válido.");
        return role;
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String nullable(String value) { String v = clean(value); return v.isEmpty() ? null : v; }
}
