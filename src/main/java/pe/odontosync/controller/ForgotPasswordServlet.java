package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.odontosync.dao.JdbcPasswordResetDAO;
import pe.odontosync.mail.SmtpPasswordResetMailer;
import pe.odontosync.security.AuditLog;
import pe.odontosync.security.CsrfTokens;
import pe.odontosync.security.PasswordResetRequestGuard;
import pe.odontosync.service.PasswordResetDeliveryException;
import pe.odontosync.service.PasswordResetService;

@WebServlet("/olvide-password")
public final class ForgotPasswordServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ForgotPasswordServlet.class);
    private static final PasswordResetRequestGuard REQUEST_GUARD = new PasswordResetRequestGuard();
    private final PasswordResetService service = new PasswordResetService(
        new JdbcPasswordResetDAO(),
        new SmtpPasswordResetMailer()
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        req.setAttribute("csrf", CsrfTokens.ensure(session));
        req.setAttribute("success", session.getAttribute("passwordResetRequestSuccess"));
        req.setAttribute("error", session.getAttribute("passwordResetRequestError"));
        session.removeAttribute("passwordResetRequestSuccess");
        session.removeAttribute("passwordResetRequestError");
        req.getRequestDispatcher("/WEB-INF/views/olvide-password.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if (!CsrfTokens.valid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Solicitud rechazada.");
            return;
        }

        String email = req.getParameter("correo");
        HttpSession session = req.getSession(true);
        try {
            if (REQUEST_GUARD.allow(email, req.getRemoteAddr())) {
                service.requestReset(email, publicUrl());
                AuditLog.passwordResetRequested(email, req.getRemoteAddr());
            }
            // Mismo mensaje exista o no la cuenta, incluso si el guard omite una repeticion.
            session.setAttribute(
                "passwordResetRequestSuccess",
                PasswordResetService.GENERIC_REQUEST_MESSAGE
            );
        } catch (PasswordResetDeliveryException ex) {
            LOG.error("No se pudo entregar el correo de recuperacion", ex);
            session.setAttribute(
                "passwordResetRequestError",
                "El servicio de correo no esta disponible en este momento. Intentalo nuevamente mas tarde."
            );
        } catch (SQLException ex) {
            LOG.error("Fallo consultando MySQL durante recuperacion de contrasena", ex);
            session.setAttribute(
                "passwordResetRequestError",
                "No se pudo procesar la solicitud en este momento."
            );
        }

        res.sendRedirect(req.getContextPath() + "/olvide-password");
    }

    private String publicUrl() {
        return System.getenv("ODONTOSYNC_PUBLIC_URL");
    }
}
