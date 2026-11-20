package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcDashboardAnalyticsDAO;
import pe.odontosync.dao.JdbcOdontologoDAO;
import pe.odontosync.model.OdontologoDetalle;
import pe.odontosync.security.AppLog;
import pe.odontosync.service.DashboardAnalyticsService;
import pe.odontosync.service.OdontologoService;

@WebServlet("/modulo/dashboard")
public final class DashboardAnalyticsServlet extends HttpServlet {
    private final DashboardAnalyticsService dashboardService =
        new DashboardAnalyticsService(new JdbcDashboardAnalyticsDAO());
    private final OdontologoService dentistService =
        new OdontologoService(new JdbcOdontologoDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        User current = (User) req.getSession(false).getAttribute("user");
        if (current == null || current.role() != Role.ADMINISTRADOR) {
            res.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "El dashboard analítico está disponible solo para Administración."
            );
            return;
        }

        String section = section(req.getParameter("section"));
        req.setAttribute("user", current);
        req.setAttribute("active", "dashboard");
        req.setAttribute("section", section);

        try {
            PeriodRange range = periodRange(req);
            boolean dentistFilterEnabled = usesDentistFilter(section);

            List<OdontologoDetalle> dentists = dentistFilterEnabled
                ? dentistService.list(current, "")
                    .stream()
                    .filter(d -> "ACTIVO".equalsIgnoreCase(d.estado()))
                    .toList()
                : List.of();

            Long dentistUserId = dentistFilterEnabled
                ? parseDentist(req.getParameter("odontologo"))
                : null;

            if (dentistUserId != null && dentists.stream()
                .noneMatch(d -> d.idUsuario() == dentistUserId)) {
                throw new IllegalArgumentException(
                    "El odontólogo seleccionado no pertenece a tu clínica."
                );
            }

            req.setAttribute("period", range.period());
            req.setAttribute("from", range.from());
            req.setAttribute("to", range.to());
            req.setAttribute("dentistUserId", dentistUserId);
            req.setAttribute("dentists", dentists);
            req.setAttribute(
                "dashboard",
                dashboardService.loadSection(
                    current,
                    range.from(),
                    range.to(),
                    dentistUserId,
                    section
                )
            );

            req.getRequestDispatcher(
                "/WEB-INF/views/dashboard.jsp"
            ).forward(req, res);
        } catch (IllegalArgumentException ex) {
            req.setAttribute("dashboardError", ex.getMessage());
            req.setAttribute("dentists", List.of());
            req.getRequestDispatcher(
                "/WEB-INF/views/dashboard.jsp"
            ).forward(req, res);
        } catch (Exception ex) {
            AppLog.error("Fallo en dashboard analítico", ex);
            res.sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "No se pudo consultar el dashboard de la clínica."
            );
        }
    }

    private String section(String raw) {
        return switch (clean(raw)) {
            case "", "resumen" -> "resumen";
            case "citas" -> "citas";
            case "pacientes" -> "pacientes";
            case "finanzas" -> "finanzas";
            case "inventario" -> "inventario";
            default -> throw new IllegalArgumentException(
                "La sección del dashboard no es válida."
            );
        };
    }

    private boolean usesDentistFilter(String section) {
        return "citas".equals(section) || "finanzas".equals(section);
    }

    private PeriodRange periodRange(HttpServletRequest req) {
        String period = clean(req.getParameter("period"));
        LocalDate today = LocalDate.now();

        return switch (period) {
            case "7d" -> new PeriodRange(
                "7d",
                today.minusDays(6),
                today
            );
            case "month" -> new PeriodRange(
                "month",
                today.with(TemporalAdjusters.firstDayOfMonth()),
                today
            );
            case "year" -> new PeriodRange(
                "year",
                LocalDate.of(today.getYear(), 1, 1),
                today
            );
            case "custom" -> new PeriodRange(
                "custom",
                parseDate(req.getParameter("from"), "Fecha inicial no válida."),
                parseDate(req.getParameter("to"), "Fecha final no válida.")
            );
            case "", "30d" -> new PeriodRange(
                "30d",
                today.minusDays(29),
                today
            );
            default -> throw new IllegalArgumentException(
                "El período seleccionado no es válido."
            );
        };
    }

    private Long parseDentist(String raw) {
        String value = clean(raw);
        if (value.isEmpty()) {
            return null;
        }
        try {
            long id = Long.parseLong(value);
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                "El odontólogo seleccionado no es válido."
            );
        }
    }

    private LocalDate parseDate(String raw, String message) {
        try {
            return LocalDate.parse(clean(raw));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private String clean(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private record PeriodRange(
        String period,
        LocalDate from,
        LocalDate to
    ) { }
}
