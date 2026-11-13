package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.PasswordChangeDAO;
import pe.odontosync.security.Passwords;

class PasswordChangeServiceTest {
    @Test
    void cambiaPasswordCuandoDatosSonValidos() throws Exception {
        FakePasswordChangeDAO dao = new FakePasswordChangeDAO("Actual123");
        PasswordChangeService service = new PasswordChangeService(dao);

        service.change(user(), "Actual123".toCharArray(), "Nueva456".toCharArray(), "Nueva456".toCharArray());

        assertEquals(5, dao.updatedClinicId);
        assertEquals(2, dao.updatedUserId);
        assertTrue(Passwords.matches("Nueva456".toCharArray(), dao.updatedHash));
        assertFalse(Passwords.matches("Actual123".toCharArray(), dao.updatedHash));
    }

    @Test
    void rechazaPasswordActualIncorrecta() {
        FakePasswordChangeDAO dao = new FakePasswordChangeDAO("Actual123");
        PasswordChangeService service = new PasswordChangeService(dao);

        assertThrows(IllegalArgumentException.class,
            () -> service.change(user(), "Incorrecta9".toCharArray(),
                "Nueva456".toCharArray(), "Nueva456".toCharArray()));
        assertEquals(null, dao.updatedHash);
    }

    @Test
    void rechazaConfirmacionDiferente() {
        FakePasswordChangeDAO dao = new FakePasswordChangeDAO("Actual123");
        PasswordChangeService service = new PasswordChangeService(dao);

        assertThrows(IllegalArgumentException.class,
            () -> service.change(user(), "Actual123".toCharArray(),
                "Nueva456".toCharArray(), "Otra789A".toCharArray()));
        assertEquals(null, dao.updatedHash);
    }

    @Test
    void rechazaPasswordNuevaDebil() {
        FakePasswordChangeDAO dao = new FakePasswordChangeDAO("Actual123");
        PasswordChangeService service = new PasswordChangeService(dao);

        assertThrows(IllegalArgumentException.class,
            () -> service.change(user(), "Actual123".toCharArray(),
                "corta".toCharArray(), "corta".toCharArray()));
        assertEquals(null, dao.updatedHash);
    }

    @Test
    void rechazaReutilizarPasswordActual() {
        FakePasswordChangeDAO dao = new FakePasswordChangeDAO("Actual123");
        PasswordChangeService service = new PasswordChangeService(dao);

        assertThrows(IllegalArgumentException.class,
            () -> service.change(user(), "Actual123".toCharArray(),
                "Actual123".toCharArray(), "Actual123".toCharArray()));
        assertEquals(null, dao.updatedHash);
    }

    private static User user() {
        return new User(2, 5, "Recepción", "Clínica", Role.RECEPCIONISTA);
    }

    private static final class FakePasswordChangeDAO implements PasswordChangeDAO {
        private final String storedHash;
        long updatedClinicId;
        long updatedUserId;
        String updatedHash;

        FakePasswordChangeDAO(String currentPassword) {
            this.storedHash = Passwords.hash(currentPassword.toCharArray());
        }

        @Override
        public Optional<String> findPasswordHash(long clinicId, long userId) {
            return Optional.of(storedHash);
        }

        @Override
        public void updatePasswordHash(long clinicId, long userId, String passwordHash) {
            this.updatedClinicId = clinicId;
            this.updatedUserId = userId;
            this.updatedHash = passwordHash;
        }
    }
}
