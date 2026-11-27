package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.dao.UsuarioCrudDAO;
import pe.odontosync.model.UsuarioDetalle;
import pe.odontosync.security.Passwords;

class UsuarioCrudServiceTest {
    @Test
    void creaRecepcionistaConPasswordHasheado() throws Exception {
        FakeDao dao = new FakeDao();
        UsuarioCrudService service = new UsuarioCrudService(dao);
        char[] password = "Recep2026!".toCharArray();

        var created = service.create(1, "Maria", "Lopez", "Diaz", "999111222",
            "RECEPCIONISTA", "ACTIVO", password, "");

        assertEquals(10L, created.id());
        assertEquals("mlopez@odontosync.pe", created.correo());
        assertEquals(Role.RECEPCIONISTA, dao.createdRole);
        assertEquals("mlopez@odontosync.pe", dao.createdEmail);
        assertTrue(Passwords.matches("Recep2026!".toCharArray(), dao.createdHash));
    }

    @Test
    void altaOdontologoSeRealizaSoloDesdeDirectorioProfesional() {
        UsuarioCrudService service = new UsuarioCrudService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.create(
            1, "Carlos", "Ramos", "Vega", "999111222",
            "ODONTOLOGO", "ACTIVO", "Dentista2026!".toCharArray(), "12345"
        ));
    }

    @Test
    void generaAlternativaCuandoCorreoExisteEnOtraClinica() throws Exception {
        FakeDao dao = new FakeDao();
        dao.occupied.add("mlopez@odontosync.pe");
        UsuarioCrudService service = new UsuarioCrudService(dao);
        var created = service.create(2, "Maria", "Lopez", "Diaz", "999111222",
            "RECEPCIONISTA", "ACTIVO", "Recep2026!".toCharArray(), "");
        assertEquals("mlopezd@odontosync.pe", created.correo());
        assertEquals("Lopez Diaz", dao.createdSurnames);
    }

    @Test
    void reintentaSiOtroRegistroOcupaElCorreo() throws Exception {
        FakeDao dao = new FakeDao();
        dao.race = true;
        var created = new UsuarioCrudService(dao).create(1, "Maria", "Lopez", "Diaz", "999111222",
            "RECEPCIONISTA", "ACTIVO", "Recep2026!".toCharArray(), "");
        assertEquals("mlopezd@odontosync.pe", created.correo());
    }

    @Test
    void sugiereCorreoSinCrearCuenta() throws Exception {
        FakeDao dao = new FakeDao();
        dao.occupied.add("mlopez@odontosync.pe");
        assertEquals("mlopezd@odontosync.pe", new UsuarioCrudService(dao)
            .suggestEmail("Maria", "Lopez", "Diaz"));
        assertEquals(null, dao.createdEmail);
    }

    @Test
    void noReintentaErrorDeIntegridadAjenoAlCorreo() {
        FakeDao dao = new FakeDao();
        dao.otherDuplicate = true;
        SQLException error = assertThrows(SQLException.class, () -> new UsuarioCrudService(dao)
            .create(1, "Maria", "Lopez", "Diaz", "999111222", "RECEPCIONISTA", "ACTIVO",
                "Recep2026!".toCharArray(), ""));
        assertEquals(1062, error.getErrorCode());
        assertEquals(1, dao.createAttempts);
    }

    @Test
    void editarConservaCorreoExistente() throws Exception {
        FakeDao dao = new FakeDao();
        dao.users.add(new UsuarioDetalle(2, "Maria", "Lopez", "original@odontosync.pe", null,
            Role.RECEPCIONISTA, "ACTIVO", null));
        new UsuarioCrudService(dao).update(1, 1, 2, "Maria Elena", "Vega Diaz", "",
            "RECEPCIONISTA", "ACTIVO", new char[0], "");
        assertEquals("original@odontosync.pe", dao.updatedEmail);
    }

    @Test
    void administradorNoPuedeDesactivarSuPropiaCuenta() {
        FakeDao dao = new FakeDao();
        dao.users.add(new UsuarioDetalle(1, "Admin", "Demo", "admin@o.pe", "999", Role.ADMINISTRADOR, "ACTIVO", null));
        UsuarioCrudService service = new UsuarioCrudService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.update(
            1, 1, 1, "Admin", "Demo", "999",
            "ADMINISTRADOR", "INACTIVO", new char[0], ""
        ));
    }

    @Test
    void noEliminaUltimoAdministradorActivo() {
        FakeDao dao = new FakeDao();
        dao.users.add(new UsuarioDetalle(2, "Otro", "Admin", "otro@o.pe", null, Role.ADMINISTRADOR, "ACTIVO", null));
        dao.activeAdmins = 1;
        UsuarioCrudService service = new UsuarioCrudService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.delete(1, 1, 2));
    }

    @Test
    void noConvierteRecepcionistaEnOdontologoDesdeUsuarios() {
        FakeDao dao = new FakeDao();
        dao.users.add(new UsuarioDetalle(2, "Maria", "Lopez Diaz", "mlopez@odontosync.pe", null,
            Role.RECEPCIONISTA, "ACTIVO", null));
        assertThrows(IllegalArgumentException.class, () -> new UsuarioCrudService(dao).update(
            1, 1, 2, "Maria", "Lopez Diaz", "", "ODONTOLOGO", "ACTIVO", new char[0], "12345"));
        assertNull(dao.updatedEmail);
    }

    @Test
    void noCambiaRolDeOdontologoDesdeUsuarios() {
        FakeDao dao = new FakeDao();
        dao.users.add(dentistAccount());
        assertThrows(IllegalArgumentException.class, () -> new UsuarioCrudService(dao).update(
            1, 1, 20, "Carlos", "Ramirez Vega", "999111222", "RECEPCIONISTA", "ACTIVO", new char[0], ""));
        assertNull(dao.updatedEmail);
    }

    @Test
    void noEliminaCuentaVinculadaAOdontologoDesdeUsuarios() {
        FakeDao dao = new FakeDao();
        dao.users.add(dentistAccount());
        assertThrows(IllegalArgumentException.class, () -> new UsuarioCrudService(dao).delete(1, 1, 20));
        assertFalse(dao.deleted);
    }

    @Test
    void datosProfesionalesSeEditanSoloEnOdontologos() {
        FakeDao dao = new FakeDao();
        dao.users.add(dentistAccount());
        assertThrows(IllegalArgumentException.class, () -> new UsuarioCrudService(dao).update(
            1, 1, 20, "Carlos Nuevo", "Ramirez Vega", "999111222", "ODONTOLOGO", "ACTIVO", new char[0], "12345"));
        assertNull(dao.updatedEmail);
    }

    private static UsuarioDetalle dentistAccount() {
        return new UsuarioDetalle(20, "Carlos", "Ramirez Vega", "cramirez@odontosync.pe", "999111222",
            Role.ODONTOLOGO, "ACTIVO", "12345");
    }

    private static final class FakeDao implements UsuarioCrudDAO {
        final List<UsuarioDetalle> users = new ArrayList<>();
        final Set<String> occupied = new HashSet<>();
        boolean race;
        boolean deleted;
        boolean otherDuplicate;
        int createAttempts;
        String createdSurnames;
        String updatedEmail;
        long activeAdmins = 2;
        Role createdRole;
        String createdEmail;
        String createdHash;

        public List<UsuarioDetalle> findDetailedByClinic(long c) { return users; }
        public Optional<UsuarioDetalle> findDetailedById(long c, long id) {
            return users.stream().filter(x -> x.id() == id).findFirst();
        }
        public boolean emailExistsGlobally(String e) { return occupied.contains(e); }
        public long countActiveAdministrators(long c) { return activeAdmins; }
        public long create(long c, Role r, String n, String a, String e, String t, String h, String s, String col) throws SQLException {
            createAttempts++;
            if (otherDuplicate) throw new SQLException("duplicate unrelated key", "23000", 1062);
            if (race) { race = false; occupied.add(e); throw new SQLException("duplicate", "23000", 1062); }
            createdSurnames=a; createdRole=r; createdEmail=e; createdHash=h; return 10;
        }
        public void update(long c,long id,Role r,String n,String a,String e,String t,String h,String s,String col) { updatedEmail=e; }
        public boolean delete(long c,long id) { deleted = true; return true; }
    }
}
