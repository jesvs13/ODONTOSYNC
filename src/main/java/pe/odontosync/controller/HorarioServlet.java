package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcHorarioDAO;
import pe.odontosync.model.HorarioDetalle;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.HorarioService;

/** CRUD de disponibilidad semanal. Administración edita; Recepción y Odontología consultan. */
@WebServlet("/horarios")
public final class HorarioServlet extends HttpServlet {
    private final HorarioService service = new HorarioService(new JdbcHorarioDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentUser(req, res);
        if (current == null) return;
        try {
            req.setAttribute("user", current);
            req.setAttribute("active", "horarios");
            req.setAttribute("canManageSchedules", service.canManage(current.role()));
            req.setAttribute("horarios", service.list(current));
            req.setAttribute("odontologosHorario", service.dentists(current));

            String edit = req.getParameter("editar");
            if (edit != null && !edit.isBlank()) {
                if (!service.canManage(current.role())) {
                    res.sendError(403, "Tu perfil puede consultar horarios, pero no modificarlos.");
                    return;
                }
                HorarioDetalle selected = service.find(current, parseId(edit))
                    .orElseThrow(() -> new IllegalArgumentException("El horario solicitado no existe."));
                req.setAttribute("editSchedule", selected);
            }

            HttpSession session = req.getSession(false);
            if (session != null) {
                req.setAttribute("success", session.getAttribute("scheduleSuccess"));
                req.setAttribute("error", session.getAttribute("scheduleError"));
                session.removeAttribute("scheduleSuccess");
                session.removeAttribute("scheduleError");
            }
            req.getRequestDispatcher("/WEB-INF/views/horarios.jsp").forward(req, res);
        } catch (IllegalArgumentException ex) {
            flash(req, "scheduleError", ex.getMessage());
            res.sendRedirect(req.getContextPath() + "/horarios");
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando horarios", ex);
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
        if (!service.canManage(current.role())) {
            res.sendError(403, "Solo Administración puede modificar horarios.");
            return;
        }

        try {
            switch (value(req, "accion")) {
                case "crear" -> {
                    long dentistId = parseId(value(req, "id_odontologo"));
                    service.create(
                        current, dentistId, value(req, "dia_semana"),
                        value(req, "hora_inicio"), value(req, "hora_fin"), value(req, "estado")
                    );
                    AuditLog.businessChange(current, "CREATE", "HORARIO_ODONTOLOGO", dentistId);
                    flash(req, "scheduleSuccess", "Horario registrado correctamente.");
                }
                case "actualizar" -> {
                    long id = parseId(value(req, "id_horario"));
                    service.update(
                        current, id, parseId(value(req, "id_odontologo")),
                        value(req, "dia_semana"), value(req, "hora_inicio"), value(req, "hora_fin"), value(req, "estado")
                    );
                    AuditLog.businessChange(current, "UPDATE", "HORARIO", id);
                    flash(req, "scheduleSuccess", "Horario actualizado correctamente.");
                }
                case "estado" -> {
                    long id = parseId(value(req, "id_horario"));
                    service.changeStatus(current, id, value(req, "estado"));
                    AuditLog.businessChange(current, "STATUS_CHANGE", "HORARIO", id);
                    flash(req, "scheduleSuccess", "Estado del horario actualizado.");
                }
                default -> throw new IllegalArgumentException("Acción no válida.");
            }
        } catch (IllegalArgumentException ex) {
            flash(req, "scheduleError", ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo guardando CRUD de horarios", ex);
            flash(req, "scheduleError", "No se pudo guardar en MySQL. Ejecuta los permisos de Odontólogos y Horarios.");
        }
        res.sendRedirect(req.getContextPath() + "/horarios");
    }

    private User currentUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        if (!user.role().allows("horarios")) {
            res.sendError(403, "No tienes permiso para acceder a horarios.");
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

    private long parseId(String raw) {
        try {
            long id = Long.parseLong(raw);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identificador no válido.");
        }
    }
}
