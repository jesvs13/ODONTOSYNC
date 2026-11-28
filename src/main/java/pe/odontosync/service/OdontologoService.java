package pe.odontosync.service;

import java.sql.SQLException;
import pe.odontosync.security.DniPolicy;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.OdontologoCrudDAO;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoDetalle;

/** Reglas de negocio del CRUD de odontólogos. */
public final class OdontologoService {
    private final OdontologoCrudDAO dao;
    private final CorreoInstitucionalService emails;
    private final TemporaryAccountCredentials credentials = new TemporaryAccountCredentials();

    public OdontologoService(OdontologoCrudDAO dao) {
        this.dao = dao;
        this.emails = new CorreoInstitucionalService(dao::emailExistsGlobally);
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

    public String suggestEmail(User current, String nombres, String primerApellido, String segundoApellido)
        throws SQLException {
        requireAdmin(current);
        return emails.generate(nombres, primerApellido, segundoApellido);
    }

    public CreatedAccount create(
        User current, String nombres, String primerApellido, String segundoApellido, String telefono,
        String dni, String colegiatura, String fechaIngreso, String estado,
        List<Long> especialidadIds
    ) throws SQLException {
        requireAdmin(current);
        String first = normalizePersonName(primerApellido);
        if (first.isEmpty()) throw new IllegalArgumentException("Ingresa el primer apellido.");
        String apellidos = (first + " " + normalizePersonName(segundoApellido)).trim();
        Validated v = validate(nombres, apellidos, telefono, colegiatura, fechaIngreso, estado, especialidadIds);
        String normalizedDni = DniPolicy.normalize(dni);
        if (dao.licenseExists(current.clinicId(), v.colegiatura(), null)) {
            throw new IllegalArgumentException("Ya existe un odontólogo con ese número de colegiatura.");
        }
        validateSpecialties(v.especialidades());
        return credentials.create(emails, v.nombres(), first, segundoApellido,
            (email, hash) -> dao.create(current.clinicId(), v.nombres(), v.apellidos(), email, v.telefono(),
                hash, v.colegiatura(), v.fechaIngreso(), v.estado(), v.especialidades(), normalizedDni));
    }

    public void update(
        User current, long dentistId, String nombres, String apellidos, String correo,
        String telefono, char[] newPassword, String colegiatura, String fechaIngreso,
        String estado, List<Long> especialidadIds
    ) throws SQLException {
        requireAdmin(current);
        OdontologoDetalle existing = dao.findDetailedById(current.clinicId(), dentistId)
            .orElseThrow(() -> new IllegalArgumentException("El odontólogo ya no existe."));
        if (newPassword != null && newPassword.length > 0) {
            throw new IllegalArgumentException("Gestiona la contraseña desde el módulo Usuarios.");
        }
        Validated v = validate(nombres, apellidos, telefono, colegiatura, fechaIngreso, estado, especialidadIds);
        if (dao.licenseExists(current.clinicId(), v.colegiatura(), dentistId)) {
            throw new IllegalArgumentException("Ya existe otro odontólogo con ese número de colegiatura.");
        }
        validateSpecialties(v.especialidades());
        dao.update(
            current.clinicId(), dentistId, v.nombres(), v.apellidos(), existing.correo(),
            v.telefono(), null, v.colegiatura(), v.fechaIngreso(), v.estado(), v.especialidades()
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
        String nombres, String apellidos, String telefono, String colegiatura,
        String fechaIngreso, String estado, List<Long> especialidadIds
    ) {
        String n = normalizePersonName(nombres);
        String a = normalizePersonName(apellidos);
        String phone = clean(telefono);
        String license = clean(colegiatura).toUpperCase(Locale.ROOT);
        if (n.length() < 2 || n.length() > 100) throw new IllegalArgumentException("Los nombres deben tener entre 2 y 100 caracteres.");
        if (a.length() < 2 || a.length() > 100) throw new IllegalArgumentException("Los apellidos deben tener entre 2 y 100 caracteres.");
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
        return new Validated(n, a, phone.isBlank() ? null : phone, license, admission, st, specialties);
    }

    private void validateSpecialties(List<Long> requested) throws SQLException {
        Set<Long> active = new HashSet<>();
        for (EspecialidadOpcion option : dao.findActiveSpecialties()) active.add(option.id());
        if (!active.containsAll(requested)) {
            throw new IllegalArgumentException("Una de las especialidades seleccionadas ya no está disponible.");
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

    private String normalizePersonName(String value) {
        return StringUtils.normalizeSpace(StringUtils.trimToEmpty(value));
    }

    private String clean(String value) { return StringUtils.trimToEmpty(value); }

    private record Validated(
        String nombres, String apellidos, String telefono, String colegiatura,
        LocalDate fechaIngreso, String estado, List<Long> especialidades
    ) { }
}
