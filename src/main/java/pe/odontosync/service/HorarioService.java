package pe.odontosync.service;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.HorarioCrudDAO;
import pe.odontosync.model.HorarioDetalle;
import pe.odontosync.model.OdontologoOpcion;

/** Reglas de negocio para disponibilidad semanal de odontólogos. */
public final class HorarioService {
    private final HorarioCrudDAO dao;

    public HorarioService(HorarioCrudDAO dao) {
        this.dao = dao;
    }

    public boolean canManage(Role role) {
        return role == Role.ADMINISTRADOR;
    }

    public List<HorarioDetalle> list(User current) throws SQLException {
        return current.role() == Role.ODONTOLOGO
            ? dao.findDetailedByDentistUser(current.clinicId(), current.id())
            : dao.findDetailedByClinic(current.clinicId());
    }

    public List<OdontologoOpcion> dentists(User current) throws SQLException {
        if (current.role() != Role.ADMINISTRADOR && current.role() != Role.RECEPCIONISTA) return List.of();
        return dao.findDentistsByClinic(current.clinicId());
    }

    public Optional<HorarioDetalle> find(User current, long scheduleId) throws SQLException {
        if (!canManage(current.role())) return Optional.empty();
        return dao.findDetailedById(current.clinicId(), scheduleId);
    }

    public long create(
        User current, long dentistId, String dayValue, String startValue, String endValue, String status
    ) throws SQLException {
        requireAdmin(current);
        ScheduleData data = validate(current, dentistId, dayValue, startValue, endValue, status, null);
        return dao.create(current.clinicId(), dentistId, data.day(), data.start(), data.end(), data.status());
    }

    public void update(
        User current, long scheduleId, long dentistId, String dayValue,
        String startValue, String endValue, String status
    ) throws SQLException {
        requireAdmin(current);
        dao.findDetailedById(current.clinicId(), scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("El horario ya no existe."));
        ScheduleData data = validate(current, dentistId, dayValue, startValue, endValue, status, scheduleId);
        dao.update(current.clinicId(), scheduleId, dentistId, data.day(), data.start(), data.end(), data.status());
    }

    public void changeStatus(User current, long scheduleId, String status) throws SQLException {
        requireAdmin(current);
        HorarioDetalle existing = dao.findDetailedById(current.clinicId(), scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("El horario ya no existe."));
        String normalized = normalizeStatus(status);
        if ("ACTIVO".equals(normalized)) {
            if (!dao.dentistIsActive(current.clinicId(), existing.idOdontologo())) {
                throw new IllegalArgumentException("No puedes activar un horario de un odontólogo inactivo.");
            }
            if (dao.overlaps(current.clinicId(), existing.idOdontologo(), existing.diaSemana(),
                existing.horaInicio(), existing.horaFin(), scheduleId)) {
                throw new IllegalArgumentException("Ese bloque se cruza con otro horario activo del odontólogo.");
            }
        }
        dao.updateStatus(current.clinicId(), scheduleId, normalized);
    }

    private ScheduleData validate(
        User current, long dentistId, String dayValue, String startValue,
        String endValue, String status, Long excludeId
    ) throws SQLException {
        if (dentistId <= 0 || !dao.dentistBelongsToClinic(current.clinicId(), dentistId)) {
            throw new IllegalArgumentException("Selecciona un odontólogo válido de tu clínica.");
        }
        int day;
        try { day = Integer.parseInt(dayValue); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Día de atención no válido."); }
        if (day < 1 || day > 7) throw new IllegalArgumentException("Día de atención no válido.");
        LocalTime start = parseTime(startValue, "hora de inicio");
        LocalTime end = parseTime(endValue, "hora de fin");
        if (!start.isBefore(end)) throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        String st = normalizeStatus(status);
        if ("ACTIVO".equals(st)) {
            if (!dao.dentistIsActive(current.clinicId(), dentistId)) {
                throw new IllegalArgumentException("No puedes asignar un horario activo a un odontólogo inactivo.");
            }
            if (dao.overlaps(current.clinicId(), dentistId, day, start, end, excludeId)) {
                throw new IllegalArgumentException("El horario se cruza con otro bloque activo del odontólogo.");
            }
        }
        return new ScheduleData(day, start, end, st);
    }

    private LocalTime parseTime(String raw, String label) {
        try { return LocalTime.parse(raw == null ? "" : raw.trim()); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("La " + label + " no es válida."); }
    }

    private String normalizeStatus(String raw) {
        String st = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVO", "INACTIVO").contains(st)) throw new IllegalArgumentException("Estado de horario no válido.");
        return st;
    }

    private void requireAdmin(User current) {
        if (current == null || current.role() != Role.ADMINISTRADOR) {
            throw new IllegalArgumentException("Solo Administración puede modificar horarios.");
        }
    }

    private record ScheduleData(int day, LocalTime start, LocalTime end, String status) { }
}
