package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.UsuarioDAO;
import pe.odontosync.model.LoginAccount;
import pe.odontosync.model.UsuarioResumen;

class AuthServiceTest {
    private static final String ADMIN_HASH =
        "pbkdf2_sha256$310000$2WQz3TgkJq8dabYf5ylFxA==$ZVYDVwH1S8qZjNP8qI9Hv/HRJ7WXyNOOagpB2pDkS2c=";

    @Test
    void aceptaCredencialesValidasYActualizaUltimoAcceso() throws Exception {
        FakeUsuarioDAO dao = new FakeUsuarioDAO(List.of(account("ACTIVO", "ACTIVA", "ADMINISTRADOR", ADMIN_HASH)));
        AuthService service = new AuthService(dao);

        User user = service.authenticate("admin@odontosync.pe", "OdontoAdmin26!".toCharArray());

        assertNotNull(user);
        assertEquals(Role.ADMINISTRADOR, user.role());
        assertEquals(1L, dao.lastUpdatedUserId);
    }

    @Test
    void rechazaContrasenaIncorrecta() throws Exception {
        FakeUsuarioDAO dao = new FakeUsuarioDAO(List.of(account("ACTIVO", "ACTIVA", "ADMINISTRADOR", ADMIN_HASH)));
        AuthService service = new AuthService(dao);

        assertNull(service.authenticate("admin@odontosync.pe", "incorrecta".toCharArray()));
        assertEquals(0L, dao.lastUpdatedUserId);
    }

    @Test
    void rechazaUsuarioOClinicaInactiva() throws Exception {
        FakeUsuarioDAO dao = new FakeUsuarioDAO(List.of(account("INACTIVO", "ACTIVA", "ADMINISTRADOR", ADMIN_HASH)));
        AuthService service = new AuthService(dao);
        assertNull(service.authenticate("admin@odontosync.pe", "OdontoAdmin26!".toCharArray()));

        dao.accounts = List.of(account("ACTIVO", "INACTIVA", "ADMINISTRADOR", ADMIN_HASH));
        assertNull(service.authenticate("admin@odontosync.pe", "OdontoAdmin26!".toCharArray()));
    }

    @Test
    void rechazaAccesoAmbiguoEnDosClinicas() throws Exception {
        FakeUsuarioDAO dao = new FakeUsuarioDAO(new ArrayList<>(List.of(
            account("ACTIVO", "ACTIVA", "ADMINISTRADOR", ADMIN_HASH),
            new LoginAccount(2, 2, "Otro", "Admin", ADMIN_HASH, "ACTIVO", "ADMINISTRADOR", "Clínica 2", "ACTIVA")
        )));
        AuthService service = new AuthService(dao);

        assertNull(service.authenticate("admin@odontosync.pe", "OdontoAdmin26!".toCharArray()));
        assertEquals(0L, dao.lastUpdatedUserId);
    }

    private static LoginAccount account(String userStatus, String clinicStatus, String role, String hash) {
        return new LoginAccount(1, 1, "Admin", "Demo", hash, userStatus, role, "OdontoSync Demo", clinicStatus);
    }

    private static final class FakeUsuarioDAO implements UsuarioDAO {
        private List<LoginAccount> accounts;
        private long lastUpdatedUserId;

        private FakeUsuarioDAO(List<LoginAccount> accounts) {
            this.accounts = accounts;
        }

        @Override
        public List<LoginAccount> findLoginAccountsByEmail(String email) {
            return accounts;
        }

        @Override
        public void updateLastAccess(long userId) throws SQLException {
            lastUpdatedUserId = userId;
        }

        @Override
        public List<UsuarioResumen> findByClinic(long clinicId) {
            return List.of();
        }
    }
}
