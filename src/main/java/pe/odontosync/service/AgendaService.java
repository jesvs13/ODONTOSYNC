package pe.odontosync.service;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.AgendaCrudDAO;
import pe.odontosync.model.CitaDetalle;
import pe.odontosync.model.CitaEstadoOpcion;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoOpcion;
import pe.odontosync.model.PacienteOpcion;

/** Reglas de negocio de Agenda/Citas. */
public final class AgendaService {
    private final AgendaCrudDAO dao;

    public AgendaService(AgendaCrudDAO dao) {
        this.dao = dao;
    }

    public boolean canSchedule(Role role) {
        return role == Role.ADMINISTRADOR || role == Role.RECEPCIONISTA;
    }

    public List<CitaDetalle> list(
        User current, String fromValue, String toValue, String status, String query
    ) throws SQLException {
        LocalDate from = parseOptionalDate(fromValue, "fecha desde");
        LocalDate to = parseOptionalDate(toValue, "fecha hasta");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        String normalizedStatus = normalizeFilterStatus(status);
        Long dentistUserId = current.role() == Role.ODONTOLOGO ? current.id() : null;
        return dao.searchDetailed(current.clinicId(), dentistUserId, from, to, normalizedStatus, clean(query, 80));
    }

    public Optional<CitaDetalle> findVisible(User current, long appointmentId) throws SQLException {
        Optional<CitaDetalle> found = dao.findDetailedById(current.clinicId(), appointmentId);
        if (found.isEmpty()) return Optional.empty();
        if (current.role() == Role.ODONTOLOGO
            && !dao.dentistBelongsToUser(current.clinicId(), found.get().idOdontologo(), current.id())) {
            return Optional.empty();
        }
        return found;
    }

    public List<PacienteOpcion> patients(User current) throws SQLException {
        return canSchedule(current.role()) ? dao.findActivePatients(current.clinicId()) : List.of();
    }

    public List<OdontologoOpcion> dentists(User current) throws SQLException {
        return canSchedule(current.role()) ? dao.findActiveDentists(current.clinicId()) : List.of();
    }

    public List<EspecialidadOpcion> specialties(User current) throws SQLException {
        return canSchedule(current.role()) ? dao.findActiveSpecialties() : List.of();
    }

    public Map<Long, List<Long>> dentistSpecialtyIds(User current) throws SQLException {
        return canSchedule(current.role()) ? dao.findActiveDentistSpecialtyIds(current.clinicId()) : Map.of();
    }

    public List<CitaEstadoOpcion> states() throws SQLException {
        return dao.findStates();
    }

    public long create(
        User current, long patientId, long dentistId, String specialtyValue,
        String dateValue, String startValue, String endValue, String reason, String notes
    ) throws SQLException {
        requireScheduler(current);
        AppointmentData data = validateScheduleData(
            current, patientId, dentistId, specialtyValue, dateValue, startValue, endValue, reason, notes, null
        );
        long pendingId = stateId("PENDIENTE");
        return dao.create(
            current.clinicId(), patientId, dentistId, data.specialtyId(), data.scheduleId(), pendingId,
            data.date(), data.start(), data.end(), data.reason(), data.notes(), current.id()
        );
    }

    public void update(
        User current, long appointmentId, long patientId, long dentistId, String specialtyValue,
        String dateValue, String startValue, String endValue, String reason, String notes
    ) throws SQLException {
        requireScheduler(current);
        CitaDetalle existing = dao.findDetailedById(current.clinicId(), appointmentId)
            .orElseThrow(() -> new IllegalArgumentException("La cita ya no existe."));
        if (!List.of("PENDIENTE", "CONFIRMADA").contains(existing.estado())) {
            throw new IllegalArgumentException("Solo se pueden reprogramar citas pendientes o confirmadas.");
        }
        AppointmentData data = validateScheduleData(
            current, patientId, dentistId, specialtyValue, dateValue, startValue, endValue, reason, notes, appointmentId
        );
        dao.update(
            current.clinicId(), appointmentId, patientId, dentistId, data.specialtyId(), data.scheduleId(),
            data.date(), data.start(), data.end(), data.reason(), data.notes()
        );
    }

    public void changeStatus(User current, long appointmentId, String targetStatus) throws SQLException {
        CitaDetalle existing = findVisible(current, appointmentId)
            .orElseThrow(() -> new IllegalArgumentException("La cita solicitada no existe o no está disponible para tu perfil."));
        String target = normalizeStatus(targetStatus);
        if (existing.estado().equals(target)) return;
        if (!transitionAllowed(current.role(), existing.estado(), target)) {
            throw new IllegalArgumentException("El cambio de estado de " + existing.estado() + " a " + target + " no está permitido para tu perfil.");
        }
        dao.updateStatus(current.clinicId(), appointmentId, stateId(target));
    }

