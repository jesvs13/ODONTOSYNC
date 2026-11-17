package pe.odontosync.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** CSRF reutilizable tambien para formularios publicos que crean una sesion anonima. */
public final class CsrfTokens {
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokens() { }

    public static String ensure(HttpSession session) {
        Object existing = session.getAttribute("csrf");
        if (existing instanceof String token && !token.isBlank()) return token;

        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        session.setAttribute("csrf", token);
        return token;
    }

    public static boolean valid(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        Object expected = session.getAttribute("csrf");
        String supplied = req.getParameter("csrf");
        if (!(expected instanceof String expectedString) || supplied == null) return false;
        return MessageDigest.isEqual(
            expectedString.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}
