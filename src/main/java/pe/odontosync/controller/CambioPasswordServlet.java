package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcPasswordChangeDAO;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.PasswordChangeService;

/** Permite a cualquier usuario autenticado cambiar su propia contraseña. */
@WebServlet("/cambiar-password")
public final class CambioPasswordServlet extends HttpServlet {
    private final PasswordChangeService service =
        new PasswordChangeService(new JdbcPasswordChangeDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentUser(req, res);
        if (current == null) return;

        req.setAttribute("user", current);
        req.setAttribute("active", "cuenta");

        HttpSession session = req.getSession(false);
        if (session != null) {
            req.setAttribute("success", session.getAttribute("passwordSuccess"));
            req.setAttribute("error", session.getAttribute("passwordError"));
            session.removeAttribute("passwordSuccess");
            session.removeAttribute("passwordError");
        }

        req.getRequestDispatcher("/WEB-INF/views/cambiar-password.jsp").forward(req, res);
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

        char[] currentPassword = chars(req, "clave_actual");
        char[] newPassword = chars(req, "clave_nueva");
        char[] confirmation = chars(req, "clave_confirmacion");
        try {
            service.change(current, currentPassword, newPassword, confirmation);
            req.changeSessionId();
            AuditLog.passwordChanged(current, req.getRemoteAddr());
            flash(req, "passwordSuccess", "Contraseña actualizada correctamente.");
        } catch (IllegalArgumentException ex) {
            flash(req, "passwordError", ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo actualizando contraseña", ex);
            flash(req, "passwordError", "No se pudo actualizar la contraseña. Inténtalo nuevamente.");
        } finally {
            Arrays.fill(currentPassword, '\0');
            Arrays.fill(newPassword, '\0');
            Arrays.fill(confirmation, '\0');
        }

        res.sendRedirect(req.getContextPath() + "/cambiar-password");
    }

    private User currentUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
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

    private char[] chars(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? new char[0] : value.toCharArray();
    }

    private void flash(HttpServletRequest req, String key, String message) {
        req.getSession().setAttribute(key, message);
    }
}