    private AppointmentData validateScheduleData(
        User current, long patientId, long dentistId, String specialtyValue,
        String dateValue, String startValue, String endValue, String reason, String notes,
        Long excludeAppointmentId
    ) throws SQLException {
        if (patientId <= 0 || !dao.patientIsActive(current.clinicId(), patientId)) {
            throw new IllegalArgumentException("Selecciona un paciente activo de tu clínica.");
        }
        if (dentistId <= 0 || !dao.dentistIsActive(current.clinicId(), dentistId)) {
            throw new IllegalArgumentException("Selecciona un odontólogo activo de tu clínica.");
        }
        LocalDate date = parseRequiredDate(dateValue, "fecha de la cita");
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede programar una cita en una fecha pasada.");
        }
        LocalTime start = parseTime(startValue, "hora de inicio");
        LocalTime end = parseTime(endValue, "hora de fin");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 15 || minutes > 240) {
            throw new IllegalArgumentException("La cita debe durar entre 15 minutos y 4 horas.");
        }

        Long specialtyId = parseOptionalId(specialtyValue, "especialidad");
        if (specialtyId != null && !dao.specialtyAssignedToDentist(dentistId, specialtyId)) {
            throw new IllegalArgumentException("La especialidad seleccionada no está asignada al odontólogo.");
        }

        Long scheduleId = dao.findCoveringSchedule(
            current.clinicId(), dentistId, date.getDayOfWeek().getValue(), start, end
        ).orElseThrow(() -> new IllegalArgumentException(
            "El odontólogo no tiene un horario activo que cubra esa fecha y rango de horas."
        ));

        if (dao.isBlocked(current.clinicId(), dentistId, date, start, end)) {
            throw new IllegalArgumentException("La agenda del odontólogo está bloqueada en ese horario.");
        }
        if (dao.dentistOverlaps(current.clinicId(), dentistId, date, start, end, excludeAppointmentId)) {
            throw new IllegalArgumentException("Horario ocupado: el odontólogo ya tiene otra cita en ese rango.");
        }
        if (dao.patientOverlaps(current.clinicId(), patientId, date, start, end, excludeAppointmentId)) {
            throw new IllegalArgumentException("El paciente ya tiene otra cita que se cruza con ese horario.");
        }

        String cleanReason = clean(reason, 255);
        if (cleanReason.length() < 3) {
            throw new IllegalArgumentException("Indica el motivo de la cita.");
        }
        String cleanNotes = clean(notes, 2000);
        return new AppointmentData(date, start, end, specialtyId, scheduleId, cleanReason, cleanNotes);
    }

    private boolean transitionAllowed(Role role, String from, String to) {
        if (role == Role.ADMINISTRADOR) {
            return switch (from) {
                case "PENDIENTE" -> List.of("CONFIRMADA", "CANCELADA").contains(to);
                case "CONFIRMADA" -> List.of("CANCELADA", "NO_ASISTIO").contains(to);
                default -> false;
            };
        }
        if (role == Role.RECEPCIONISTA) {
            return switch (from) {
                case "PENDIENTE" -> List.of("CONFIRMADA", "CANCELADA").contains(to);
                case "CONFIRMADA" -> List.of("CANCELADA", "NO_ASISTIO").contains(to);
                default -> false;
            };
        }
        if (role == Role.ODONTOLOGO) {
            return "CONFIRMADA".equals(from) && "NO_ASISTIO".equals(to);
        }
        return false;
    }

    private long stateId(String name) throws SQLException {
        return dao.findStates().stream()
            .filter(x -> name.equals(x.nombre()))
            .mapToLong(CitaEstadoOpcion::id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("El estado " + name + " no existe en MySQL."));
    }

    private String normalizeFilterStatus(String raw) {
        String st = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (st.isEmpty()) return "";
        if (!List.of("PENDIENTE", "CONFIRMADA", "ATENDIDA", "CANCELADA", "NO_ASISTIO").contains(st)) {
            throw new IllegalArgumentException("Filtro de estado no válido.");
        }
        return st;
    }

    private String normalizeStatus(String raw) {
        String st = normalizeFilterStatus(raw);
        if (st.isEmpty()) throw new IllegalArgumentException("Selecciona un estado válido.");
        return st;
    }

    private LocalDate parseRequiredDate(String raw, String label) {
        LocalDate date = parseOptionalDate(raw, label);
        if (date == null) throw new IllegalArgumentException("La " + label + " es obligatoria.");
        return date;
    }

    private LocalDate parseOptionalDate(String raw, String label) {
        if (raw == null || raw.isBlank()) return null;
        try { return LocalDate.parse(raw.trim()); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("La " + label + " no es válida."); }
    }

    private LocalTime parseTime(String raw, String label) {
        try { return LocalTime.parse(raw == null ? "" : raw.trim()); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("La " + label + " no es válida."); }
    }

    private Long parseOptionalId(String raw, String label) {
        if (raw == null || raw.isBlank()) return null;
        try {
            long id = Long.parseLong(raw.trim());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("La " + label + " no es válida.");
        }
    }

    private String clean(String raw, int max) {
        String value = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        return value.length() > max ? value.substring(0, max) : value;
    }

    private void requireScheduler(User current) {
        if (current == null || !canSchedule(current.role())) {
            throw new IllegalArgumentException("Solo Administración y Recepción pueden programar o reprogramar citas.");
        }
    }

    private record AppointmentData(
        LocalDate date, LocalTime start, LocalTime end, Long specialtyId, Long scheduleId,
        String reason, String notes
    ) { }
}
