package pe.odontosync.controller;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.UsuarioCrudDAO;
import pe.odontosync.model.UsuarioDetalle;
import pe.odontosync.security.Passwords;
import pe.odontosync.service.CreatedAccount;
import pe.odontosync.service.UsuarioCrudService;

class UsuarioPasswordResetTest {
    @Test void entregaCredencialSoloEnRespuestaYBorraLaClaveAlTerminar() throws Exception {
        Fixture f = new Fixture(Role.ADMINISTRADOR);
        f.post();
        assertEquals(1, f.dao.writes);
        assertEquals("/WEB-INF/views/credenciales-alta.jsp", f.view);
        assertTrue(f.resetView);
        assertTrue(f.passwordMatches);
        assertEquals(12, f.delivered.length);
        for (char ch : f.delivered) assertEquals('\0', ch);
        assertTrue(f.headers.get("Cache-Control").contains("no-store"));
        assertFalse(f.sessionData.containsKey("altaNonce:usuarios"));
        assertFalse(f.sessionData.values().stream().anyMatch(CreatedAccount.class::isInstance));
        assertNull(f.attrs.get("createdAccount"));
        assertNull(f.attrs.get("credentialReset"));
        assertNull(f.redirect);
    }
    @Test void reenviarFormularioNoRestableceOtraVez() throws Exception {
        Fixture f = new Fixture(Role.ADMINISTRADOR);
        f.post();
        f.post();
        assertEquals(1, f.dao.writes);
        assertEquals("/odontosync/usuarios", f.redirect);
        assertNotNull(f.sessionData.get("crudError"));
    }
    @Test void csrfIncorrectoNoRestablece() throws Exception {
        Fixture f = new Fixture(Role.ADMINISTRADOR);
        f.params.put("csrf", "incorrecto");
        f.post();
        assertEquals(403, f.status);
        assertEquals(0, f.dao.writes);
        assertNull(f.delivered);
    }
    @Test void recepcionNoPuedeRestablecer() throws Exception {
        Fixture f = new Fixture(Role.RECEPCIONISTA);
        f.post();
        assertEquals(403, f.status);
        assertEquals(0, f.dao.writes);
    }
    @Test void noEntregaCredencialAnteFalloDePersistencia() throws Exception {
        Fixture f = new Fixture(Role.ADMINISTRADOR);
        f.dao.fail = true;
        f.post();
        assertEquals(1, f.dao.writes);
        assertNull(f.delivered);
        assertEquals("/odontosync/usuarios", f.redirect);
        assertNotNull(f.sessionData.get("crudError"));
    }
    @Test void noAceptaClaveManualEnRestablecimiento() throws Exception {
        Fixture f = new Fixture(Role.ADMINISTRADOR);
        f.params.put("clave", "Manual2026A");
        f.post();
        assertEquals(0, f.dao.writes);
        assertNull(f.delivered);
        assertNotNull(f.sessionData.get("crudError"));
    }

    @Test void modalEntregaCredencialSinNavegarYRenuevaNonce() throws Exception {
        Fixture f = new Fixture(Role.ADMINISTRADOR);
        f.params.put("respuesta", "modal");
        f.post();
        Map<String, String> fields = f.responseFields();
        assertEquals(1, f.dao.writes);
        assertEquals("prueba@odontosync.pe", fields.get("correo"));
        assertTrue(Passwords.matches(fields.get("clave").toCharArray(), f.dao.hash));
        assertEquals(12, fields.get("clave").length());
        assertEquals(fields.get("nonce"), f.sessionData.get("altaNonce:usuarios"));
        assertFalse("nonce-original".equals(fields.get("nonce")));
        assertTrue(f.headers.get("Cache-Control").contains("no-store"));
        assertEquals("1", f.headers.get("X-OdontoSync-Reset"));
        assertNull(f.redirect);
        assertNull(f.view);
        assertFalse(f.sessionData.containsValue(fields.get("clave")));
        f.response.getBuffer().setLength(0);
        f.post();
        assertEquals(400, f.status);
        assertEquals(1, f.dao.writes);
        assertNull(f.responseFields().get("clave"));
        f.params.put("alta_nonce", fields.get("nonce"));
        f.response.getBuffer().setLength(0);
        f.post();
        assertEquals(2, f.dao.writes);
        assertNotNull(f.responseFields().get("clave"));
    }
    @Test void modalInformaFalloSinEntregarCredencial() throws Exception {
        Fixture f = new Fixture(Role.ADMINISTRADOR);
        f.params.put("respuesta", "modal");
        f.dao.fail = true;
        f.post();
        assertEquals(503, f.status);
        assertNotNull(f.responseFields().get("error"));
        assertNull(f.responseFields().get("clave"));
        assertNull(f.redirect);
        assertTrue(f.headers.get("Cache-Control").contains("no-store"));
    }
    @Test void modalNoOmiteProteccionesDeCsrfNiRol() throws Exception {
        Fixture f = new Fixture(Role.ADMINISTRADOR);
        f.params.put("respuesta", "modal");
        f.params.put("csrf", "incorrecto");
        f.post();
        assertEquals(403, f.status);
        assertEquals(0, f.dao.writes);
        Fixture other = new Fixture(Role.RECEPCIONISTA);
        other.params.put("respuesta", "modal");
        other.post();
        assertEquals(403, other.status);
        assertEquals(0, other.dao.writes);
    }

