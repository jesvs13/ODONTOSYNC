package pe.odontosync.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.OdontologoCrudDAO;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoDetalle;
import pe.odontosync.security.Passwords;

/** Reglas de negocio del CRUD de odontólogos. */
public final class OdontologoService {
    private final OdontologoCrudDAO dao;

    public OdontologoService(OdontologoCrudDAO dao) {
        this.dao = dao;
    }

    public boolean canManage(Role role) {
        return role == Role.ADMINISTRADOR;
    }

    public List<OdontologoDetalle> list(User current, String query) throws SQLException {
        if (current.role() != Role.ADMINISTRADOR && current.role() != Role.RECEPCIONISTA) {
            throw new IllegalArgumentException("Tu perfil no puede consultar el directorio de odontólogos.");
        }
        String q = clean(query);
        if (q.length() > 80) throw new IllegalArgumentException("La búsqueda es demasiado larga.");
        return dao.searchByClinic(current.clinicId(), q);
    }

    public Optional<OdontologoDetalle> find(User current, long dentistId) throws SQLException {
        if (current.role() != Role.ADMINISTRADOR && current.role() != Role.RECEPCIONISTA) {
            return Optional.empty();
        }
        return dao.findDetailedById(current.clinicId(), dentistId);
    }

    public List<EspecialidadOpcion> specialties() throws SQLException {
        return dao.findActiveSpecialties();
    }

    public long create(
        User current, String nombres, String apellidos, String correo, String telefono,
        char[] password, String colegiatura, String fechaIngreso, String estado,
        List<Long> especialidadIds
    ) throws SQLException {
        requireAdmin(current);
        Validated v = validate(nombres, apellidos, correo, telefono, colegiatura, fechaIngreso, estado, especialidadIds);
        validatePassword(password, true);
        if (dao.emailExists(current.clinicId(), v.correo(), null)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo en la clínica.");
        }
        if (dao.licenseExists(current.clinicId(), v.colegiatura(), null)) {
            throw new IllegalArgumentException("Ya existe un odontólogo con ese número de colegiatura.");
        }
        validateSpecialties(v.especialidades());
        return dao.create(
            current.clinicId(), v.nombres(), v.apellidos(), v.correo(), v.telefono(),
            Passwords.hash(password), v.colegiatura(), v.fechaIngreso(), v.estado(), v.especialidades()
        );
    }

    public void update(
        User current, long dentistId, String nombres, String apellidos, String correo,
        String telefono, char[] newPassword, String colegiatura, String fechaIngreso,
        String estado, List<Long> especialidadIds
    ) throws SQLException {
        requireAdmin(current);
        OdontologoDetalle existing = dao.findDetailedById(current.clinicId(), dentistId)
            .orElseThrow(() -> new IllegalArgumentException("El odontólogo ya no existe."));
        Validated v = validate(nombres, apellidos, correo, telefono, colegiatura, fechaIngreso, estado, especialidadIds);
        if (dao.emailExists(current.clinicId(), v.correo(), existing.idUsuario())) {
            throw new IllegalArgumentException("Ya existe otra cuenta con ese correo en la clínica.");
        }
        if (dao.licenseExists(current.clinicId(), v.colegiatura(), dentistId)) {
            throw new IllegalArgumentException("Ya existe otro odontólogo con ese número de colegiatura.");
        }
        validateSpecialties(v.especialidades());
        String hash = null;
        if (newPassword != null && newPassword.length > 0) {
            validatePassword(newPassword, false);
            hash = Passwords.hash(newPassword);
        }
        dao.update(
            current.clinicId(), dentistId, v.nombres(), v.apellidos(), v.correo(),
            v.telefono(), hash, v.colegiatura(), v.fechaIngreso(), v.estado(), v.especialidades()
        );
    }

    public void changeStatus(User current, long dentistId, String estado) throws SQLException {
        requireAdmin(current);
        String normalized = normalizeStatus(estado);
        dao.findDetailedById(current.clinicId(), dentistId)
            .orElseThrow(() -> new IllegalArgumentException("El odontólogo ya no existe."));
        dao.updateStatus(current.clinicId(), dentistId, normalized);
    }

    private Validated validate(
        String nombres, String apellidos, String correo, String telefono, String colegiatura,
        String fechaIngreso, String estado, List<Long> especialidadIds
    ) {
        String n = clean(nombres);
        String a = clean(apellidos);
        String email = clean(correo).toLowerCase(Locale.ROOT);
        String phone = clean(telefono);
        String license = clean(colegiatura).toUpperCase(Locale.ROOT);
        if (n.length() < 2 || n.length() > 100) throw new IllegalArgumentException("Los nombres deben tener entre 2 y 100 caracteres.");
        if (a.length() < 2 || a.length() > 100) throw new IllegalArgumentException("Los apellidos deben tener entre 2 y 100 caracteres.");
        if (email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Ingresa un correo electrónico válido.");
        }
        if (phone.length() > 20 || (!phone.isBlank() && !phone.matches("[0-9+() -]{6,20}"))) {
            throw new IllegalArgumentException("El teléfono contiene caracteres no válidos.");
        }
        if (license.length() < 3 || license.length() > 50 || !license.matches("[A-Z0-9./ -]+")) {
            throw new IllegalArgumentException("Ingresa un número de colegiatura válido.");
        }
        LocalDate admission = parseDate(fechaIngreso);
        if (admission.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de ingreso no puede estar en el futuro.");
        }
        String st = normalizeStatus(estado);
        List<Long> specialties = especialidadIds == null ? List.of() : especialidadIds.stream().distinct().toList();
        if (specialties.isEmpty()) throw new IllegalArgumentException("Selecciona al menos una especialidad.");
        if (specialties.size() > 8 || specialties.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("La selección de especialidades no es válida.");
        }
        return new Validated(n, a, email, phone.isBlank() ? null : phone, license, admission, st, specialties);
    }

    private void validateSpecialties(List<Long> requested) throws SQLException {
        Set<Long> active = new HashSet<>();
        for (EspecialidadOpcion option : dao.findActiveSpecialties()) active.add(option.id());
        if (!active.containsAll(requested)) {
            throw new IllegalArgumentException("Una de las especialidades seleccionadas ya no está disponible.");
        }
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

    private LocalDate parseDate(String raw) {
        String value = clean(raw);
        if (value.isBlank()) return LocalDate.now();
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Fecha de ingreso no válida."); }
    }

    private String normalizeStatus(String status) {
        String st = clean(status).toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVO", "INACTIVO").contains(st)) throw new IllegalArgumentException("Estado de odontólogo no válido.");
        return st;
    }

    private void requireAdmin(User current) {
        if (current == null || current.role() != Role.ADMINISTRADOR) {
            throw new IllegalArgumentException("Solo Administración puede modificar odontólogos.");
        }
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }

    private record Validated(
        String nombres, String apellidos, String correo, String telefono, String colegiatura,
        LocalDate fechaIngreso, String estado, List<Long> especialidades
    ) { }
}
