package pe.odontosync.security;

import com.google.common.collect.ImmutableSet;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;
import java.sql.SQLException;
import java.util.Objects;
import pe.odontosync.dao.JdbcUsuarioDAO;
import pe.odontosync.model.LoginOtpChallenge;
import pe.odontosync.User;

/**
 * Comprueba sesión, permite activar HTTPS en producción y aplica cabeceras de seguridad.
 * Guava mantiene inmutables las rutas protegidas.
 */
@WebFilter("/*")
public final class AccessFilter implements Filter {
    @FunctionalInterface
    public interface CredentialCheck {
        boolean isCurrent(User user) throws SQLException;
    }

    private final CredentialCheck credentials;

    public AccessFilter() { this(new JdbcUsuarioDAO()::isCredentialCurrent); }

    public AccessFilter(CredentialCheck credentials) {
        this.credentials = Objects.requireNonNull(credentials);
    }

    private static final Set<String> PRIVATE_EXACT = ImmutableSet.of(
        "/panel", "/modulo", "/usuarios", "/pacientes", "/odontologos",
        "/horarios", "/agenda", "/atencion", "/historias", "/logout",
        "/cambiar-password", "/reportes/exportar-citas.xlsx"
    );

    private static final Set<String> PRIVATE_PREFIXES = ImmutableSet.of(
        "/modulo/", "/reportes/"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (SecurityConfig.requireHttps() && !req.isSecure()) {
            int port = SecurityConfig.httpsPort();
            StringBuilder target = new StringBuilder("https://")
                .append(req.getServerName());
            if (port != 443) target.append(':').append(port);
            target.append(req.getRequestURI());
            if (req.getQueryString() != null && !req.getQueryString().isBlank()) {
                target.append('?').append(req.getQueryString());
            }
            res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT); // 307 conserva POST/GET.
            res.setHeader("Location", target.toString());
            return;
        }

        // Defensa frente a XSS, clickjacking, MIME sniffing y exposición de contexto.
        SecurityHeaders.values().forEach(res::setHeader);

        if (req.isSecure()) {
            res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        // La ruta del servlet ya fue normalizada por el contenedor (sin parámetros de matriz).
        String path = req.getServletPath();
        if (path == null || path.isEmpty()) {
            path = req.getRequestURI().substring(req.getContextPath().length())
                .replaceAll(";[^/]*", "");
        } else if (req.getPathInfo() != null) {
            path += req.getPathInfo();
        }
        boolean privateRoute = PRIVATE_EXACT.contains(path)
            || PRIVATE_PREFIXES.stream().anyMatch(path::startsWith);

        boolean sensitivePublicRoute = "/login".equals(path)
            || "/olvide-password".equals(path)
            || "/restablecer-password".equals(path)
            || "/verificar-login".equals(path);

        if (privateRoute || sensitivePublicRoute) {
            res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            res.setHeader("Pragma", "no-cache");
        }

        HttpSession session = (privateRoute || sensitivePublicRoute) ? req.getSession(false) : null;
        if (session != null && !"/logout".equals(path)) {
            User principal = session.getAttribute("user") instanceof User user ? user : null;
            if (principal == null) {
                LoginOtpChallenge challenge = LoginSessions.pending(session);
                if (challenge != null) principal = challenge.user();
            }
            if (principal != null) {
                try {
                    if (!credentials.isCurrent(principal)) {
                        session.invalidate();
                        res.sendRedirect(req.getContextPath() + "/login");
                        return;
                    }
                } catch (SQLException ex) {
                    AppLog.error("No se pudo comprobar la vigencia de la credencial", ex);
                    res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "No se pudo verificar la sesión.");
                    return;
                }
            }
        }

        if (privateRoute) {
            if (session == null || !(session.getAttribute("user") instanceof User user)) {
                AuditLog.sessionRequired(path, req.getRemoteAddr());
                res.sendRedirect(req.getContextPath() + "/login");
                return;
            }
            if (user.mustChangePassword() && !"/cambiar-password".equals(path)
                && !"/logout".equals(path)) {
                AuditLog.accessDenied(user, path);
                res.sendRedirect(req.getContextPath() + "/cambiar-password");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
