package pe.odontosync.controller;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.PasswordChangeDAO;
import pe.odontosync.security.LoginSessions;
import pe.odontosync.security.Passwords;
import pe.odontosync.service.PasswordChangeService;

class CambioPasswordActivationTest {
    private static final String CONTEXT = "/odontosync";

    @Test void destinoDespuesDelOtpDependeDelEstadoParaTodosLosRoles() {
        for (Role role : Role.values()) {
            assertEquals("/cambiar-password", LoginSessions.destination(user(role, true)));
            assertEquals("/panel", LoginSessions.destination(user(role, false)));
        }
    }

    @Test void muestraVistaDeActivacionSinUsarElFormularioHabitual() throws Exception {
        Fixture f = new Fixture(user(Role.ODONTOLOGO, true));
        f.servlet.doGet(f.request, f.response);
        assertEquals("/WEB-INF/views/activar-cuenta.jsp", f.view);
        assertEquals("csrf-original", f.attributes.get("csrf"));
        Fixture normal = new Fixture(user(Role.ADMINISTRADOR, false));
        normal.servlet.doGet(normal.request, normal.response);
        assertEquals("/WEB-INF/views/cambiar-password.jsp", normal.view);
    }

    @Test void cambioExitosoRenuevaSesionYConservaIdentidadYRol() throws Exception {
        for (Role role : Role.values()) {
            User pending = user(role, true);
            Fixture f = new Fixture(pending);
            HttpSession old = f.session;
            f.servlet.doPost(f.request, f.response);
            assertEquals(1, f.dao.writes);
            assertTrue(Passwords.matches("NuevaClave2026".toCharArray(), f.dao.hash));
            assertEquals(new User(pending.id(), pending.clinicId(), pending.name(), pending.clinicName(), role, false), f.sessionData.get("user"));
            assertNotEquals(old, f.session);
            assertNotEquals("csrf-original", f.sessionData.get("csrf"));
            assertEquals(CONTEXT + "/panel", f.redirect);
        }
    }

    @Test void activacionConservaVersionDeCredencialAlRenovarSesion() throws Exception {
        User pending = new User(7L, 2L, "Usuario Prueba", "Clínica Prueba", Role.ODONTOLOGO, true, 9L);
        Fixture f = new Fixture(pending);
        f.servlet.doPost(f.request, f.response);
        User activated = (User) f.sessionData.get("user");
        assertNotNull(activated);
        assertFalse(activated.mustChangePassword());
        assertEquals(9L, activated.credentialVersion());
        assertEquals(CONTEXT + "/panel", f.redirect);
    }

    @Test void claveIncorrectaNoActivaLaCuenta() throws Exception {
        Fixture f = new Fixture(user(Role.ODONTOLOGO, true));
        f.params.put("clave_actual", "incorrecta");
        f.servlet.doPost(f.request, f.response);
        assertEquals(0, f.dao.writes);
        assertTrue(((User) f.sessionData.get("user")).mustChangePassword());
        assertNotNull(f.sessionData.get("passwordError"));
        assertEquals(CONTEXT + "/cambiar-password", f.redirect);
    }

    @Test void falloDePersistenciaNoActivaLaCuenta() throws Exception {
        Fixture f = new Fixture(user(Role.RECEPCIONISTA, true));
        f.dao.failUpdate = true;
        f.servlet.doPost(f.request, f.response);
        assertEquals(0, f.dao.writes);
        assertTrue(((User) f.sessionData.get("user")).mustChangePassword());
        assertNotNull(f.sessionData.get("passwordError"));
        assertEquals(CONTEXT + "/cambiar-password", f.redirect);
    }

    @Test void csrfInvalidoNoPermiteCambiarContraseña() throws Exception {
        Fixture f = new Fixture(user(Role.ADMINISTRADOR, true));
        f.params.put("csrf", "falso");
        f.servlet.doPost(f.request, f.response);
        assertEquals(403, f.status);
        assertEquals(0, f.dao.writes);
        assertTrue(((User) f.sessionData.get("user")).mustChangePassword());
    }

