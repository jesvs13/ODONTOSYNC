package pe.odontosync.service;

import java.sql.SQLException;
import pe.odontosync.security.DniPolicy;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.UsuarioCrudDAO;
import pe.odontosync.model.UsuarioDetalle;

/** Reglas de negocio del CRUD de usuarios. No contiene SQL ni código HTTP. */
public final class UsuarioCrudService {
    private final UsuarioCrudDAO dao;
    private final CorreoInstitucionalService emails;

    private final TemporaryAccountCredentials credentials = new TemporaryAccountCredentials();

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

    public CreatedAccount create(
        long clinicId, String nombres, String primerApellido, String segundoApellido, String telefono,
        String roleValue, String estado, String dni
    ) throws SQLException {
        Role role = parseRole(roleValue);
        if (role == Role.ODONTOLOGO) {
            throw new IllegalArgumentException("Registra al profesional desde el módulo Odontólogos.");
        }
        String apellidos = joinSurnames(primerApellido, segundoApellido);
        validateCommon(nombres, apellidos, telefono, estado);
        String normalizedDni = DniPolicy.normalize(dni);
        return credentials.create(emails, nombres, primerApellido, segundoApellido,
            (email, hash) -> dao.create(clinicId, role, clean(nombres), apellidos, email,
                nullable(telefono), hash, estado, normalizedDni));
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
        boolean professional = isProfessional(existing);
        if ((professional && role != existing.role()) || (!professional && role == Role.ODONTOLOGO)) {
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

        if (newPassword != null && newPassword.length > 0) {
            throw new IllegalArgumentException("Usa el restablecimiento de contraseña para generar una clave temporal.");
        }

        dao.update(
            clinicId, userId, role, clean(nombres), clean(apellidos), existing.correo(),
            nullable(telefono), null, estado, colegiatura
        );
    }

    public CreatedAccount resetPassword(User actor, long userId) throws SQLException {
        if (actor == null || actor.role() != Role.ADMINISTRADOR || actor.mustChangePassword()) {
            throw new IllegalArgumentException("Solo un administrador activado puede restablecer contraseñas.");
        }
        if (actor.id() == userId) {
            throw new IllegalArgumentException("Usa el cambio de contraseña de tu propia cuenta.");
        }
        UsuarioDetalle target = dao.findDetailedById(actor.clinicId(), userId)
            .orElseThrow(() -> new IllegalArgumentException("El usuario ya no existe."));
        return credentials.reset(userId, target.correo(), hash -> {
            if (!dao.resetTemporaryPassword(actor.clinicId(), userId, hash)) {
                throw new IllegalArgumentException("El usuario ya no existe.");
            }
        });
    }

    public void delete(long clinicId, long currentUserId, long userId) throws SQLException {
        if (currentUserId == userId) {
            throw new IllegalArgumentException("No puedes eliminar la cuenta con la que estás conectado.");
        }
        UsuarioDetalle existing = dao.findDetailedById(clinicId, userId)
            .orElseThrow(() -> new IllegalArgumentException("El usuario ya no existe."));
        if (isProfessional(existing)) {
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

    private boolean isProfessional(UsuarioDetalle user) {
        return user.role() == Role.ODONTOLOGO || user.numeroColegiatura() != null;
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
