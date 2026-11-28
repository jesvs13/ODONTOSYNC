package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.UsuarioCrudDAO;
import pe.odontosync.model.UsuarioDetalle;
import pe.odontosync.security.PasswordPolicy;
import pe.odontosync.security.Passwords;

class AdministrativePasswordResetTest {
    private static final long CLINIC = 5L;
    private static final long TARGET = 20L;

    @Test void restableceCuentasDeLosTresRolesConHashYClaveTemporal() throws Exception {
        for (Role role : Role.values()) {
            FakeDao dao = new FakeDao(role, "ACTIVO");
            try (CreatedAccount result = new UsuarioCrudService(dao).resetPassword(admin(), TARGET)) {
                assertEquals(TARGET, result.id());
                assertEquals(dao.target.correo(), result.correo());
                assertEquals(12, result.temporaryPassword().length);
                PasswordPolicy.validate(result.temporaryPassword(), false);
                assertTrue(Passwords.matches(result.temporaryPassword(), dao.hash));
                assertNotEquals(new String(result.temporaryPassword()), dao.hash);
                assertEquals(1, dao.writes);
                assertEquals(CLINIC, dao.writtenClinic);
                assertEquals(TARGET, dao.writtenUser);
            }
        }
    }

    @Test void noRestableceCuentaDeOtraClinica() {
        FakeDao dao = new FakeDao(Role.RECEPCIONISTA, "ACTIVO");
        User otherAdmin = new User(1L, 8L, "Admin", "Otra clínica", Role.ADMINISTRADOR);
        assertThrows(IllegalArgumentException.class,
            () -> new UsuarioCrudService(dao).resetPassword(otherAdmin, TARGET));
        assertEquals(0, dao.writes);
    }

    @Test void exigeAdministradorActivado() {
        FakeDao dao = new FakeDao(Role.ODONTOLOGO, "ACTIVO");
        for (User actor : List.of(
                new User(1L, CLINIC, "Recepción", "Clínica", Role.RECEPCIONISTA),
                new User(1L, CLINIC, "Dentista", "Clínica", Role.ODONTOLOGO),
                new User(1L, CLINIC, "Admin", "Clínica", Role.ADMINISTRADOR, true))) {
            assertThrows(IllegalArgumentException.class,
                () -> new UsuarioCrudService(dao).resetPassword(actor, TARGET));
        }
        assertEquals(0, dao.writes);
    }

    @Test void noRestableceLaPropiaCuentaAdministrativa() {
        FakeDao dao = new FakeDao(Role.ADMINISTRADOR, "ACTIVO");
        User self = new User(TARGET, CLINIC, "Admin", "Clínica", Role.ADMINISTRADOR);
        assertThrows(IllegalArgumentException.class,
            () -> new UsuarioCrudService(dao).resetPassword(self, TARGET));
        assertEquals(0, dao.writes);
    }

    @Test void noEntregaCredencialesSiLaCuentaDesapareceDuranteElReset() {
        FakeDao dao = new FakeDao(Role.RECEPCIONISTA, "ACTIVO");
        dao.updated = false;
        assertThrows(IllegalArgumentException.class,
            () -> new UsuarioCrudService(dao).resetPassword(admin(), TARGET));
        assertEquals(1, dao.writes);
    }

    @Test void propagaFalloDePersistenciaSinEntregarCredenciales() {
        FakeDao dao = new FakeDao(Role.RECEPCIONISTA, "ACTIVO");
        dao.fail = true;
        assertThrows(SQLException.class,
            () -> new UsuarioCrudService(dao).resetPassword(admin(), TARGET));
        assertEquals(1, dao.writes);
    }

    @Test void editarUsuarioNoPermiteAsignarClaveManual() {
        FakeDao dao = new FakeDao(Role.RECEPCIONISTA, "ACTIVO");
        assertThrows(IllegalArgumentException.class, () -> new UsuarioCrudService(dao).update(
            CLINIC, 1L, TARGET, "Usuario", "Prueba", "", "RECEPCIONISTA", "ACTIVO",
            "Manual123A".toCharArray(), ""));
        assertEquals(0, dao.writes);
    }

    private static User admin() {
        return new User(1L, CLINIC, "Admin", "Clínica", Role.ADMINISTRADOR);
    }

    private static final class FakeDao implements UsuarioCrudDAO {
        final UsuarioDetalle target;
        int writes;
        boolean updated = true, fail;
        String hash;
        long writtenClinic, writtenUser;
        FakeDao(Role role, String status) {
            target = new UsuarioDetalle(TARGET, "Usuario", "Prueba", "prueba@odontosync.pe", null,
                role, status, role == Role.ODONTOLOGO ? "COP-12345" : null);
        }
        public List<UsuarioDetalle> findDetailedByClinic(long c) { return List.of(); }
        public Optional<UsuarioDetalle> findDetailedById(long c, long id) {
            return c == CLINIC && id == TARGET ? Optional.of(target) : Optional.empty();
        }
        public boolean emailExistsGlobally(String e) { return false; }
        public long countActiveAdministrators(long c) { return 2; }
        public long create(long c, Role r, String n, String a, String e, String t, String h, String s, String dni) {
            throw new AssertionError("No se debe crear otra cuenta");
        }
        public void update(long c, long id, Role r, String n, String a, String e, String t, String h, String s, String col) {
            writes++;
        }
        public boolean delete(long c, long id) { throw new AssertionError("No se debe eliminar la cuenta"); }
        // Contrato GREEN: hash + cambio obligatorio + versión + revocación de tokens en una transacción.
        public boolean resetTemporaryPassword(long c, long id, String newHash) throws SQLException {
            writes++;
            if (fail) throw new SQLException("Fallo simulado");
            writtenClinic = c; writtenUser = id; hash = newHash;
            return updated;
        }
    }
}
