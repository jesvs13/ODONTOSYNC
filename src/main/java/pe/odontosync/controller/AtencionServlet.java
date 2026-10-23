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
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcClinicaAtencionDAO;
import pe.odontosync.model.AtencionCitaDetalle;
import pe.odontosync.security.AuditLog;
import pe.odontosync.security.AppLog;
import pe.odontosync.service.AtencionClinicaService;

/** Registra la atención clínica de una cita confirmada. */
@WebServlet("/atencion")
public final class AtencionServlet extends HttpServlet {
    private final AtencionClinicaService service = new AtencionClinicaService(new JdbcClinicaAtencionDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentDentist(req, res);
        if (current == null) return;
        try {
            long appointmentId = parseId(value(req, "id"), "cita");
            AtencionCitaDetalle appointment = service.load(current, appointmentId);
            req.setAttribute("user", current);
            req.setAttribute("active", "agenda");
            req.setAttribute("attention", appointment);
            HttpSession session = req.getSession(false);
            if (session != null) {
                req.setAttribute("success", session.getAttribute("attentionSuccess"));
                req.setAttribute("error", session.getAttribute("attentionError"));
                session.removeAttribute("attentionSuccess");
                session.removeAttribute("attentionError");
            }
            req.getRequestDispatcher("/WEB-INF/views/atencion.jsp").forward(req, res);
        } catch (IllegalArgumentException ex) {
            flash(req, "agendaError", ex.getMessage());
            res.sendRedirect(req.getContextPath() + "/agenda");
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando atención clínica", ex);
            res.sendError(503, "No se pudo consultar la información clínica en MySQL.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentDentist(req, res);
        if (current == null) return;
        req.setCharacterEncoding("UTF-8");
        if (!validCsrf(req)) {
            res.sendError(403, "Solicitud rechazada.");
            return;
        }
        long appointmentId;
        try {
            appointmentId = parseId(value(req, "id_cita"), "cita");
            long consultationId = service.save(
                current, appointmentId, value(req, "motivo_consulta"), value(req, "diagnostico"),
                value(req, "tratamiento"), value(req, "observaciones"), value(req, "recomendaciones")
            );
            AuditLog.clinicalAttentionSaved(current, appointmentId, consultationId);
            flash(req, "attentionSuccess", "Atención clínica guardada. La cita quedó en estado ATENDIDA y la historia del paciente fue actualizada.");
        } catch (IllegalArgumentException ex) {
            appointmentId = safeId(value(req, "id_cita"));
            flash(req, "attentionError", ex.getMessage());
        } catch (SQLException ex) {
            appointmentId = safeId(value(req, "id_cita"));
            AppLog.error("Fallo guardando atención clínica", ex);
            flash(req, "attentionError", friendlySqlError(ex));
        }
        if (appointmentId > 0) res.sendRedirect(req.getContextPath() + "/atencion?id=" + appointmentId);
        else res.sendRedirect(req.getContextPath() + "/agenda");
    }

    private String friendlySqlError(SQLException ex) {
        String state = ex.getSQLState();
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if ((state != null && state.startsWith("28")) || message.contains("denied to user") || message.contains("command denied")) {
            return "MySQL rechazó los permisos para guardar información clínica. Ejecuta permisos_atencion_historia.sql.";
        }
        if ("45000".equals(state)) {
            return ex.getMessage();
        }
        if (state != null && state.startsWith("23")) {
            return "No se pudo guardar la atención por una relación de datos inválida. Verifica la cita y vuelve a intentarlo.";
        }
        return "No se pudo guardar la atención clínica por un error de base de datos. Revisa el log de Tomcat si el problema continúa.";
    }

    private User currentDentist(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        if (user.role() != Role.ODONTOLOGO) {
            res.sendError(403, "Solo el odontólogo puede registrar la atención clínica.");
            return null;
        }
        return user;
    }

    private boolean validCsrf(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("csrf");
        return expected != null && expected.equals(req.getParameter("csrf"));
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

    private long safeId(String raw) {
        try { return Long.parseLong(raw); } catch (Exception ignored) { return 0; }
    }

    private void flash(HttpServletRequest req, String key, String message) {
        req.getSession().setAttribute(key, message);
    }
}
