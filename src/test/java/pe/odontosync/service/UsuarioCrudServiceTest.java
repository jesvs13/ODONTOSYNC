package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
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

        long id = service.create(1, "Maria", "Lopez", "NUEVA@ODONTO.PE", "999111222",
            "RECEPCIONISTA", "ACTIVO", password, "");

        assertEquals(10L, id);
        assertEquals(Role.RECEPCIONISTA, dao.createdRole);
        assertEquals("nueva@odonto.pe", dao.createdEmail);
        assertTrue(Passwords.matches("Recep2026!".toCharArray(), dao.createdHash));
    }

    @Test
    void odontologoRequiereColegiatura() {
        UsuarioCrudService service = new UsuarioCrudService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.create(
            1, "Carlos", "Ramos", "carlos@o.pe", "999111222",
            "ODONTOLOGO", "ACTIVO", "Dentista2026!".toCharArray(), ""
        ));
    }

    @Test
    void noPermiteCorreoDuplicado() {
        FakeDao dao = new FakeDao(); dao.duplicateEmail = true;
        UsuarioCrudService service = new UsuarioCrudService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.create(
            1, "Maria", "Lopez", "recepcion@odontosync.pe", "999111222",
            "RECEPCIONISTA", "ACTIVO", "Recep2026!".toCharArray(), ""
        ));
    }

    @Test
    void administradorNoPuedeDesactivarSuPropiaCuenta() {
        FakeDao dao = new FakeDao();
        dao.users.add(new UsuarioDetalle(1, "Admin", "Demo", "admin@o.pe", "999", Role.ADMINISTRADOR, "ACTIVO", null));
        UsuarioCrudService service = new UsuarioCrudService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.update(
            1, 1, 1, "Admin", "Demo", "admin@o.pe", "999",
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

    private static final class FakeDao implements UsuarioCrudDAO {
        final List<UsuarioDetalle> users = new ArrayList<>();
        boolean duplicateEmail;
        long activeAdmins = 2;
        Role createdRole;
        String createdEmail;
        String createdHash;

        public List<UsuarioDetalle> findDetailedByClinic(long c) { return users; }
        public Optional<UsuarioDetalle> findDetailedById(long c, long id) {
            return users.stream().filter(x -> x.id() == id).findFirst();
        }
        public boolean emailExists(long c, String e, Long exclude) { return duplicateEmail; }
        public long countActiveAdministrators(long c) { return activeAdmins; }
        public long create(long c, Role r, String n, String a, String e, String t, String h, String s, String col) {
            createdRole=r; createdEmail=e; createdHash=h; return 10;
        }
        public void update(long c,long id,Role r,String n,String a,String e,String t,String h,String s,String col) { }
        public boolean delete(long c,long id) { return true; }
    }
}
