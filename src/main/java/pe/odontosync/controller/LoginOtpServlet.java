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
import pe.odontosync.User;
import pe.odontosync.dao.JdbcUsuarioDAO;
import pe.odontosync.mail.SmtpLoginOtpMailer;
import pe.odontosync.model.LoginOtpChallenge;
import pe.odontosync.model.LoginOtpStatus;
import pe.odontosync.model.LoginOtpVerification;
import pe.odontosync.security.AuditLog;
import pe.odontosync.security.CsrfTokens;
import pe.odontosync.service.AuthService;
import pe.odontosync.service.LoginOtpDeliveryException;
import pe.odontosync.service.LoginOtpService;

/** Segundo factor del login: código temporal enviado al correo del usuario. */
@WebServlet("/verificar-login")
public final class LoginOtpServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(LoginOtpServlet.class);

    private final AuthService authService = new AuthService(new JdbcUsuarioDAO());
    private final LoginOtpService otpService =
        new LoginOtpService(new SmtpLoginOtpMailer());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") instanceof User) {
            res.sendRedirect(req.getContextPath() + "/panel");
            return;
        }

        LoginOtpChallenge challenge = pending(session);
        if (challenge == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if (otpService.isExpired(challenge) || otpService.isLocked(challenge)) {
            session.invalidate();
            res.sendRedirect(req.getContextPath() + "/login?verificacion=expirada");
            return;
        }

        render(req, res, session, challenge, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        LoginOtpChallenge challenge = pending(session);

        if (challenge == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!CsrfTokens.valid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Solicitud rechazada.");
            return;
        }

        if ("reenviar".equals(req.getParameter("accion"))) {
            resend(req, res, session, challenge);
            return;
        }

        LoginOtpVerification verification =
            otpService.verify(challenge, req.getParameter("codigo"));

        if (verification.status() == LoginOtpStatus.VERIFIED) {
            complete(req, res, session, challenge);
            return;
        }

        if (verification.status() == LoginOtpStatus.EXPIRED) {
            session.invalidate();
            res.sendRedirect(req.getContextPath() + "/login?verificacion=expirada");
            return;
        }

        if (verification.status() == LoginOtpStatus.LOCKED) {
            AuditLog.loginSecondFactorBlocked(challenge.user(), req.getRemoteAddr());
            session.invalidate();
            res.sendRedirect(req.getContextPath() + "/login?verificacion=bloqueada");
            return;
        }

        LoginOtpChallenge failed = verification.challenge();
        session.setAttribute("loginOtpChallenge", failed);
        AuditLog.loginSecondFactorFailure(failed.user(), req.getRemoteAddr());
        render(
            req,
            res,
            session,
            failed,
            "Código incorrecto. Te quedan "
                + otpService.attemptsRemaining(failed)
                + " intentos.",
            null
        );
    }

    private void resend(
        HttpServletRequest req,
        HttpServletResponse res,
        HttpSession session,
        LoginOtpChallenge challenge
    ) throws ServletException, IOException {
        try {
            LoginOtpChallenge replacement = otpService.resend(challenge);
            session.setAttribute("loginOtpChallenge", replacement);
            AuditLog.loginSecondFactorResent(replacement.user(), req.getRemoteAddr());
            render(
                req,
                res,
                session,
                replacement,
                null,
                "Se envió un nuevo código a tu correo."
            );
        } catch (IllegalStateException ex) {
            render(req, res, session, challenge, ex.getMessage(), null);
        } catch (LoginOtpDeliveryException ex) {
            LOG.error("No se pudo reenviar el segundo factor", ex);
            render(
                req,
                res,
                session,
                challenge,
                "No se pudo reenviar el código en este momento.",
                null
            );
        }
    }

    private void complete(
        HttpServletRequest req,
        HttpServletResponse res,
        HttpSession pendingSession,
        LoginOtpChallenge challenge
    ) throws IOException {
        try {
            authService.completeLogin(challenge.user());
        } catch (SQLException ex) {
            LOG.error("No se pudo registrar el acceso después del segundo factor", ex);
            res.sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "No se pudo completar el inicio de sesión."
            );
            return;
        }

        pendingSession.invalidate();

        HttpSession authenticated = req.getSession(true);
        authenticated.setMaxInactiveInterval(30 * 60);
        authenticated.setAttribute("user", challenge.user());
        CsrfTokens.ensure(authenticated);

        AuditLog.loginSuccess(challenge.user(), req.getRemoteAddr());
        res.sendRedirect(req.getContextPath() + "/panel");
    }

    private void render(
        HttpServletRequest req,
        HttpServletResponse res,
        HttpSession session,
        LoginOtpChallenge challenge,
        String error,
        String success
    ) throws ServletException, IOException {
        req.setAttribute("csrf", CsrfTokens.ensure(session));
        req.setAttribute("maskedEmail", maskEmail(challenge.email()));
        req.setAttribute("error", error);
        req.setAttribute("success", success);
        req.getRequestDispatcher("/WEB-INF/views/login-otp.jsp").forward(req, res);
    }

    private LoginOtpChallenge pending(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("loginOtpChallenge");
        return value instanceof LoginOtpChallenge challenge ? challenge : null;
    }

    private String maskEmail(String email) {
        if (email == null) return "***";
        int at = email.indexOf('@');
        if (at <= 1) return "***" + (at >= 0 ? email.substring(at) : "");
        return email.substring(0, 1) + "***" + email.substring(at);
    }
}
