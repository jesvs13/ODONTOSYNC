package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcUsuarioDAO;
import pe.odontosync.mail.SmtpLoginOtpMailer;
import pe.odontosync.security.AuditLog;
import pe.odontosync.security.LoginAttemptGuard;
import pe.odontosync.security.LoginSessions;
import pe.odontosync.service.AuthService;
import pe.odontosync.service.LoginOtpDeliveryException;
import pe.odontosync.service.LoginOtpService;

/** Primer factor del inicio de sesión: correo y contraseña. */
@WebServlet("/login")
public final class LoginServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(LoginServlet.class);
    private static final LoginAttemptGuard ATTEMPTS = new LoginAttemptGuard();

    private final AuthService authService = new AuthService(new JdbcUsuarioDAO());
    private final LoginOtpService otpService =
        new LoginOtpService(new SmtpLoginOtpMailer());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") instanceof User user) {
            res.sendRedirect(req.getContextPath() + LoginSessions.destination(user));
            return;
        }
        if (LoginSessions.pending(session) != null) {
            res.sendRedirect(req.getContextPath() + "/verificar-login");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String email = StringUtils.lowerCase(
            StringUtils.trimToEmpty(req.getParameter("correo"))
        );
        String submitted = req.getParameter("clave");

        if (invalidCredentialsInput(email, submitted)) {
            AuditLog.loginFailure(email, req.getRemoteAddr());
            error(req, res, 400, "Ingresa un correo y contraseña válidos.");
            return;
        }

        String attemptKey = attemptKey(req, res, email);
        if (attemptKey == null) return;

        char[] password = submitted.toCharArray();
        try {
            User user = authService.verifyFirstFactor(email, password);
            if (user == null) {
                ATTEMPTS.recordFailure(attemptKey);
                AuditLog.loginFailure(email, req.getRemoteAddr());
                error(
                    req,
                    res,
                    401,
                    "Correo o contraseña incorrectos, o cuenta inactiva."
                );
                return;
            }

            var challenge = otpService.issue(user, email);
            ATTEMPTS.recordSuccess(attemptKey);
            LoginSessions.beginSecondFactor(req, challenge);

            AuditLog.loginSecondFactorIssued(user, req.getRemoteAddr());
            res.sendRedirect(req.getContextPath() + "/verificar-login");
        } catch (LoginOtpDeliveryException ex) {
            LOG.error("No se pudo entregar el segundo factor por correo", ex);
            error(
                req,
                res,
                503,
                "No se pudo enviar el código de seguridad. Inténtalo nuevamente."
            );
        } catch (Exception ex) {
            LOG.error("Fallo consultando MySQL durante el login", ex);
            error(req, res, 503, "No se pudo consultar MySQL. Revisa la conexión.");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private String attemptKey(
        HttpServletRequest req,
        HttpServletResponse res,
        String email
    ) throws ServletException, IOException {
        String key;
        try {
            key = ATTEMPTS.key(email, req.getRemoteAddr());
        } catch (IllegalArgumentException ex) {
            AuditLog.loginFailure(email, req.getRemoteAddr());
            error(req, res, 400, "Datos de acceso no válidos.");
            return null;
        }

        if (!ATTEMPTS.isAllowed(key)) {
            AuditLog.loginBlocked(email, req.getRemoteAddr());
            res.setHeader("Retry-After", "300");
            error(
                req,
                res,
                429,
                "Demasiados intentos fallidos. Espera unos minutos antes de volver a intentar."
            );
            return null;
        }
        return key;
    }

    private boolean invalidCredentialsInput(String email, String password) {
        return StringUtils.isBlank(email)
            || email.length() > 150
            || StringUtils.isEmpty(password)
            || password.length() > 1000;
    }

    private void error(
        HttpServletRequest req,
        HttpServletResponse res,
        int status,
        String message
    ) throws ServletException, IOException {
        req.setAttribute("error", message);
        res.setStatus(status);
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
    }
}
