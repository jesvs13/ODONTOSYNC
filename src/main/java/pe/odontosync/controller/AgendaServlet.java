package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import org.apache.commons.lang3.StringUtils;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcAgendaDAO;
import pe.odontosync.model.CitaDetalle;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.AgendaService;

/** Agenda funcional: alta, reprogramación, estados, filtros y validación de disponibilidad. */
@WebServlet("/agenda")
public final class AgendaServlet extends HttpServlet {
    private final AgendaService service = new AgendaService(new JdbcAgendaDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentUser(req, res);
        if (current == null) return;

        try {
            boolean hasFilters = req.getParameterMap().keySet().stream()
                .anyMatch(k -> k.equals("desde") || k.equals("hasta") || k.equals("estado") || k.equals("q"));
            String from = hasFilters ? value(req, "desde") : LocalDate.now().toString();
            String to = hasFilters ? value(req, "hasta") : LocalDate.now().plusDays(30).toString();
            String status = value(req, "estado");
            String query = value(req, "q");

            req.setAttribute("user", current);
            req.setAttribute("active", "agenda");
            req.setAttribute("canScheduleAgenda", service.canSchedule(current.role()));
            req.setAttribute("agenda", service.list(current, from, to, status, query));
            req.setAttribute("agendaStates", service.states());
            req.setAttribute("filterFrom", from);
            req.setAttribute("filterTo", to);
            req.setAttribute("filterStatus", status);
            req.setAttribute("filterQuery", query);

            if (service.canSchedule(current.role())) {
                req.setAttribute("agendaPatients", service.patients(current));
                req.setAttribute("agendaDentists", service.dentists(current));
                req.setAttribute("agendaSpecialties", service.specialties(current));
                req.setAttribute("agendaDentistSpecialtyIds", service.dentistSpecialtyIds(current));
                String patient = value(req, "paciente");
                if (!patient.isBlank()) req.setAttribute("preselectedPatientId", parseId(patient, "paciente"));
            }

            String edit = value(req, "editar");
            if (!edit.isBlank()) {
                if (!service.canSchedule(current.role())) {
                    res.sendError(403, "Tu perfil puede consultar la agenda, pero no reprogramar citas.");
                    return;
                }
                CitaDetalle selected = service.findVisible(current, parseId(edit, "cita"))
                    .orElseThrow(() -> new IllegalArgumentException("La cita solicitada no existe."));
                if (!java.util.List.of("PENDIENTE", "CONFIRMADA").contains(selected.estado())) {
                    throw new IllegalArgumentException("Solo se pueden reprogramar citas pendientes o confirmadas.");
                }
                req.setAttribute("editAppointment", selected);
            }

            HttpSession session = req.getSession(false);
            if (session != null) {
                req.setAttribute("success", session.getAttribute("agendaSuccess"));
                req.setAttribute("error", session.getAttribute("agendaError"));
                session.removeAttribute("agendaSuccess");
                session.removeAttribute("agendaError");
            }
            req.getRequestDispatcher("/WEB-INF/views/agenda.jsp").forward(req, res);
        } catch (IllegalArgumentException ex) {
            flash(req, "agendaError", ex.getMessage());
            res.sendRedirect(req.getContextPath() + "/agenda");
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando agenda", ex);
            res.sendError(503, "No se pudo consultar MySQL.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentUser(req, res);
        if (current == null) return;
        req.setCharacterEncoding("UTF-8");
        if (!validCsrf(req)) {
            res.sendError(403, "Solicitud rechazada.");
            return;
        }

        try {
            switch (value(req, "accion")) {
                case "crear" -> {
                    long id = service.create(
                        current,
                        parseId(value(req, "id_paciente"), "paciente"),
                        parseId(value(req, "id_odontologo"), "odontólogo"),
                        value(req, "id_especialidad"), value(req, "fecha"),
                        value(req, "hora_inicio"), value(req, "hora_fin"),
                        value(req, "motivo"), value(req, "observaciones")
                    );
                    AuditLog.businessChange(current, "CREATE", "CITA", id);
                    flash(req, "agendaSuccess", "Cita registrada correctamente con estado PENDIENTE.");
                    res.sendRedirect(req.getContextPath() + "/agenda?editar=" + id);
                    return;
                }
                case "actualizar" -> {
                    long id = parseId(value(req, "id_cita"), "cita");
                    service.update(
                        current, id,
                        parseId(value(req, "id_paciente"), "paciente"),
                        parseId(value(req, "id_odontologo"), "odontólogo"),
                        value(req, "id_especialidad"), value(req, "fecha"),
                        value(req, "hora_inicio"), value(req, "hora_fin"),
                        value(req, "motivo"), value(req, "observaciones")
                    );
                    AuditLog.businessChange(current, "UPDATE", "CITA", id);
                    flash(req, "agendaSuccess", "Cita reprogramada correctamente.");
                }
                case "estado" -> {
                    long id = parseId(value(req, "id_cita"), "cita");
                    service.changeStatus(current, id, value(req, "estado"));
                    AuditLog.businessChange(current, "STATUS_CHANGE", "CITA", id);
                    flash(req, "agendaSuccess", "Estado de la cita actualizado correctamente.");
                }
                default -> throw new IllegalArgumentException("Acción de agenda no válida.");
            }
        } catch (IllegalArgumentException ex) {
            flash(req, "agendaError", ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo guardando agenda", ex);
            String sqlState = ex.getSQLState();
            String message;
            if ("23000".equals(sqlState)) {
                message = "No se pudo guardar la cita por una relación inválida. Verifica paciente, odontólogo y especialidad.";
            } else if (sqlState != null && sqlState.startsWith("42")) {
                message = "No se pudo guardar la cita por un error interno de consulta SQL. Revisa el log de Tomcat.";
            } else if (sqlState != null && sqlState.startsWith("28")) {
                message = "MySQL rechazó las credenciales o permisos de la aplicación.";
            } else if (sqlState != null && sqlState.startsWith("08")) {
                message = "No se pudo conectar con MySQL. Verifica que el servidor esté disponible.";
            } else {
                message = "No se pudo guardar la cita por un error de base de datos. Revisa el log de Tomcat.";
            }
            flash(req, "agendaError", message);
        }
        res.sendRedirect(req.getContextPath() + "/agenda");
    }

    private User currentUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        if (!user.role().allows("agenda")) {
            res.sendError(403, "No tienes permiso para acceder a la agenda.");
            return null;
        }
        return user;
    }

    private boolean validCsrf(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("csrf");
        String supplied = req.getParameter("csrf");
        return expected != null && expected.equals(supplied);
    }

    private void flash(HttpServletRequest req, String key, String message) {
        req.getSession().setAttribute(key, message);
    }

    private String value(HttpServletRequest req, String name) {
        return StringUtils.trimToEmpty(req.getParameter(name));
    }

    private long parseId(String raw, String label) {
        try {
            long id = Long.parseLong(raw);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identificador de " + label + " no válido.");
        }
    }
}
