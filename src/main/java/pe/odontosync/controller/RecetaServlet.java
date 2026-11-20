package pe.odontosync.controller;

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
import pe.odontosync.dao.JdbcRecetaDAO;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.RecetaService;

/**
 * Ruta de compatibilidad para recetas.
 * La experiencia principal se integra dentro de Historia clínica.
 */
@WebServlet("/modulo/recetas")
public final class RecetaServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private transient RecetaService service;

    @Override
    public void init() {
        service = new RecetaService(new JdbcRecetaDAO());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws IOException {

        User current = currentDentist(req, res);
        if (current == null) return;

        long patientId = safePositiveLong(req.getParameter("paciente"));
        res.sendRedirect(historyTarget(req, patientId));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws IOException {

        User current = currentDentist(req, res);
        if (current == null) return;

        req.setCharacterEncoding("UTF-8");
        if (!validCsrf(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Solicitud rechazada.");
            return;
        }

        long patientId = safePositiveLong(value(req, "id_paciente"));

        try {
            String action = value(req, "accion");
            if (!"crear".equals(action)) {
                throw new IllegalArgumentException("Acción de receta no válida.");
            }

            patientId = parseId(value(req, "id_paciente"));

            long recipeId = service.create(
                current,
                patientId,
                value(req, "medicamentos"),
                value(req, "indicaciones")
            );

            AuditLog.businessChange(current, "CREATE", "RECETA", recipeId);
            flash(
                req,
                "historySuccess",
                "Receta médica emitida correctamente."
            );
        } catch (IllegalArgumentException ex) {
            flash(req, "historyError", ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo registrando receta", ex);
            String state = ex.getSQLState();
            String message = state != null && state.startsWith("42")
                ? "MySQL rechazó la operación. Ejecuta permisos_recetas.sql."
                : "No se pudo registrar la receta en MySQL.";
            flash(req, "historyError", message);
        }

        res.sendRedirect(historyTarget(req, patientId));
    }

    private String historyTarget(HttpServletRequest req, long patientId) {
        String target = req.getContextPath() + "/historias";
        if (patientId > 0) {
            target += "?paciente=" + patientId + "&tab=recetas";
        }
        return target;
    }

    private User currentDentist(HttpServletRequest req, HttpServletResponse res)
        throws IOException {

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }

        if (user.role() != Role.ODONTOLOGO || !user.role().allows("recetas")) {
            AuditLog.accessDenied(user, "/modulo/recetas");
            res.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Solo el odontólogo puede acceder a Recetas."
            );
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

    private String value(HttpServletRequest req, String name) {
        return StringUtils.trimToEmpty(req.getParameter(name));
    }

    private long parseId(String raw) {
        long value = safePositiveLong(raw);
        if (value <= 0) {
            throw new IllegalArgumentException("Selecciona un paciente válido.");
        }
        return value;
    }

    private long safePositiveLong(String raw) {
        try {
            long value = Long.parseLong(StringUtils.trimToEmpty(raw));
            return value > 0 ? value : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void flash(HttpServletRequest req, String key, String message) {
        req.getSession().setAttribute(key, message);
    }
}
