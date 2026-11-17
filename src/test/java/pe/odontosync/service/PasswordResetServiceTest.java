package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.dao.PasswordResetDAO;
import pe.odontosync.model.PasswordResetAccount;
import pe.odontosync.model.PasswordResetResult;

class PasswordResetServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-12T20:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String TOKEN = "Abcdefghijklmnopqrstuvwxyz0123456789_-ABCDE";

    @Test
    void generaTokenYEnviaEnlaceParaCuentaActiva() throws Exception {
        FakeDAO dao = new FakeDAO();
        dao.accounts.add(new PasswordResetAccount(7, 2, "ana@demo.pe", "Ana", "Clinica Demo"));
        FakeMailer mailer = new FakeMailer();
        PasswordResetService service = service(dao, mailer);

        service.requestReset("  ANA@DEMO.PE  ", "http://localhost:8080/odontosync");

        assertEquals("ana@demo.pe", dao.lastEmailLookup);
        assertEquals(1, dao.saved.size());
        assertEquals(NOW.plus(TTL), dao.saved.get(0).expiresAt);
        assertEquals(1, mailer.links.size());
        assertTrue(mailer.links.get(0).contains("/restablecer-password?token="));
        assertFalse(mailer.links.get(0).contains(dao.saved.get(0).tokenHash));
    }

    @Test
    void invalidaTokensAnterioresAntesDeCrearUnoNuevo() throws Exception {
        FakeDAO dao = new FakeDAO();
        dao.accounts.add(new PasswordResetAccount(7, 2, "ana@demo.pe", "Ana", "Clinica Demo"));
        PasswordResetService service = service(dao, new FakeMailer());

        service.requestReset("ana@demo.pe", "http://localhost:8080/odontosync");

        assertEquals(List.of(7L), dao.invalidatedUsers);
    }

    @Test
    void correoInexistenteNoEnviaCorreo() throws Exception {
        FakeDAO dao = new FakeDAO();
        FakeMailer mailer = new FakeMailer();
        PasswordResetService service = service(dao, mailer);

        service.requestReset("nadie@demo.pe", "http://localhost:8080/odontosync");

        assertTrue(dao.saved.isEmpty());
        assertTrue(mailer.links.isEmpty());
    }

    @Test
    void correoInvalidoNoConsultaBaseDeDatos() throws Exception {
        FakeDAO dao = new FakeDAO();
        PasswordResetService service = service(dao, new FakeMailer());

        service.requestReset("correo-invalido", "http://localhost:8080/odontosync");

        assertEquals(0, dao.lookupCount);
    }

    @Test
    void reconoceTokenValido() throws Exception {
        FakeDAO dao = new FakeDAO();
        dao.usable = true;
        PasswordResetService service = service(dao, new FakeMailer());

        assertTrue(service.isTokenUsable(TOKEN));
        assertNotNull(dao.lastUsabilityHash);
        assertEquals(64, dao.lastUsabilityHash.length());
    }

    @Test
    void rechazaTokenConFormatoInvalido() throws Exception {
        FakeDAO dao = new FakeDAO();
        PasswordResetService service = service(dao, new FakeMailer());

        assertFalse(service.isTokenUsable("token-corto"));
        assertEquals(0, dao.usabilityCount);
    }

    @Test
    void rechazaPasswordDebil() {
        PasswordResetService service = service(new FakeDAO(), new FakeMailer());

        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(
            TOKEN,
            "debil".toCharArray(),
            "debil".toCharArray()
        ));
    }

    private PasswordResetService service(FakeDAO dao, FakeMailer mailer) {
        return new PasswordResetService(dao, mailer, () -> TOKEN, CLOCK, TTL);
    }

    private static final class FakeMailer implements PasswordResetMailer {
        private final List<String> links = new ArrayList<>();

        @Override
        public void sendResetLink(PasswordResetAccount account, String resetLink) {
            links.add(resetLink);
        }
    }

    private static final class FakeDAO implements PasswordResetDAO {
        private final List<PasswordResetAccount> accounts = new ArrayList<>();
        private final List<Long> invalidatedUsers = new ArrayList<>();
        private final List<SavedToken> saved = new ArrayList<>();
        private int lookupCount;
        private String lastEmailLookup;
        private boolean usable;
        private int usabilityCount;
        private String lastUsabilityHash;
        private Optional<PasswordResetResult> consumeResult = Optional.empty();
        private String lastNewPasswordHash;

        @Override
        public List<PasswordResetAccount> findActiveAccountsByEmail(String email) {
            lookupCount++;
            lastEmailLookup = email;
            return List.copyOf(accounts);
        }

        @Override
        public void invalidateUnusedTokens(long userId, Instant usedAt) {
            invalidatedUsers.add(userId);
        }

        @Override
        public void saveToken(long userId, long clinicId, String tokenHash, Instant expiresAt) {
            saved.add(new SavedToken(userId, clinicId, tokenHash, expiresAt));
        }

        @Override
        public boolean isTokenUsable(String tokenHash, Instant now) {
            usabilityCount++;
            lastUsabilityHash = tokenHash;
            return usable;
        }

        @Override
        public Optional<PasswordResetResult> consumeTokenAndUpdatePassword(
            String tokenHash,
            String newPasswordHash,
            Instant now
        ) throws SQLException {
            lastNewPasswordHash = newPasswordHash;
            return consumeResult;
        }
    }

    private record SavedToken(long userId, long clinicId, String tokenHash, Instant expiresAt) { }
}
