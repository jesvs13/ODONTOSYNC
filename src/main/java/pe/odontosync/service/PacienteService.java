package pe.odontosync.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.PacienteCrudDAO;
import pe.odontosync.model.PacienteDetalle;
import pe.odontosync.model.PacienteFicha;

/** Reglas de negocio del CRUD y ficha de pacientes. No contiene SQL ni código HTTP. */
public final class PacienteService {
    private static final List<String> DOCUMENT_TYPES = List.of("DNI", "CE", "PASAPORTE", "OTRO");
    private static final List<String> SEXES = List.of("M", "F", "OTRO");
    private final PacienteCrudDAO dao;

    public PacienteService(PacienteCrudDAO dao) {
        this.dao = dao;
    }

    public boolean canManage(Role role) {
        return role == Role.ADMINISTRADOR || role == Role.RECEPCIONISTA;
    }

    public List<PacienteDetalle> list(User current, String query) throws SQLException {
        String q = clean(query);
        if (q.length() > 80) throw new IllegalArgumentException("La búsqueda es demasiado larga.");
        return current.role() == Role.ODONTOLOGO
            ? dao.searchAssignedToDentist(current.clinicId(), current.id(), q)
            : dao.searchByClinic(current.clinicId(), q);
    }

    public Optional<PacienteDetalle> findVisible(User current, long patientId) throws SQLException {
        Optional<PacienteDetalle> patient = dao.findDetailedById(current.clinicId(), patientId);
        if (patient.isEmpty()) return Optional.empty();
        if (current.role() == Role.ODONTOLOGO
            && !dao.isAssignedToDentist(current.clinicId(), current.id(), patientId)) {
            return Optional.empty();
        }
        return patient;
    }

    public PacienteFicha ficha(User current, long patientId) throws SQLException {
        findVisible(current, patientId)
            .orElseThrow(() -> new IllegalArgumentException("El paciente solicitado no está disponible para tu perfil."));
        boolean includeClinical = current.role() == Role.ODONTOLOGO;
        boolean includePayments = current.role() == Role.ADMINISTRADOR || current.role() == Role.RECEPCIONISTA;
        return dao.loadFicha(current.clinicId(), patientId, includeClinical, includePayments);
    }

    public long create(
        User current, String documentType, String documentNumber, String nombres, String apellidos,
        String birthDate, String sexo, String direccion, String telefono, String correo,
        String ocupacion, String emergencyContact, String emergencyPhone, String estado
    ) throws SQLException {
        requireManager(current);
        Validated v = validate(
            documentType, documentNumber, nombres, apellidos, birthDate, sexo, direccion,
            telefono, correo, ocupacion, emergencyContact, emergencyPhone, estado
        );
        if (dao.documentExists(current.clinicId(), v.documentType(), v.documentNumber(), null)) {
            throw new IllegalArgumentException("Ya existe un paciente con ese tipo y número de documento.");
        }
        return dao.create(
            current.clinicId(), v.documentType(), v.documentNumber(), v.nombres(), v.apellidos(),
            v.birthDate(), v.sexo(), v.direccion(), v.telefono(), v.correo(), v.ocupacion(),
            v.emergencyContact(), v.emergencyPhone(), v.estado()
        );
    }

    public void update(
        User current, long patientId, String documentType, String documentNumber,
        String nombres, String apellidos, String birthDate, String sexo, String direccion,
        String telefono, String correo, String ocupacion, String emergencyContact,
        String emergencyPhone, String estado
    ) throws SQLException {
        requireManager(current);
        dao.findDetailedById(current.clinicId(), patientId)
            .orElseThrow(() -> new IllegalArgumentException("El paciente ya no existe."));
        Validated v = validate(
            documentType, documentNumber, nombres, apellidos, birthDate, sexo, direccion,
            telefono, correo, ocupacion, emergencyContact, emergencyPhone, estado
        );
        if (dao.documentExists(current.clinicId(), v.documentType(), v.documentNumber(), patientId)) {
            throw new IllegalArgumentException("Ese documento pertenece a otro paciente de la clínica.");
        }
        dao.update(
            current.clinicId(), patientId, v.documentType(), v.documentNumber(), v.nombres(), v.apellidos(),
            v.birthDate(), v.sexo(), v.direccion(), v.telefono(), v.correo(), v.ocupacion(),
            v.emergencyContact(), v.emergencyPhone(), v.estado()
        );
    }

