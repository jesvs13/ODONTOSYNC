package pe.odontosync.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import pe.odontosync.User;
import pe.odontosync.model.LoginOtpChallenge;

/** Ciclo de vida de la sesión durante primer factor, segundo factor y acceso completo. */
public final class LoginSessions {
    private static final String PENDING_OTP = "loginOtpChallenge";
    private static final int PENDING_SECONDS = 10 * 60;
    private static final int AUTHENTICATED_SECONDS = 30 * 60;

    private LoginSessions() { }

    public static HttpSession beginSecondFactor(
        HttpServletRequest request,
        LoginOtpChallenge challenge
    ) {
        invalidateCurrent(request);
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(PENDING_SECONDS);
        session.setAttribute(PENDING_OTP, challenge);
        CsrfTokens.ensure(session);
        return session;
    }

    public static LoginOtpChallenge pending(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute(PENDING_OTP);
        return value instanceof LoginOtpChallenge challenge ? challenge : null;
    }

    public static void replacePending(
        HttpSession session,
        LoginOtpChallenge challenge
    ) {
        if (session == null || challenge == null) {
            throw new IllegalArgumentException("La sesión OTP pendiente es obligatoria.");
        }
        session.setAttribute(PENDING_OTP, challenge);
    }

    public static HttpSession complete(
        HttpServletRequest request,
        User user
    ) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario autenticado es obligatorio.");
        }
        invalidateCurrent(request);
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(AUTHENTICATED_SECONDS);
        session.setAttribute("user", user);
        CsrfTokens.ensure(session);
        return session;
    }

    /** Destino permitido tras completar OTP, o al volver al login autenticado. */
    public static String destination(User user) {
        return user.mustChangePassword() ? "/cambiar-password" : "/panel";
    }

    /** Solo se invoca tras guardar satisfactoriamente el nuevo hash y el estado. */
    public static HttpSession completeActivation(HttpServletRequest request, User user) {
        if (user == null || !user.mustChangePassword()) {
            throw new IllegalArgumentException("La cuenta debe estar pendiente de activación.");
        }
        return complete(request, new User(user.id(), user.clinicId(), user.name(),
            user.clinicName(), user.role(), false));
    }

    public static void invalidateCurrent(HttpServletRequest request) {
        HttpSession current = request.getSession(false);
        if (current != null) current.invalidate();
    }
}
