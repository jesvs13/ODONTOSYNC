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
import pe.odontosync.dao.JdbcPagoDAO;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.PagoService;

/** Consulta y registro de pagos de citas atendidas. */
@WebServlet("/modulo/pagos")
public final class PagoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String SUCCESS_KEY = "paymentSuccess";
    private static final String ERROR_KEY = "paymentError";

    private transient PagoService service;

    @Override
    public void init() {
        service = new PagoService(new JdbcPagoDAO());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentUser(req, res);
        if (current == null) return;

        try {
            req.setAttribute("user", current);
            req.setAttribute("active", "pagos");
            req.setAttribute("canRegisterPayments", service.canRegister(current.role()));
            req.setAttribute("pendingPayments", service.pending(current));
            req.setAttribute("paymentHistory", service.history(current));
            loadFlashMessages(req);
            req.getRequestDispatcher("/WEB-INF/views/pagos.jsp").forward(req, res);
        } catch (IllegalArgumentException ex) {
            flash(req, ERROR_KEY, ex.getMessage());
            res.sendRedirect(req.getContextPath() + "/panel");
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando pagos", ex);
            res.sendError(503, "No se pudo consultar la información de pagos.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws IOException {
        User current = currentUser(req, res);
        if (current == null) return;

        req.setCharacterEncoding("UTF-8");
        if (!validCsrf(req)) {
            res.sendError(403, "Solicitud rechazada.");
            return;
        }
        if (!service.canRegister(current.role())) {
            res.sendError(403, "Tu perfil no puede registrar pagos.");
            return;
        }

        try {
            String action = value(req, "accion");
            if (!"registrar".equals(action)) {
                throw new IllegalArgumentException("Acción de pago no válida.");
            }

            long paymentId = service.register(
                current,
                parseId(value(req, "id_cita")),
                value(req, "monto"),
                value(req, "metodo_pago"),
                value(req, "observaciones")
            );

            AuditLog.businessChange(current, "CREATE", "PAGO", paymentId);
            flash(req, SUCCESS_KEY, "Pago registrado correctamente.");
        } catch (IllegalArgumentException ex) {
            flash(req, ERROR_KEY, ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo registrando pago", ex);
            String state = ex.getSQLState();
            String message = state != null && state.startsWith("42")
                ? "MySQL rechazó la operación. Revisa los permisos del módulo Pagos."
                : "No se pudo registrar el pago en MySQL.";
            flash(req, ERROR_KEY, message);
        }

        res.sendRedirect(req.getContextPath() + "/modulo/pagos");
    }

    private User currentUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        if (!user.role().allows("pagos")) {
            AuditLog.accessDenied(user, "/modulo/pagos");
            res.sendError(403, "No tienes permiso para acceder a Pagos.");
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
            throw new IllegalArgumentException("Selecciona una cita atendida válida.", ex);
        }
    }
}
