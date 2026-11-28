package pe.odontosync.security;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.model.LoginOtpChallenge;

class CredentialRevocationTest {
    private static final String CONTEXT = "/odontosync";

    @Test void sesionAnteriorAlResetNoPuedeAbrirModulosNiEjecutarPost() throws Exception {
        for (String verb : new String[]{"GET", "POST"}) {
            Fixture f = new Fixture("/usuarios", verb, user(3), false);
            f.run(new AccessFilter(u -> u.credentialVersion() == 4L));
            assertTrue(f.invalidated);
            assertFalse(f.proceeded);
            assertEquals(CONTEXT + "/login", f.redirect);
        }
    }

    @Test void sesionConVersionVigenteConservaAcceso() throws Exception {
        Fixture f = new Fixture("/panel", "GET", user(4), false);
        f.run(new AccessFilter(u -> u.credentialVersion() == 4L));
        assertTrue(f.proceeded);
        assertFalse(f.invalidated);
    }

    @Test void otpPendienteAnteriorAlResetNoPuedeVerificarseNiReenviarse() throws Exception {
        for (String action : new String[]{"verificar", "reenviar"}) {
            Fixture f = new Fixture("/verificar-login", "POST", user(3), true);
            f.action = action;
            f.run(new AccessFilter(u -> u.credentialVersion() == 4L));
            assertTrue(f.invalidated);
            assertFalse(f.proceeded);
            assertEquals(CONTEXT + "/login", f.redirect);
        }
    }

    @Test void otpConVersionVigenteConservaFlujo() throws Exception {
        Fixture f = new Fixture("/verificar-login", "POST", user(4), true);
        f.run(new AccessFilter(u -> u.credentialVersion() == 4L));
        assertTrue(f.proceeded);
        assertFalse(f.invalidated);
    }

    @Test void falloConsultandoVersionNoPermiteAcceso() throws Exception {
        Fixture f = new Fixture("/panel", "GET", user(4), false);
        f.run(new AccessFilter(u -> { throw new SQLException("No disponible"); }));
        assertFalse(f.proceeded);
        assertEquals(503, f.status);
    }

    private static User user(long version) {
        return new User(20L, 5L, "Usuario", "Clínica", Role.ADMINISTRADOR, false, version);
    }
    private static final class Fixture {
        boolean invalidated, proceeded;
        String redirect, action = "verificar";
        int status;
        final Map<String, Object> attrs = new HashMap<>();
        final HttpServletRequest request;
        final HttpServletResponse response;
        Fixture(String path, String verb, User user, boolean pending) {
            if (pending) {
                Instant now = Instant.now();
                attrs.put("loginOtpChallenge", new LoginOtpChallenge(user, "prueba@odontosync.pe", "salt", "hash",
                    now, now.plusSeconds(300), now.plusSeconds(30), 0));
            } else attrs.put("user", user);
            HttpSession session = proxy(HttpSession.class, (obj, method, args) -> switch (method.getName()) {
                case "getAttribute" -> attrs.get(args[0]);
                case "invalidate" -> { invalidated = true; attrs.clear(); yield null; }
                default -> null;
            });
            request = proxy(HttpServletRequest.class, (obj, method, args) -> switch (method.getName()) {
                case "isSecure" -> true;
                case "getContextPath" -> CONTEXT;
                case "getRequestURI" -> CONTEXT + path;
                case "getServletPath" -> path;
                case "getMethod" -> verb;
                case "getSession" -> invalidated ? null : session;
                case "getParameter" -> "accion".equals(args[0]) ? action : null;
                case "getRemoteAddr" -> "127.0.0.1";
                default -> null;
            });
            response = proxy(HttpServletResponse.class, (obj, method, args) -> {
                if ("sendRedirect".equals(method.getName())) redirect = (String) args[0];
                if ("sendError".equals(method.getName()) || "setStatus".equals(method.getName())) status = (Integer) args[0];
                return null;
            });
        }
        void run(AccessFilter filter) throws Exception {
            FilterChain chain = (req, res) -> proceeded = true;
            filter.doFilter(request, response, chain);
        }
    }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
