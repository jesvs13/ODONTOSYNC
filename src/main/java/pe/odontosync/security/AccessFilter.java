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
import pe.odontosync.User;

/**
 * Comprueba sesión, permite activar HTTPS en producción y aplica cabeceras de seguridad.
 * Guava mantiene inmutables las rutas protegidas.
 */
@WebFilter("/*")
public final class AccessFilter implements Filter {
    private static final Set<String> PRIVATE_EXACT = ImmutableSet.of(
        "/panel", "/modulo", "/usuarios", "/pacientes", "/odontologos",
        "/horarios", "/agenda", "/atencion", "/historias", "/logout",
        "/reportes/exportar-citas.xlsx"
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
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "no-referrer");
        res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        res.setHeader("Content-Security-Policy",
            "default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self'; "
            + "form-action 'self'; base-uri 'none'; frame-ancestors 'none'; object-src 'none'");

        if (req.isSecure()) {
            res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean privateRoute = PRIVATE_EXACT.contains(path)
            || PRIVATE_PREFIXES.stream().anyMatch(path::startsWith);

        if (privateRoute || "/login".equals(path)) {
            res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            res.setHeader("Pragma", "no-cache");
        }

        if (privateRoute) {
            HttpSession session = req.getSession(false);
            if (session == null || !(session.getAttribute("user") instanceof User)) {
                AuditLog.sessionRequired(path, req.getRemoteAddr());
                res.sendRedirect(req.getContextPath() + "/login");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
