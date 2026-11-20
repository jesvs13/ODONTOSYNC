package pe.odontosync.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.DashboardAnalyticsDAO;
import pe.odontosync.model.DashboardAnalyticsSnapshot;

public final class DashboardAnalyticsService {

    private final DashboardAnalyticsDAO dao;

    public DashboardAnalyticsService(DashboardAnalyticsDAO dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    public DashboardAnalyticsSnapshot load(
        User current,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) throws SQLException {
        requireAdministrator(current);
        validateRange(from, to);
        validateDentistFilter(dentistUserId);

        long clinicId = current.clinicId();

        return new DashboardAnalyticsSnapshot(
            dao.summary(clinicId, from, to, dentistUserId),
            dao.appointmentsTrend(clinicId, from, to, dentistUserId),
            dao.appointmentStatus(clinicId, from, to, dentistUserId),
            dao.incomeTrend(clinicId, from, to, dentistUserId),
            dao.newPatientsTrend(clinicId, from, to),
            dao.attentionsByDentist(clinicId, from, to),
            dao.inventoryStatus(clinicId),
            dao.paymentMethods(clinicId, from, to)
        );
    }

    private void requireAdministrator(User current) {
        if (current == null || current.role() != Role.ADMINISTRADOR) {
            throw new IllegalArgumentException(
                "Solo un administrador puede consultar el dashboard analítico."
            );
        }
        if (current.clinicId() <= 0) {
            throw new IllegalArgumentException(
                "La clínica autenticada no es válida."
            );
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                "Debes indicar un rango de fechas válido."
            );
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                "La fecha inicial no puede ser posterior a la fecha final."
            );
        }
    }

    private void validateDentistFilter(Long dentistUserId) {
        if (dentistUserId != null && dentistUserId <= 0) {
            throw new IllegalArgumentException(
                "El odontólogo seleccionado no es válido."
            );
        }
    }
}