    private static final class FakeDao implements UsuarioCrudDAO {
        int writes;
        boolean fail;
        String hash;
        public List<UsuarioDetalle> findDetailedByClinic(long c) { return List.of(); }
        public Optional<UsuarioDetalle> findDetailedById(long c, long id) {
            return c == 2L && id == 20L ? Optional.of(new UsuarioDetalle(20L, "Usuario", "Prueba",
                "prueba@odontosync.pe", null, Role.ODONTOLOGO, "ACTIVO", "COP-12345")) : Optional.empty();
        }
        public boolean emailExistsGlobally(String e) { return false; }
        public long countActiveAdministrators(long c) { return 2; }
        public long create(long c, Role r, String n, String a, String e, String t, String h, String s, String dni) {
            throw new AssertionError("No crear otra cuenta");
        }
        public void update(long c, long id, Role r, String n, String a, String e, String t, String h, String s, String col) {
            throw new AssertionError("No editar perfil");
        }
        public boolean delete(long c, long id) { throw new AssertionError("No eliminar cuenta"); }
        public boolean resetTemporaryPassword(long c, long id, String h) throws SQLException {
            assertEquals(2L, c); assertEquals(20L, id);
            writes++;
            if (fail) throw new SQLException("Fallo simulado");
            hash = h; return true;
        }
    }
    private static final class Fixture {
        final FakeDao dao = new FakeDao();
        final UsuarioServlet servlet = new UsuarioServlet(new UsuarioCrudService(dao));
        final Map<String, Object> sessionData = new HashMap<>(), attrs = new HashMap<>();
        final Map<String, String> headers = new HashMap<>();
        final Map<String, String> params = new HashMap<>(Map.of("csrf", "csrf-original", "alta_nonce", "nonce-original",
            "accion", "restablecer-password", "id_usuario", "20"));
        final HttpServletRequest req;
        final HttpServletResponse res;
        String redirect, view;
        int status;
        final StringWriter response = new StringWriter();
        boolean resetView, passwordMatches;
        char[] delivered;
        Fixture(Role role) {
            sessionData.put("user", new User(7L, 2L, "Usuario Prueba", "Clínica Prueba", role));
            sessionData.put("csrf", "csrf-original");
            sessionData.put("altaNonce:usuarios", "nonce-original");
            HttpSession session = proxy(HttpSession.class, (o, m, a) -> switch (m.getName()) {
                case "getAttribute" -> sessionData.get(a[0]);
                case "setAttribute" -> { sessionData.put((String) a[0], a[1]); yield null; }
                case "removeAttribute" -> { sessionData.remove(a[0]); yield null; }
                default -> null;
            });
            req = proxy(HttpServletRequest.class, (o, m, a) -> switch (m.getName()) {
                case "getSession" -> session;
                case "getContextPath" -> "/odontosync";
                case "getParameter" -> params.get(a[0]);
                case "setAttribute" -> { attrs.put((String) a[0], a[1]); yield null; }
                case "removeAttribute" -> { attrs.remove(a[0]); yield null; }
                case "getAttribute" -> attrs.get(a[0]);
                case "getRequestDispatcher" -> {
                    view = (String) a[0];
                    yield proxy(RequestDispatcher.class, (d, dm, da) -> {
                        if ("forward".equals(dm.getName())) {
                            CreatedAccount account = (CreatedAccount) attrs.get("createdAccount");
                            assertNotNull(account);
                            assertEquals("prueba@odontosync.pe", account.correo());
                            delivered = account.temporaryPassword();
                            passwordMatches = Passwords.matches(delivered, dao.hash);
                            resetView = Boolean.TRUE.equals(attrs.get("credentialReset"));
                        }
                        return null;
                    });
                }
                default -> null;
            });
            res = proxy(HttpServletResponse.class, (o, m, a) -> {
                switch (m.getName()) {
                    case "sendRedirect" -> redirect = (String) a[0];
                    case "sendError", "setStatus" -> status = (Integer) a[0];
                    case "getWriter" -> { return new PrintWriter(response); }
                    case "setHeader" -> headers.put((String) a[0], (String) a[1]);
                    default -> { }
                }
                return null;
            });
        }
        void post() throws Exception { servlet.doPost(req, res); }
        Map<String, String> responseFields() {
            Map<String, String> result = new HashMap<>();
            for (String field : response.toString().split("&")) {
                String[] pair = field.split("=", 2);
                if (pair.length == 2) result.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
            return result;
        }
    }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
