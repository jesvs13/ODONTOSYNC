package pe.odontosync.security;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;

class AccessFilterActivationTest {
    private static final String CONTEXT = "/odontosync";

    @Test
    void bloqueaPanelYModulosPorUrlDirectaParaTodosLosRoles() throws Exception {
        for (Role role : Role.values()) {
            for (String path : new String[]{"/panel", "/usuarios", "/agenda", "/historias",
                "/modulo/recetas/pdf", "/modulo/placas/archivo", "/reportes/exportar-citas.xlsx"}) {
                Result result = filter(user(role, true), path, path, "GET");
                assertFalse(result.proceeded(), role + " " + path);
                assertEquals(CONTEXT + "/cambiar-password", result.redirect());
            }
        }
    }

    @Test
    void bloqueaOperacionesPostAntesDelCambio() throws Exception {
        Result result = filter(user(Role.ADMINISTRADOR, true), "/usuarios", "/usuarios", "POST");
        assertFalse(result.proceeded());
        assertEquals(CONTEXT + "/cambiar-password", result.redirect());
    }

    @Test
    void permiteCambiarPasswordYCerrarSesionSinBucle() throws Exception {
        User pending = user(Role.ODONTOLOGO, true);
        assertTrue(filter(pending, "/cambiar-password", "/cambiar-password", "GET").proceeded());
        assertTrue(filter(pending, "/cambiar-password", "/cambiar-password", "POST").proceeded());
        assertTrue(filter(pending, "/logout", "/logout", "POST").proceeded());
    }

    @Test
    void conservaAccesoNormalCuandoNoHayCambioPendiente() throws Exception {
        assertTrue(filter(user(Role.RECEPCIONISTA, false), "/panel", "/panel", "GET").proceeded());
    }

    @Test
    void sinSegundoFactorNoPuedeEntrarAlCambioDePassword() throws Exception {
        Result result = filter(null, "/cambiar-password", "/cambiar-password", "GET");
        assertFalse(result.proceeded());
        assertEquals(CONTEXT + "/login", result.redirect());
    }

    @Test
    void parametrosDeRutaNoEludenLaRestriccion() throws Exception {
        Result result = filter(user(Role.ADMINISTRADOR, true), "/panel;jsessionid=prueba", "/panel", "GET");
        assertFalse(result.proceeded());
        assertEquals(CONTEXT + "/cambiar-password", result.redirect());
    }

    private static User user(Role role, boolean required) {
        return new User(2L, 5L, "Usuario Prueba", "Clínica de prueba", role, required);
    }

    private static Result filter(User user, String uri, String mappedPath, String verb) throws Exception {
        HttpSession session = proxy(HttpSession.class, (obj, method, args) ->
            "getAttribute".equals(method.getName()) && "user".equals(args[0]) ? user : null);
        String servletPath = mappedPath.startsWith("/modulo/") ? "/modulo" : mappedPath;
        String pathInfo = mappedPath.startsWith("/modulo/") ? mappedPath.substring("/modulo".length()) : null;
        HttpServletRequest request = proxy(HttpServletRequest.class, (obj, method, args) -> switch (method.getName()) {
            case "isSecure" -> true;
            case "getContextPath" -> CONTEXT;
            case "getRequestURI" -> CONTEXT + uri;
            case "getServletPath" -> servletPath;
            case "getPathInfo" -> pathInfo;
            case "getSession" -> session;
            case "getMethod" -> verb;
            case "getRemoteAddr" -> "127.0.0.1";
            default -> null;
        });
        AtomicReference<String> redirect = new AtomicReference<>();
        HttpServletResponse response = proxy(HttpServletResponse.class, (obj, method, args) -> {
            if ("sendRedirect".equals(method.getName())) redirect.set((String) args[0]);
            return null;
        });
        AtomicBoolean proceeded = new AtomicBoolean();
        FilterChain chain = (req, res) -> proceeded.set(true);
        new AccessFilter(ignored -> true).doFilter(request, response, chain);
        return new Result(proceeded.get(), redirect.get());
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private record Result(boolean proceeded, String redirect) { }
}