    @Test void cambioHabitualMantieneFlujoExistente() throws Exception {
        Fixture f = new Fixture(user(Role.ADMINISTRADOR, false));
        f.servlet.doPost(f.request, f.response);
        assertEquals(1, f.dao.writes);
        assertTrue(f.rotated);
        assertNotNull(f.sessionData.get("passwordSuccess"));
        assertEquals(CONTEXT + "/cambiar-password", f.redirect);
    }

    @Test void sinSesionAutenticadaNoPermiteActivacion() throws Exception {
        Fixture f = new Fixture(null);
        f.servlet.doPost(f.request, f.response);
        assertEquals(0, f.dao.writes);
        assertEquals(CONTEXT + "/login", f.redirect);
    }

    private static User user(Role role, boolean pending) {
        return new User(7, 2, "Usuario Prueba", "Clínica Prueba", role, pending);
    }

    private static final class FakeDao implements PasswordChangeDAO {
        String hash = Passwords.hash("Temporal2026".toCharArray());
        int writes;
        boolean failUpdate;
        public Optional<String> findPasswordHash(long clinicId, long userId) {
            assertEquals(2L, clinicId);
            assertEquals(7L, userId);
            return Optional.of(hash);
        }
        public void updatePasswordHashAndClearRequirement(long clinicId, long userId,
                String expectedHash, String newHash) throws SQLException {
            assertEquals(2L, clinicId);
            assertEquals(7L, userId);
            assertEquals(hash, expectedHash);
            if (failUpdate) throw new SQLException("Fallo de prueba");
            hash = newHash;
            writes++;
        }
    }

    private static final class Fixture {
        final FakeDao dao = new FakeDao();
        final CambioPasswordServlet servlet = new CambioPasswordServlet(new PasswordChangeService(dao));
        final Map<String, Object> sessionData = new HashMap<>();
        final Map<String, Object> attributes = new HashMap<>();
        final Map<String, String> params = new HashMap<>(Map.of("csrf", "csrf-original",
            "clave_actual", "Temporal2026", "clave_nueva", "NuevaClave2026", "clave_confirmacion", "NuevaClave2026"));
        HttpSession session;
        String redirect;
        String view;
        boolean rotated;
        int status;
        final HttpServletRequest request;
        final HttpServletResponse response;

        Fixture(User user) {
            if (user != null) {
                session = newSession();
                sessionData.put("user", user);
                sessionData.put("csrf", "csrf-original");
            }
            request = proxy(HttpServletRequest.class, (obj, method, args) -> switch (method.getName()) {
                case "getSession" -> {
                    if (session == null && (args == null || Boolean.TRUE.equals(args[0]))) session = newSession();
                    yield session;
                }
                case "getContextPath" -> CONTEXT;
                case "getRemoteAddr" -> "127.0.0.1";
                case "getParameter" -> params.get(args[0]);
                case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
                case "getRequestDispatcher" -> {
                    view = (String) args[0];
                    yield proxy(RequestDispatcher.class, (o, m, a) -> null);
                }
                case "changeSessionId" -> { rotated = true; yield "rotated"; }
                default -> null;
            });
            response = proxy(HttpServletResponse.class, (obj, method, args) -> {
                if ("sendRedirect".equals(method.getName())) redirect = (String) args[0];
                if ("sendError".equals(method.getName())) status = (Integer) args[0];
                return null;
            });
        }
        HttpSession newSession() {
            return proxy(HttpSession.class, (obj, method, args) -> switch (method.getName()) {
                case "getAttribute" -> sessionData.get(args[0]);
                case "setAttribute" -> { sessionData.put((String) args[0], args[1]); yield null; }
                case "removeAttribute" -> { sessionData.remove(args[0]); yield null; }
                case "invalidate" -> { sessionData.clear(); session = null; yield null; }
                case "equals" -> obj == args[0];
                case "hashCode" -> System.identityHashCode(obj);
                case "toString" -> "Test session";
                default -> null;
            });
        }
    }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
