package pe.odontosync.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import pe.odontosync.Role;
import pe.odontosync.dao.UsuarioCrudDAO;
import pe.odontosync.model.UsuarioDetalle;
import pe.odontosync.security.Passwords;

/** Reglas de negocio del CRUD de usuarios. No contiene SQL ni código HTTP. */
public final class UsuarioCrudService {
    private final UsuarioCrudDAO dao;

    public UsuarioCrudService(UsuarioCrudDAO dao) {
        this.dao = dao;
    }

    public List<UsuarioDetalle> list(long clinicId) throws SQLException {
        return dao.findDetailedByClinic(clinicId);
    }

    public Optional<UsuarioDetalle> find(long clinicId, long userId) throws SQLException {
        return dao.findDetailedById(clinicId, userId);
    }

    public long create(
        long clinicId, String nombres, String apellidos, String correo, String telefono,
        String roleValue, String estado, char[] password, String numeroColegiatura
    ) throws SQLException {
        Role role = parseRole(roleValue);
        String normalizedEmail = validateCommon(nombres, apellidos, correo, telefono, estado);
        validatePassword(password, true);
        String colegiatura = validateColegiatura(role, numeroColegiatura);

        if (dao.emailExists(clinicId, normalizedEmail, null)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo en la clínica.");
        }

        String hash = Passwords.hash(password);
        return dao.create(
            clinicId, role, clean(nombres), clean(apellidos), normalizedEmail,
            nullable(telefono), hash, estado, colegiatura
        );
    }

    public void update(
        long clinicId, long currentUserId, long userId,
        String nombres, String apellidos, String correo, String telefono,
        String roleValue, String estado, char[] newPassword, String numeroColegiatura
    ) throws SQLException {
        UsuarioDetalle existing = dao.findDetailedById(clinicId, userId)
            .orElseThrow(() -> new IllegalArgumentException("El usuario ya no existe."));

        Role role = parseRole(roleValue);
        String normalizedEmail = validateCommon(nombres, apellidos, correo, telefono, estado);
        String colegiatura = validateColegiatura(role, numeroColegiatura);

        if (currentUserId == userId && (role != Role.ADMINISTRADOR || !"ACTIVO".equals(estado))) {
            throw new IllegalArgumentException("No puedes quitarte el rol de administrador ni desactivar tu propia cuenta.");
        }

        if (existing.role() == Role.ADMINISTRADOR && "ACTIVO".equals(existing.estado())
            && (role != Role.ADMINISTRADOR || !"ACTIVO".equals(estado))
            && dao.countActiveAdministrators(clinicId) <= 1) {
            throw new IllegalArgumentException("Debe quedar al menos un administrador activo en la clínica.");
        }

        if (dao.emailExists(clinicId, normalizedEmail, userId)) {
            throw new IllegalArgumentException("Ya existe otra cuenta con ese correo en la clínica.");
        }

        String hash = null;
        if (newPassword != null && newPassword.length > 0) {
            validatePassword(newPassword, false);
            hash = Passwords.hash(newPassword);
        }

        dao.update(
            clinicId, userId, role, clean(nombres), clean(apellidos), normalizedEmail,
            nullable(telefono), hash, estado, colegiatura
        );
    }

    public void delete(long clinicId, long currentUserId, long userId) throws SQLException {
        if (currentUserId == userId) {
            throw new IllegalArgumentException("No puedes eliminar la cuenta con la que estás conectado.");
        }
        UsuarioDetalle existing = dao.findDetailedById(clinicId, userId)
            .orElseThrow(() -> new IllegalArgumentException("El usuario ya no existe."));
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

    private String validateCommon(
        String nombres, String apellidos, String correo, String telefono, String estado
    ) {
        if (clean(nombres).length() < 2 || clean(nombres).length() > 100) {
            throw new IllegalArgumentException("Los nombres deben tener entre 2 y 100 caracteres.");
        }
        if (clean(apellidos).length() < 2 || clean(apellidos).length() > 100) {
            throw new IllegalArgumentException("Los apellidos deben tener entre 2 y 100 caracteres.");
        }
        String email = clean(correo).toLowerCase(Locale.ROOT);
        if (email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Ingresa un correo electrónico válido.");
        }
        String phone = clean(telefono);
        if (phone.length() > 20 || (!phone.isEmpty() && !phone.matches("[0-9+() -]{6,20}"))) {
            throw new IllegalArgumentException("El teléfono contiene caracteres no válidos.");
        }
        if (!List.of("ACTIVO", "INACTIVO", "BLOQUEADO").contains(estado)) {
            throw new IllegalArgumentException("Estado de usuario no válido.");
        }
        return email;
    }

    private void validatePassword(char[] password, boolean required) {
        if (password == null || password.length == 0) {
            if (required) throw new IllegalArgumentException("La contraseña es obligatoria.");
            return;
        }
        if (password.length < 8 || password.length > 72) {
            throw new IllegalArgumentException("La contraseña debe tener entre 8 y 72 caracteres.");
        }
        boolean upper = false, lower = false, digit = false;
        for (char c : password) {
            upper |= Character.isUpperCase(c);
            lower |= Character.isLowerCase(c);
            digit |= Character.isDigit(c);
        }
        if (!upper || !lower || !digit) {
            throw new IllegalArgumentException("La contraseña debe incluir mayúscula, minúscula y número.");
        }
    }

    private Role parseRole(String value) {
        Role role = Role.fromDatabase(value);
        if (role == null) throw new IllegalArgumentException("Rol no válido.");
        return role;
    }

    private String validateColegiatura(Role role, String value) {
        if (role != Role.ODONTOLOGO) return null;
        String cleaned = clean(value);
        if (cleaned.length() < 3 || cleaned.length() > 50) {
            throw new IllegalArgumentException("Para un odontólogo debes indicar un número de colegiatura válido.");
        }
        return cleaned;
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String nullable(String value) { String v = clean(value); return v.isEmpty() ? null : v; }
}
