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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.odontosync.dao.JdbcPasswordResetDAO;
import pe.odontosync.mail.SmtpPasswordResetMailer;
import pe.odontosync.model.PasswordResetResult;
import pe.odontosync.security.AuditLog;
import pe.odontosync.security.CsrfTokens;
import pe.odontosync.service.PasswordResetService;

@WebServlet("/restablecer-password")
public final class ResetPasswordServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ResetPasswordServlet.class);
    private final PasswordResetService service = new PasswordResetService(
        new JdbcPasswordResetDAO(),
        new SmtpPasswordResetMailer()
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        String token = req.getParameter("token");
        HttpSession session = req.getSession(true);
        req.setAttribute("csrf", CsrfTokens.ensure(session));
        req.setAttribute("token", token == null ? "" : token);
        try {
            if (!service.isTokenUsable(token)) {
                req.setAttribute(
                    "error",
                    "El enlace de recuperacion no es valido, ya fue utilizado o ha expirado."
                );
            }
        } catch (SQLException ex) {
            LOG.error("Fallo validando token de recuperacion", ex);
            req.setAttribute("error", "No se pudo validar el enlace en este momento.");
        }
        req.getRequestDispatcher("/WEB-INF/views/restablecer-password.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if (!CsrfTokens.valid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Solicitud rechazada.");
            return;
        }

        String token = req.getParameter("token");
        char[] password = chars(req, "clave_nueva");
        char[] confirmation = chars(req, "clave_confirmacion");
        try {
            PasswordResetResult result = service.resetPassword(token, password, confirmation);
            AuditLog.passwordResetCompleted(result.userId(), result.clinicId(), req.getRemoteAddr());

            HttpSession session = req.getSession(false);
            if (session != null) session.invalidate();
            res.sendRedirect(req.getContextPath() + "/login?restablecida=1");
            return;
        } catch (IllegalArgumentException ex) {
            req.setAttribute("error", ex.getMessage());
        } catch (SQLException ex) {
            LOG.error("Fallo restableciendo contrasena", ex);
            req.setAttribute("error", "No se pudo actualizar la contrasena en este momento.");
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }

        HttpSession session = req.getSession(true);
        req.setAttribute("csrf", CsrfTokens.ensure(session));
        req.setAttribute("token", token == null ? "" : token);
        req.getRequestDispatcher("/WEB-INF/views/restablecer-password.jsp").forward(req, res);
    }

    private char[] chars(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? new char[0] : value.toCharArray();
    }
}
