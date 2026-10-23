package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcUsuarioDAO;
import pe.odontosync.security.AuditLog;
import pe.odontosync.security.LoginAttemptGuard;
import pe.odontosync.service.AuthService;

/** Controlador HTTP del caso de uso de inicio de sesión. */
@WebServlet("/login")
public final class LoginServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(LoginServlet.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final LoginAttemptGuard ATTEMPTS = new LoginAttemptGuard();
    private final AuthService authService = new AuthService(new JdbcUsuarioDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        if (req.getSession(false) != null && req.getSession(false).getAttribute("user") instanceof User) {
            res.sendRedirect(req.getContextPath() + "/panel");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String email = StringUtils.lowerCase(StringUtils.trimToEmpty(req.getParameter("correo")));
        String submitted = req.getParameter("clave");

        if (StringUtils.isBlank(email) || email.length() > 150
            || StringUtils.isEmpty(submitted) || submitted.length() > 1000) {
            AuditLog.loginFailure(email, req.getRemoteAddr());
            error(req, res, 400, "Ingresa un correo y contraseña válidos.");
            return;
        }

        String attemptKey;
        try {
            attemptKey = ATTEMPTS.key(email, req.getRemoteAddr());
        } catch (IllegalArgumentException ex) {
            AuditLog.loginFailure(email, req.getRemoteAddr());
            error(req, res, 400, "Datos de acceso no válidos.");
            return;
        }
        if (!ATTEMPTS.isAllowed(attemptKey)) {
            AuditLog.loginBlocked(email, req.getRemoteAddr());
            res.setHeader("Retry-After", "300");
            error(req, res, 429, "Demasiados intentos fallidos. Espera unos minutos antes de volver a intentar.");
            return;
        }

        char[] password = submitted.toCharArray();
        try {
            User user = authService.authenticate(email, password);
            if (user == null) {
                ATTEMPTS.recordFailure(attemptKey);
                AuditLog.loginFailure(email, req.getRemoteAddr());
                error(req, res, 401, "Correo o contraseña incorrectos, o cuenta inactiva.");
                return;
            }
            ATTEMPTS.recordSuccess(attemptKey);

            // Mitiga session fixation: invalida cualquier sesión previa y crea una nueva.
            HttpSession previous = req.getSession(false);
            if (previous != null) previous.invalidate();

            HttpSession session = req.getSession(true);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute("user", user);

            byte[] random = new byte[32];
            RANDOM.nextBytes(random);
            session.setAttribute("csrf", Base64.getUrlEncoder().withoutPadding().encodeToString(random));

            AuditLog.loginSuccess(user, req.getRemoteAddr());
            res.sendRedirect(req.getContextPath() + "/panel");
        } catch (Exception ex) {
            LOG.error("Fallo consultando MySQL durante el login", ex);
            error(req, res, 503, "No se pudo consultar MySQL. Revisa la conexión.");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void error(HttpServletRequest req, HttpServletResponse res, int status, String message)
        throws ServletException, IOException {
        req.setAttribute("error", message);
        res.setStatus(status);
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
    }
}
