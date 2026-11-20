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
import pe.odontosync.dao.JdbcRecetaDAO;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.RecetaService;

/** Emisión y consulta de recetas médicas por odontólogo. */
@WebServlet("/modulo/recetas")
public final class RecetaServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String SUCCESS_KEY = "recipeSuccess";
    private static final String ERROR_KEY = "recipeError";

    private transient RecetaService service;

    @Override
    public void init() {
        service = new RecetaService(new JdbcRecetaDAO());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentDentist(req, res);
        if (current == null) return;

        try {
            req.setAttribute("user", current);
            req.setAttribute("active", "recetas");
            req.setAttribute("patients", service.patients(current));
            req.setAttribute("recipeHistory", service.history(current));
            req.setAttribute("selectedPatientId", safePositiveLong(req.getParameter("paciente")));
            loadFlashMessages(req);
            req.getRequestDispatcher("/WEB-INF/views/recetas.jsp").forward(req, res);
        } catch (IllegalArgumentException ex) {
            flash(req, ERROR_KEY, ex.getMessage());
            res.sendRedirect(req.getContextPath() + "/panel");
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando recetas", ex);
            res.sendError(503, "No se pudo consultar la información de recetas.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws IOException {
        User current = currentDentist(req, res);
        if (current == null) return;

        req.setCharacterEncoding("UTF-8");
        if (!validCsrf(req)) {
            res.sendError(403, "Solicitud rechazada.");
            return;
        }

        try {
            String action = value(req, "accion");
            if (!"crear".equals(action)) {
                throw new IllegalArgumentException("Acción de receta no válida.");
            }

            long recipeId = service.create(
                current,
                parseId(value(req, "id_paciente")),
                value(req, "medicamentos"),
                value(req, "indicaciones")
            );

            AuditLog.businessChange(current, "CREATE", "RECETA", recipeId);
            flash(req, SUCCESS_KEY, "Receta médica emitida correctamente.");
        } catch (IllegalArgumentException ex) {
            flash(req, ERROR_KEY, ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo registrando receta", ex);
            String state = ex.getSQLState();
            String message = state != null && state.startsWith("42")
                ? "MySQL rechazó la operación. Ejecuta permisos_recetas.sql."
                : "No se pudo registrar la receta en MySQL.";
            flash(req, ERROR_KEY, message);
        }

        res.sendRedirect(req.getContextPath() + "/modulo/recetas");
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
            res.sendError(403, "Solo el odontólogo puede acceder a Recetas.");
            return null;
        }
        return user;
    }

    private void loadFlashMessages(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;
        req.setAttribute("success", session.getAttribute(SUCCESS_KEY));
        req.setAttribute("error", session.getAttribute(ERROR_KEY));
        session.removeAttribute(SUCCESS_KEY);
        session.removeAttribute(ERROR_KEY);
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
        try {
            long id = Long.parseLong(raw);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Selecciona un paciente válido.", ex);
        }
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
