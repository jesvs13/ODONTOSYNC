package pe.odontosync.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.DashboardAnalyticsDAO;
import pe.odontosync.model.DashboardAnalyticsSnapshot;
import pe.odontosync.model.DashboardSummary;

public final class DashboardAnalyticsService {

    private static final DashboardSummary EMPTY_SUMMARY =
        new DashboardSummary(0, 0, BigDecimal.ZERO, 0.0);

    private final DashboardAnalyticsDAO dao;

    public DashboardAnalyticsService(DashboardAnalyticsDAO dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    /**
     * Contrato completo conservado para pruebas y consumidores que necesiten
     * todas las métricas en una sola carga.
     */
    public DashboardAnalyticsSnapshot load(
        User current,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) throws SQLException {
        QueryContext context = context(
            current,
            from,
            to,
            dentistUserId
        );

        return fullSnapshot(context);
    }

    /**
     * Carga únicamente las métricas utilizadas por la sección visible.
     * Mantiene la clínica exclusivamente desde la sesión autenticada.
     */
    public DashboardAnalyticsSnapshot loadSection(
        User current,
        LocalDate from,
        LocalDate to,
        Long dentistUserId,
        String section
    ) throws SQLException {
        QueryContext context = context(
            current,
            from,
            to,
            dentistUserId
        );

        return switch (normalizeSection(section)) {
            case "resumen" -> summarySnapshot(context);
            case "citas" -> appointmentsSnapshot(context);
            case "pacientes" -> patientsSnapshot(context);
            case "finanzas" -> financeSnapshot(context);
            case "inventario" -> inventorySnapshot(context);
            default -> throw new IllegalArgumentException(
                "La sección del dashboard no es válida."
            );
        };
    }

    private DashboardAnalyticsSnapshot fullSnapshot(QueryContext context)
        throws SQLException {

        return new DashboardAnalyticsSnapshot(
            dao.summary(
                context.clinicId(),
                context.from(),
                context.to(),
                context.dentistUserId()
            ),
            dao.appointmentsTrend(
                context.clinicId(),
                context.from(),
                context.to(),
                context.dentistUserId()
            ),
            dao.appointmentStatus(
                context.clinicId(),
                context.from(),
                context.to(),
                context.dentistUserId()
            ),
            dao.incomeTrend(
                context.clinicId(),
                context.from(),
                context.to(),
                context.dentistUserId()
            ),
            dao.newPatientsTrend(
                context.clinicId(),
                context.from(),
                context.to()
            ),
            dao.attentionsByDentist(
                context.clinicId(),
                context.from(),
                context.to()
            ),
            dao.inventoryStatus(context.clinicId()),
            dao.paymentMethods(
                context.clinicId(),
                context.from(),
                context.to()
            )
        );
    }

    private DashboardAnalyticsSnapshot summarySnapshot(QueryContext context)
        throws SQLException {

        return snapshot(
            dao.summary(
                context.clinicId(),
                context.from(),
                context.to(),
                null
            )
        );
    }

    private DashboardAnalyticsSnapshot appointmentsSnapshot(QueryContext context)
        throws SQLException {

        return new DashboardAnalyticsSnapshot(
            dao.summary(
                context.clinicId(),
                context.from(),
                context.to(),
                context.dentistUserId()
            ),
            dao.appointmentsTrend(
                context.clinicId(),
                context.from(),
                context.to(),
                context.dentistUserId()
            ),
            dao.appointmentStatus(
                context.clinicId(),
                context.from(),
                context.to(),
                context.dentistUserId()
            ),
            List.of(),
            List.of(),
            dao.attentionsByDentist(
                context.clinicId(),
                context.from(),
                context.to()
            ),
            List.of(),
            List.of()
        );
    }

    private DashboardAnalyticsSnapshot patientsSnapshot(QueryContext context)
        throws SQLException {

        return new DashboardAnalyticsSnapshot(
            dao.summary(
                context.clinicId(),
                context.from(),
                context.to(),
                null
            ),
            List.of(),
            List.of(),
            List.of(),
            dao.newPatientsTrend(
                context.clinicId(),
                context.from(),
                context.to()
            ),
            List.of(),
            List.of(),
            List.of()
        );
    }

    private DashboardAnalyticsSnapshot financeSnapshot(QueryContext context)
        throws SQLException {

        return new DashboardAnalyticsSnapshot(
            dao.summary(
                context.clinicId(),
                context.from(),
                context.to(),
                context.dentistUserId()
            ),
            List.of(),
            List.of(),
            dao.incomeTrend(
                context.clinicId(),
                context.from(),
                context.to(),
                context.dentistUserId()
            ),
            List.of(),
            List.of(),
            List.of(),
            dao.paymentMethods(
                context.clinicId(),
                context.from(),
                context.to()
            )
        );
    }

    private DashboardAnalyticsSnapshot inventorySnapshot(QueryContext context)
        throws SQLException {

        return new DashboardAnalyticsSnapshot(
            EMPTY_SUMMARY,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            dao.inventoryStatus(context.clinicId()),
            List.of()
        );
    }

    private DashboardAnalyticsSnapshot snapshot(DashboardSummary summary) {
        return new DashboardAnalyticsSnapshot(
            summary,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
    }

    private QueryContext context(
        User current,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) {
        requireAdministrator(current);
        validateRange(from, to);
        validateDentistFilter(dentistUserId);

        return new QueryContext(
            current.clinicId(),
            from,
            to,
            dentistUserId
        );
    }

    private String normalizeSection(String section) {
        return section == null ? "" : section.trim().toLowerCase(
            java.util.Locale.ROOT
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

    private record QueryContext(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) { }
}
