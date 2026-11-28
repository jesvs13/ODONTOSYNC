package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.UsuarioDAO;
import pe.odontosync.model.LoginAccount;
import pe.odontosync.model.UsuarioResumen;
import pe.odontosync.security.Passwords;

class AuthActivationTest {
    private static final String EMAIL = "prueba@odontosync.pe";
    private static final String HASH = Passwords.hash("Temporal123A".toCharArray());

    @Test
    void primerFactorConservaCambioObligatorioParaTodosLosRoles() throws Exception {
        for (Role role : Role.values()) {
            FakeDao dao = new FakeDao(account(role, "ACTIVO", true));
            User user = new AuthService(dao).verifyFirstFactor(EMAIL, "Temporal123A".toCharArray());
            assertNotNull(user);
            assertTrue(user.mustChangePassword());
            assertEquals(role, user.role());
            assertEquals(5L, user.clinicId());
            assertEquals(0L, dao.lastAccess);
        }
    }

    @Test
    void cuentaExistenteSinCambioPendienteConservaFlujoNormal() throws Exception {
        FakeDao dao = new FakeDao(account(Role.RECEPCIONISTA, "ACTIVO", false));
        User user = new AuthService(dao).verifyFirstFactor(EMAIL, "Temporal123A".toCharArray());
        assertNotNull(user);
        assertFalse(user.mustChangePassword());
    }

    @Test
    void cuentaPendienteNoEludeValidacionDeCredencialesNiEstado() throws Exception {
        FakeDao dao = new FakeDao(account(Role.ADMINISTRADOR, "BLOQUEADO", true));
        assertNull(new AuthService(dao).verifyFirstFactor(EMAIL, "Temporal123A".toCharArray()));
        dao.account = account(Role.ADMINISTRADOR, "ACTIVO", true);
        assertNull(new AuthService(dao).verifyFirstFactor(EMAIL, "Incorrecta9".toCharArray()));
        assertEquals(0L, dao.lastAccess);
    }

    private static LoginAccount account(Role role, String status, boolean required) {
        return new LoginAccount(2L, 5L, "Usuario", "Prueba", HASH, status,
            role.name(), "Clínica de prueba", "ACTIVA", required);
    }

    private static final class FakeDao implements UsuarioDAO {
        private LoginAccount account;
        private long lastAccess;
        FakeDao(LoginAccount account) { this.account = account; }
        public List<LoginAccount> findLoginAccountsByEmail(String email) { return List.of(account); }
        public void updateLastAccess(long userId) { lastAccess = userId; }
        public List<UsuarioResumen> findByClinic(long clinicId) { return List.of(); }
    }
}
