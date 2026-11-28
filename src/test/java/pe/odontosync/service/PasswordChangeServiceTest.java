package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
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

    @Test
    void cambioValidoGuardaHashYQuitaObligacionEnUnaOperacion() throws Exception {
        FakePasswordChangeDAO dao = new FakePasswordChangeDAO("Actual123");
        PasswordChangeService service = new PasswordChangeService(dao);
        service.change(new User(2, 5, "Recepción", "Clínica", Role.RECEPCIONISTA, true),
            "Actual123".toCharArray(), "Nueva456".toCharArray(), "Nueva456".toCharArray());
        assertFalse(dao.mustChangePassword);
        assertEquals(5L, dao.updatedClinicId);
        assertEquals(2L, dao.updatedUserId);
        assertTrue(Passwords.matches("Nueva456".toCharArray(), dao.updatedHash));
    }

    @Test
    void cambioRechazadoMantieneObligacion() {
        FakePasswordChangeDAO dao = new FakePasswordChangeDAO("Actual123");
        PasswordChangeService service = new PasswordChangeService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.change(
            new User(2, 5, "Recepción", "Clínica", Role.RECEPCIONISTA, true),
            "Incorrecta9".toCharArray(), "Nueva456".toCharArray(), "Nueva456".toCharArray()));
        assertTrue(dao.mustChangePassword);
        assertEquals(null, dao.updatedHash);
    }

    @Test
    void cambioConcurrenteNoSobrescribeUnaNuevaCredencial() {
        FakePasswordChangeDAO dao = new FakePasswordChangeDAO("Actual123");
        dao.concurrentChange = true;
        PasswordChangeService service = new PasswordChangeService(dao);
        assertThrows(SQLException.class, () -> service.change(user(), "Actual123".toCharArray(),
            "Nueva456".toCharArray(), "Nueva456".toCharArray()));
        assertTrue(dao.mustChangePassword);
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
        boolean mustChangePassword = true;
        boolean concurrentChange;

        FakePasswordChangeDAO(String currentPassword) {
            this.storedHash = Passwords.hash(currentPassword.toCharArray());
        }

        @Override
        public Optional<String> findPasswordHash(long clinicId, long userId) {
            return Optional.of(storedHash);
        }

        // Contrato anterior: no garantiza limpiar el estado ni proteger cambios concurrentes.
        public void updatePasswordHash(long clinicId, long userId, String passwordHash) {
            throw new AssertionError("El servicio debe actualizar el hash y la obligación juntos");
        }

        // Nuevo contrato RED: una sola operación condicionada al hash que se verificó.
        public void updatePasswordHashAndClearRequirement(long clinicId, long userId,
            String expectedHash, String passwordHash) throws SQLException {
            if (concurrentChange || !storedHash.equals(expectedHash)) {
                throw new SQLException("La credencial cambió durante la operación");
            }
            this.mustChangePassword = false;
            this.updatedClinicId = clinicId;
            this.updatedUserId = userId;
            this.updatedHash = passwordHash;
        }
    }
}