    /** Baja lógica: conserva citas, historia clínica, recetas, placas y pagos del paciente. */
    public void changeStatus(User current, long patientId, String estado) throws SQLException {
        requireManager(current);
        if (!List.of("ACTIVO", "INACTIVO").contains(estado)) {
            throw new IllegalArgumentException("Estado de paciente no válido.");
        }
        dao.findDetailedById(current.clinicId(), patientId)
            .orElseThrow(() -> new IllegalArgumentException("El paciente ya no existe."));
        dao.updateStatus(current.clinicId(), patientId, estado);
    }

    private void requireManager(User current) {
        if (!canManage(current.role())) {
            throw new IllegalArgumentException("Tu perfil puede consultar pacientes, pero no modificar sus datos administrativos.");
        }
    }

    private Validated validate(
        String documentType, String documentNumber, String nombres, String apellidos,
        String birthDate, String sexo, String direccion, String telefono, String correo,
        String ocupacion, String emergencyContact, String emergencyPhone, String estado
    ) {
        String type = clean(documentType).toUpperCase(Locale.ROOT);
        if (!DOCUMENT_TYPES.contains(type)) throw new IllegalArgumentException("Tipo de documento no válido.");

        String document = clean(documentNumber).toUpperCase(Locale.ROOT);
        if (document.length() < 4 || document.length() > 20 || !document.matches("[A-Z0-9-]+")) {
            throw new IllegalArgumentException("El número de documento debe tener entre 4 y 20 caracteres válidos.");
        }
        if ("DNI".equals(type) && !document.matches("\\d{8}")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos.");
        }

        String firstNames = clean(nombres);
        String lastNames = clean(apellidos);
        if (firstNames.length() < 2 || firstNames.length() > 100) {
            throw new IllegalArgumentException("Los nombres deben tener entre 2 y 100 caracteres.");
        }
        if (lastNames.length() < 2 || lastNames.length() > 100) {
            throw new IllegalArgumentException("Los apellidos deben tener entre 2 y 100 caracteres.");
        }

        LocalDate birth;
        try { birth = LocalDate.parse(clean(birthDate)); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Ingresa una fecha de nacimiento válida."); }
        LocalDate today = LocalDate.now();
        if (birth.isAfter(today) || birth.isBefore(today.minusYears(120))) {
            throw new IllegalArgumentException("La fecha de nacimiento no es válida.");
        }

        String sex = clean(sexo).toUpperCase(Locale.ROOT);
        if (!sex.isEmpty() && !SEXES.contains(sex)) throw new IllegalArgumentException("Sexo no válido.");

        String phone = optionalPhone(telefono, "teléfono");
        String emergencyPhoneValue = optionalPhone(emergencyPhone, "teléfono de emergencia");
        String email = clean(correo).toLowerCase(Locale.ROOT);
        if (!email.isEmpty() && (email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new IllegalArgumentException("Ingresa un correo electrónico válido.");
        }
        String address = maxNullable(direccion, 255, "La dirección");
        String job = maxNullable(ocupacion, 100, "La ocupación");
        String emergency = maxNullable(emergencyContact, 150, "El contacto de emergencia");
        if (!List.of("ACTIVO", "INACTIVO").contains(estado)) {
            throw new IllegalArgumentException("Estado de paciente no válido.");
        }

        return new Validated(
            type, document, firstNames, lastNames, birth, nullable(sex), address, phone,
            nullable(email), job, emergency, emergencyPhoneValue, estado
        );
    }

    private String optionalPhone(String value, String label) {
        String phone = clean(value);
        if (phone.isEmpty()) return null;
        if (phone.length() > 20 || !phone.matches("[0-9+() -]{6,20}")) {
            throw new IllegalArgumentException("El " + label + " contiene caracteres no válidos.");
        }
        return phone;
    }

    private String maxNullable(String value, int max, String label) {
        String cleaned = clean(value);
        if (cleaned.length() > max) throw new IllegalArgumentException(label + " supera el máximo permitido.");
        return nullable(cleaned);
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String nullable(String value) { String v = clean(value); return v.isEmpty() ? null : v; }

    private record Validated(
        String documentType, String documentNumber, String nombres, String apellidos,
        LocalDate birthDate, String sexo, String direccion, String telefono, String correo,
        String ocupacion, String emergencyContact, String emergencyPhone, String estado
    ) { }
}
