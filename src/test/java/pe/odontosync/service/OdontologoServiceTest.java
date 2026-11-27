package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.OdontologoCrudDAO;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoDetalle;

class OdontologoServiceTest {
    @Test
    void administradorCreaOdontologoConCuentaYEspecialidad() throws Exception {
        FakeDao dao = new FakeDao();
        OdontologoService service = new OdontologoService(dao);
        long id = service.create(
            admin(), " Carlos ", " Ramirez ", "Vega", "999111222",
            "Clave2026".toCharArray(), "cop-12345", "2026-01-10", "ACTIVO", List.of(1L, 2L)
        );
        assertEquals(50L, id);
        assertEquals("Carlos", dao.createdNames);
        assertEquals("cramirez@odontosync.pe", dao.createdEmail);
        assertEquals("Ramirez Vega", dao.createdSurnames);
        assertEquals("COP-12345", dao.createdLicense);
        assertEquals(List.of(1L, 2L), dao.createdSpecialties);
        assertNotNull(dao.createdHash);
        assertFalse(dao.createdHash.contains("Clave2026"));
    }

    @Test
    void recepcionSoloConsultaYNoPuedeCrear() {
        OdontologoService service = new OdontologoService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.create(
            reception(), "Carlos", "Ramirez", "Vega", "999111222", "Clave2026".toCharArray(),
            "COP-1", "2026-01-10", "ACTIVO", List.of(1L)
        ));
    }

    @Test
    void exigeEspecialidadActiva() {
        FakeDao dao = new FakeDao();
        OdontologoService service = new OdontologoService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.create(
            admin(), "Carlos", "Ramirez", "Vega", "999111222", "Clave2026".toCharArray(),
            "COP-2", "2026-01-10", "ACTIVO", List.of(99L)
        ));
    }

    @Test
    void bajaEsLogica() throws Exception {
        FakeDao dao = new FakeDao();
        dao.dentists.add(sample());
        OdontologoService service = new OdontologoService(dao);
        service.changeStatus(admin(), 10, "INACTIVO");
        assertEquals("INACTIVO", dao.newStatus);
    }

    @Test
    void correoOdontologoConsideraCoincidenciasDeTodasLasClinicas() throws Exception {
        FakeDao dao = new FakeDao();
        dao.occupied.add("cramirez@odontosync.pe");
        dao.occupied.add("cramirezv@odontosync.pe");
        new OdontologoService(dao).create(admin(), "Carlos", "Ramirez", "Vega", "999111222",
            "Clave2026".toCharArray(), "COP-12345", "2026-01-10", "ACTIVO", List.of(1L));
        assertEquals("cramirezv2@odontosync.pe", dao.createdEmail);
        assertEquals(1L, dao.createdClinic);
    }

    @Test
    void reintentaAltaSiOtroUsuarioOcupaElCorreo() throws Exception {
        FakeDao dao = new FakeDao();
        dao.race = true;
        new OdontologoService(dao).create(admin(), "Carlos", "Ramirez", "Vega", "999111222",
            "Clave2026".toCharArray(), "COP-12345", "2026-01-10", "ACTIVO", List.of(1L));
        assertEquals("cramirezv@odontosync.pe", dao.createdEmail);
        assertEquals(2, dao.createAttempts);
    }

    @Test
    void edicionProfesionalConservaCorreoDeAcceso() throws Exception {
        FakeDao dao = new FakeDao();
        dao.dentists.add(sample());
        new OdontologoService(dao).update(admin(), 10, "Carlos Alberto", "Ramirez", "alterado@x.pe",
            "999111222", new char[0], "COP-1", "2026-01-10", "ACTIVO", List.of(1L));
        assertEquals("carlos@x.pe", dao.updatedEmail);
        assertNull(dao.updatedHash);
    }

    @Test
    void cambioDePasswordNoSeRealizaDesdePerfilProfesional() {
        FakeDao dao = new FakeDao();
        dao.dentists.add(sample());
        assertThrows(IllegalArgumentException.class, () -> new OdontologoService(dao).update(
            admin(), 10, "Carlos", "Ramirez", "carlos@x.pe", "999111222", "OtraClave2026".toCharArray(),
            "COP-1", "2026-01-10", "ACTIVO", List.of(1L)));
        assertNull(dao.updatedEmail);
    }

    private static User admin() { return new User(1, 1, "Admin", "Clínica", Role.ADMINISTRADOR); }
    private static User reception() { return new User(3, 1, "Recepción", "Clínica", Role.RECEPCIONISTA); }

    private static OdontologoDetalle sample() {
        return new OdontologoDetalle(10, 20, "Carlos", "Ramirez", "COP-1", "999111222",
            "carlos@x.pe", LocalDate.of(2026,1,10), "ACTIVO", List.of(1L), List.of("Odontología General"));
    }

    private static final class FakeDao implements OdontologoCrudDAO {
        final List<OdontologoDetalle> dentists = new ArrayList<>();
        String createdNames, createdSurnames, createdEmail, createdLicense, createdHash, newStatus;
        String updatedEmail, updatedHash;
        long createdClinic;
        int createAttempts;
        boolean race;
        final Set<String> occupied = new HashSet<>();
        List<Long> createdSpecialties;

        public List<OdontologoDetalle> searchByClinic(long c, String q) { return dentists; }
        public Optional<OdontologoDetalle> findDetailedById(long c, long id) {
            return dentists.stream().filter(d -> d.idOdontologo() == id).findFirst();
        }
        public List<EspecialidadOpcion> findActiveSpecialties() {
            return List.of(new EspecialidadOpcion(1,"Odontología General","ACTIVA"), new EspecialidadOpcion(2,"Ortodoncia","ACTIVA"));
        }
        public boolean emailExists(long c,String e,Long x) { return false; }
        // El contrato global se incorporará al DAO en GREEN.
        public boolean emailExistsGlobally(String e) { return occupied.contains(e); }
        public boolean licenseExists(long c,String e,Long x) { return false; }
        public long create(long c,String n,String a,String e,String t,String h,String co,LocalDate f,String st,List<Long> sp) throws SQLException {
            createAttempts++;
            if (race) { race = false; occupied.add(e); throw new SQLException("duplicate email", "23000", 1062); }
            createdClinic=c; createdSurnames=a; createdNames=n; createdEmail=e; createdLicense=co; createdHash=h; createdSpecialties=sp; return 50;
        }
        public void update(long c,long id,String n,String a,String e,String t,String h,String co,LocalDate f,String st,List<Long> sp) { updatedEmail=e; updatedHash=h; }
        public void updateStatus(long c,long id,String st) { newStatus=st; }
    }
}
