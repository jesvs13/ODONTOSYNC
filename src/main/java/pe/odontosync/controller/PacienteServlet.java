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
import pe.odontosync.dao.JdbcPacienteDAO;
import pe.odontosync.model.PacienteDetalle;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.PacienteService;

/** CRUD de pacientes y ficha longitudinal. */
@WebServlet("/pacientes")
public final class PacienteServlet extends HttpServlet {
    private final PacienteService service = new PacienteService(new JdbcPacienteDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentUser(req, res);
        if (current == null) return;

        try {
            String query = value(req, "q");
            req.setAttribute("user", current);
            req.setAttribute("active", "pacientes");
            req.setAttribute("query", query);
            req.setAttribute("canManagePatients", service.canManage(current.role()));
            req.setAttribute("canViewClinical", current.role() == pe.odontosync.Role.ODONTOLOGO);
            req.setAttribute("canViewPayments", current.role() == pe.odontosync.Role.ADMINISTRADOR || current.role() == pe.odontosync.Role.RECEPCIONISTA);
            req.setAttribute("pacientes", service.list(current, query));

            String edit = req.getParameter("editar");
            if (edit != null && !edit.isBlank()) {
                if (!service.canManage(current.role())) {
                    res.sendError(403, "Tu perfil no puede modificar pacientes.");
                    return;
                }
                long id = parseId(edit);
                PacienteDetalle selected = service.findVisible(current, id)
                    .orElseThrow(() -> new IllegalArgumentException("El paciente solicitado no existe."));
                req.setAttribute("editPatient", selected);
            }

            String view = req.getParameter("ver");
            if (view != null && !view.isBlank()) {
                req.setAttribute("patientFile", service.ficha(current, parseId(view)));
            }

            HttpSession session = req.getSession(false);
            if (session != null) {
                req.setAttribute("success", session.getAttribute("patientSuccess"));
                req.setAttribute("error", session.getAttribute("patientError"));
                session.removeAttribute("patientSuccess");
                session.removeAttribute("patientError");
            }
            req.getRequestDispatcher("/WEB-INF/views/pacientes.jsp").forward(req, res);
        } catch (IllegalArgumentException ex) {
            flash(req, "patientError", ex.getMessage());
            res.sendRedirect(req.getContextPath() + "/pacientes");
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando pacientes", ex);
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
            res.sendError(403, "Tu perfil puede consultar pacientes, pero no modificarlos.");
            return;
        }

        try {
            switch (value(req, "accion")) {
                case "crear" -> {
                    long id = service.create(
                        current,
                        value(req, "tipo_documento"), value(req, "numero_documento"),
                        value(req, "nombres"), value(req, "apellidos"), value(req, "fecha_nacimiento"),
                        value(req, "sexo"), value(req, "direccion"), value(req, "telefono"),
                        value(req, "correo"), value(req, "ocupacion"), value(req, "contacto_emergencia"),
                        value(req, "telefono_emergencia"), value(req, "estado")
                    );
                    AuditLog.businessChange(current, "CREATE", "PACIENTE", id);
                    flash(req, "patientSuccess", "Paciente registrado correctamente.");
                    res.sendRedirect(req.getContextPath() + "/pacientes?ver=" + id);
                    return;
                }
                case "actualizar" -> {
                    long id = parseId(value(req, "id_paciente"));
                    service.update(
                        current, id,
                        value(req, "tipo_documento"), value(req, "numero_documento"),
                        value(req, "nombres"), value(req, "apellidos"), value(req, "fecha_nacimiento"),
                        value(req, "sexo"), value(req, "direccion"), value(req, "telefono"),
                        value(req, "correo"), value(req, "ocupacion"), value(req, "contacto_emergencia"),
                        value(req, "telefono_emergencia"), value(req, "estado")
                    );
                    AuditLog.businessChange(current, "UPDATE", "PACIENTE", id);
                    flash(req, "patientSuccess", "Datos del paciente actualizados correctamente.");
                    res.sendRedirect(req.getContextPath() + "/pacientes?ver=" + id);
                    return;
                }
                case "estado" -> {
                    long id = parseId(value(req, "id_paciente"));
                    service.changeStatus(current, id, value(req, "estado"));
                    AuditLog.businessChange(current, "STATUS_CHANGE", "PACIENTE", id);
                    flash(req, "patientSuccess", "Estado del paciente actualizado. Su historial se mantiene intacto.");
                }
                default -> throw new IllegalArgumentException("Acción no válida.");
            }
        } catch (IllegalArgumentException ex) {
            flash(req, "patientError", ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo guardando CRUD de pacientes", ex);
            String message = "23000".equals(ex.getSQLState())
                ? "No se pudo guardar: el documento ya está registrado o existe una relación inválida."
                : "No se pudo guardar en MySQL. Ejecuta los permisos del CRUD de pacientes.";
            flash(req, "patientError", message);
        }
        res.sendRedirect(req.getContextPath() + "/pacientes");
    }

    private User currentUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        if (!user.role().allows("pacientes")) {
            res.sendError(403, "No tienes permiso para acceder a pacientes.");
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
            throw new IllegalArgumentException("Identificador de paciente no válido.");
        }
    }
}
